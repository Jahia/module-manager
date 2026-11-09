import org.jahia.api.Constants
import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate

// Adds MODULE_ID to the modules of the site SITE_KEY in the live workspace, or removes it when
// USE_ON_SITE is false. The Modules screen reads the live workspace to tell that a site uses a module.
def moduleId = 'MODULE_ID'
def use = 'USE_ON_SITE' == 'true'
JCRTemplate.instance.doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE, null, new JCRCallback<Object>() {
    Object doInJCR(JCRSessionWrapper session) {
        def site = session.getNode('/sites/SITE_KEY')
        def modules = site.hasProperty('j:installedModules') ? site.getProperty('j:installedModules').values*.string : []
        modules.remove(moduleId)
        if (use) {
            modules.add(moduleId)
        }
        site.setProperty('j:installedModules', modules as String[])
        session.save()
        return null
    }
})
