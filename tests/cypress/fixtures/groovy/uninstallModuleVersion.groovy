import org.jahia.osgi.BundleUtils
import org.jahia.services.modulemanager.ModuleManager

// Uninstalls one version of a module, and keeps the record of the definitions that it deployed.
BundleUtils.getOsgiService(ModuleManager.class, null).uninstall('MODULE_KEY', null)
