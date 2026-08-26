import org.apache.commons.lang3.StringUtils
import org.jahia.osgi.BundleUtils
import org.jahia.services.content.*
import org.jahia.services.content.nodetypes.ExtendedNodeType
import org.jahia.services.content.nodetypes.NodeTypeRegistry
import org.jahia.services.modulemanager.spi.Config
import org.jahia.services.modulemanager.spi.ConfigService
import javax.jcr.RepositoryException
import javax.jcr.nodetype.NoSuchNodeTypeException
import javax.jcr.query.Query
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean

// Obsolete node types to migrate (forge settings only) then delete, in deletion order.
def removedNodeTypes = [
        "jnt:forgeServerSettings",
        "jnt:forgesServerSettings",
        "jnt:serverSettingsManageForges"
]

NodeTypeRegistry registry = NodeTypeRegistry.getInstance()
def delete = true

try {
    // Resolve the obsolete node types that are still registered, preserving list order.
    List<ExtendedNodeType> nodeTypes = removedNodeTypes.collectMany { String name ->
        try {
            [registry.getNodeType(name)]
        } catch (NoSuchNodeTypeException ignored) {
            []
        }
    }

    if (!nodeTypes.isEmpty()) {
        log.info("Migrating then removing nodes [${nodeTypes.join(',')}] from JCR")
        // Phase 1: migrate forge settings to Karaf configuration (reads the JCR)...
        boolean migrated = migrateNodes()
        // ...Phase 2: delete the now-obsolete nodes from the JCR if delete true and migration has been done
        deleteNodes(nodeTypes.iterator(), delete && migrated)
    }
} catch (Exception e) {
    log.error(e.getMessage(), e)
}


/**
 * Phase 1 — migrate: only jnt:forgeServerSettings needs migrating. Its JCR nodes are
 * converted to Karaf .cfg configuration here, before deleteNodes removes them, so the
 * source data is still available while we copy it over.
 */
private boolean migrateNodes() {
    def configService = BundleUtils.getOsgiService(ConfigService.class, null)

    // Without the config service we cannot migrate forge settings. Report failure so the
    // caller skips deletion and the forge data is preserved for a later run.
    if (configService == null) {
        log.warn("ConfigService unavailable, skipping forge settings migration")
        return false
    }

    // Shared mutable holder: the migration flag is updated from inside the anonymous inner
    // class (JCRCallback) below. Reassigning a captured local from an anonymous inner class
    // is not reliably propagated in Groovy, so we mutate a holder by reference instead.
    AtomicBoolean migrated = new AtomicBoolean(true)

    // If jnt:forgeServerSettings is no longer registered there is nothing to migrate (it was
    // removed in a previous run); querying it would fail, so only run the migration when present.
    if (isNodeTypeRegistered("jnt:forgeServerSettings")) {
        JCRCallback<Object> callback = new JCRCallback<Object>() {
            @Override
            Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                javax.jcr.NodeIterator nodes = queryNodes(session, "jnt:forgeServerSettings")
                while (nodes.hasNext()) {
                    javax.jcr.Node node = nodes.next()
                    migrated.set(migrated.get() && migrateForgeSettings(node, configService))
                }
                return null
            }
        }
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, "default", null, callback)
        } catch (Exception e) {
            migrated.set(false)
            log.error(e.getMessage(), e)
        }
    }

    // Marker file is a test signal only — its I/O must not affect the migration verdict.
    if (migrated.get()) {
        try {
            def path = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"), "forge_nodes_migrated.txt")
            if (!Files.exists(path)) {
                Files.createFile(path)
            }
        } catch (Exception e) {
            log.error("Could not create forge migration marker file", e)
        }
    }
    return migrated.get()
}

/**
 * Phase 2 — delete: remove the obsolete nodes from the JCR. Migration has already run,
 * so it is safe to drop the nodes here.
 */
private void deleteNodes(Iterator<ExtendedNodeType> it, boolean delete) {
    while (it.hasNext()) {
        ExtendedNodeType nodeType = it.next()
        String nodeTypeName = nodeType.getName()

        JCRCallback<Object> callback = new JCRCallback<Object>() {
            @Override
            Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                javax.jcr.NodeIterator nodes = queryNodes(session, nodeTypeName)
                int count = 0
                while (nodes.hasNext()) {
                    javax.jcr.Node node = nodes.next()
                    if (delete) {
                        node.remove()
                        if ((++count % 100) == 0) {
                            session.save()
                        }
                    }
                }
                if (delete) {
                    session.save()
                }
                log.info("Called remove for node: $nodeTypeName for workspace: ${session.getWorkspace().getName()} (effective: $delete)")
                return null
            }
        }
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, "default", null, callback)
        } catch (Exception e) {
            log.error(e.getMessage(), e)
        }
    }
}

/**
 * Run the JCR-SQL2 query that returns every node of the given type. Shared by the migrate
 * and delete phases so the query is defined in a single place.
 */
private javax.jcr.NodeIterator queryNodes(JCRSessionWrapper session, String nodeTypeName) throws RepositoryException {
    JCRNodeWrapper root = session.getNode("/")
    return session
            .getProviderSession(root.getProvider())
            .getWorkspace()
            .getQueryManager()
            .createQuery("SELECT * FROM ['${nodeTypeName}']", Query.JCR_SQL2)
            .execute()
            .getNodes()
}

/**
 * True when the given node type is still registered. Lets the migrate phase skip the
 * JCR-SQL2 query when the type has already been removed, instead of letting it fail.
 */
private boolean isNodeTypeRegistered(String nodeTypeName) {
    try {
        NodeTypeRegistry.getInstance().getNodeType(nodeTypeName)
        return true
    } catch (NoSuchNodeTypeException ignored) {
        return false
    }
}

private boolean migrateForgeSettings(javax.jcr.Node node, ConfigService configService) {
    if (!node.hasProperty("j:url")) {
        log.info("No JCR forge settings to convert")
        return true
    }
    String url = node.getProperty("j:url").getString()
    if (url.contains("store.jahia.com")) {
        log.info("Not converting JCR forge settings ${node.getPath()} to Karaf cfg file, already there")
        return true
    }
    log.info("Converting JCR forge settings ${node.getPath()} to Karaf cfg file")
    Config config = configService.getConfig("org.jahia.modules.modulemanager.forge.configuration", node.getName())
    Map<String, String> properties = config.getRawProperties()
    properties["url"] = url
    if (node.hasProperty("j:user") && !StringUtils.isEmpty(node.getProperty("j:user").getString())) {
        properties["user"] = node.getProperty("j:user").getString()
    }
    if (node.hasProperty("j:password") && !StringUtils.isEmpty(node.getProperty("j:password").getString())) {
        properties["password"] = node.getProperty("j:password").getString()
    }
    configService.storeConfig(config)
    return true
}
