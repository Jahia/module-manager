import org.apache.commons.lang.StringUtils
import org.jahia.osgi.BundleUtils
import org.jahia.services.content.*
import org.jahia.services.content.nodetypes.ExtendedNodeType
import org.jahia.services.content.nodetypes.NodeTypeRegistry
import org.jahia.services.modulemanager.spi.Config
import org.jahia.services.modulemanager.spi.ConfigService
import javax.jcr.RepositoryException
import javax.jcr.query.Query
import java.nio.file.FileSystems
import java.nio.file.Files

def removedNodeTypes = [
        [module: "system-jahia", nodeTypes: [
                [name: "jnt:forgeServerSettings", order: 2],
                [name: "jnt:forgesServerSettings", order: 1]
        ]],
        [module: "serverSettings-ee", nodeTypes: [
                [name: "jnt:serverSettingsManageForges"]
        ]]
]

NodeTypeRegistry registry = NodeTypeRegistry.getInstance()
def unregister = true
def delete = true

removedNodeTypes.each { map ->
    try {
        String moduleName = map.module

        // Delete first the node in a specific order
        List<ExtendedNodeType> nodeTypes = map.nodeTypes.collectMany { typeEntry ->
            try {
                [registry.getNodeType(typeEntry.name as String)]
            } catch (Exception ignored) {
                []
            }
        }

        if (!nodeTypes.isEmpty()) {
            log.info("Removing nodes [${nodeTypes.join(',')}] from JCR")
            deleteNodes(nodeTypes.iterator(), delete)
        }

        // In second, unregister the nodetypes depending on the definitions hierarchy
        nodeTypes = map.nodeTypes.sort { it.order ?: 0 }.collectMany { typeEntry ->
            try {
                [registry.getNodeType(typeEntry.name as String)]
            } catch (Exception ignored) {
                []
            }
        }

        if (!nodeTypes.isEmpty()) {
            log.info("Removing nodeTypes [${nodeTypes.join(',')}] from module $moduleName")
            deleteNodeTypes(nodeTypes.iterator(), unregister)
            JCRStoreService.getInstance().deployDefinitions(moduleName)
        }

    } catch (Exception e) {
        log.error(e.getMessage(), e)
    }
}


private void deleteNodes(Iterator<ExtendedNodeType> it, boolean delete) {
    def configService = BundleUtils.getOsgiService(ConfigService.class, null)
    boolean migrated = false
    while (it.hasNext()) {
        ExtendedNodeType nodeType = it.next()
        String nodeTypeName = nodeType.getName()

        JCRCallback<Object> callback = new JCRCallback<Object>() {
            @Override
            Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper root = session.getNode("/")
                javax.jcr.NodeIterator nodes = session
                        .getProviderSession(root.getProvider())
                        .getWorkspace()
                        .getQueryManager()
                        .createQuery("SELECT * FROM ['${nodeTypeName}']", Query.JCR_SQL2)
                        .execute()
                        .getNodes()

                int count = 0
                while (nodes.hasNext()) {
                    javax.jcr.Node node = nodes.next()
                    if (nodeTypeName == "jnt:forgeServerSettings" && configService != null) {
                        migrated = migrated || migrateForgeSettings(node, configService)
                    }
                    if (nodeType.isMixin() && !node.getPrimaryNodeType().isNodeType(nodeTypeName)) {
                        if (delete) {
                            node.removeMixin(nodeTypeName)
                        }
                    } else {
                        if (delete) {
                            node.remove()
                        }
                    }
                    if ((++count % 100) == 0) {
                        session.save()
                    }
                }
                log.info("Called remove for node: $nodeTypeName for workspace: ${session.getWorkspace().getName()} (effective: $delete)")
                session.save()
                return null
            }
        }
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, "default", null, callback)
        } catch (Exception e) {
            log.info(e.getMessage())
        }
    }

    // Create an empty directory to know if configurations have been migrated, needed for Cypress tests
    if (migrated) {
        def tempDirectory = Files.createFile(FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"), "forge_nodes_migrated.txt"))
    }
}

private void deleteNodeTypes(Iterator<ExtendedNodeType> it, boolean unregister) {
    NodeTypeRegistry registry = NodeTypeRegistry.getInstance()

    while (it.hasNext()) {
        ExtendedNodeType nodeType = it.next()
        String nodeTypeName = nodeType.getName()
        try {
            log.info("Called remove for nodeType: $nodeTypeName for workspace (effective: $unregister)")
            if (unregister) {
                registry.unregisterNodeType(nodeTypeName)
            }
        } catch (Exception e) {
            log.info(e.getMessage())
        }
    }
}

private boolean migrateForgeSettings(javax.jcr.Node node, ConfigService configService) {
    if (!node.hasProperty("j:url")) {
        log.info("No JCR forge settings to convert")
        return false
    }
    String url = node.getProperty("j:url").getString()
    if (url.contains("store.jahia.com")) {
        log.info("Not converting JCR forge settings ${node.getPath()} to Karaf cfg file")
        return false
    }
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
    log.info("Converting JCR forge settings ${node.getPath()} to Karaf cfg file")
    return true
}
