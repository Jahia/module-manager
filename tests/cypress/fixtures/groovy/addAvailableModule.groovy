import groovy.json.JsonSlurper
import org.jahia.services.SpringContextSingleton

// Lists one more module in the Available modules tab, as an app store would list it.
// MODULE_ENTRY is the module as a JSON object, Base64-encoded, keyed by the properties of Module.
def entry = new JsonSlurper().parseText(new String('MODULE_ENTRY'.decodeBase64(), 'UTF-8'))
def forgeService = SpringContextSingleton.getBeanInModulesContext('forgeService')
// A pending reload empties the list, so the list loads first and keeps the entry until the next reload.
forgeService.loadModules()
def module = forgeService.class.classLoader.loadClass('org.jahia.modules.modulemanager.forge.Module').newInstance()
entry.each { key, value -> module[key] = value }
forgeService.modules.removeIf { it.id == module.id }
forgeService.modules.add(module)
