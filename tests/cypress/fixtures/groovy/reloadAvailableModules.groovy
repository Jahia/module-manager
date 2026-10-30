import org.jahia.services.SpringContextSingleton

// The next render of the Available modules tab lists the modules of the app stores again.
SpringContextSingleton.getBeanInModulesContext('forgeService').flushModules()
