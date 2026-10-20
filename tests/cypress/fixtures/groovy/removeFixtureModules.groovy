import org.jahia.osgi.BundleUtils
import org.jahia.osgi.FrameworkService
import org.jahia.services.content.JCRStoreService
import org.jahia.services.content.nodetypes.NodeTypeRegistry
import org.jahia.services.modulemanager.ModuleManager

// Uninstalls every version of the fixture modules, including what a failed run left behind.
def moduleIds = 'MODULE_IDS'.split(',') as Set
def moduleManager = BundleUtils.getOsgiService(ModuleManager.class, null)
FrameworkService.bundleContext.bundles
        .findAll { moduleIds.contains(it.symbolicName) }
        .each { moduleManager.uninstall("${it.headers.get('Jahia-GroupId')}/${it.symbolicName}/${it.version}", null) }

// Jahia keeps the definitions of a module after its uninstall, with the version and the date of the
// jar that deployed them. The definition check of an upload runs only for a newer jar, and compares it
// with the registered definitions, so a committed jar is checked the same way on every run only once
// the record and the registered definitions are both gone.
moduleIds.each {
    JCRStoreService.getInstance().undeployDefinitions(it)
    NodeTypeRegistry.getInstance().unregisterNodeTypes(it)
}
