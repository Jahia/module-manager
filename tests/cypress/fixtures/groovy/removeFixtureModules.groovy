import org.jahia.osgi.BundleUtils
import org.jahia.osgi.FrameworkService
import org.jahia.services.modulemanager.ModuleManager

// Uninstalls every version of every module of the fixture group, including what a failed run left behind.
def moduleManager = BundleUtils.getOsgiService(ModuleManager.class, null)
FrameworkService.bundleContext.bundles
        .findAll { it.headers.get('Jahia-GroupId') == 'GROUP_ID' }
        .each { moduleManager.uninstall("GROUP_ID/${it.symbolicName}/${it.version}", null) }
