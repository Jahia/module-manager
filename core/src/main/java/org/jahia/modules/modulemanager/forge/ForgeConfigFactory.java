package org.jahia.modules.modulemanager.forge;

import org.jahia.bin.Jahia;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        service = {ForgeConfigFactory.class, ManagedServiceFactory.class},
        immediate = true,
        property = {
                Constants.SERVICE_PID + "=org.jahia.modules.modulemanager.forge.configuration",
                Constants.SERVICE_DESCRIPTION + "=Jahia forge configuration service",
                Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME
        }
)
public class ForgeConfigFactory implements ManagedServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(ForgeConfigFactory.class);
    private final Map<String, ForgeConfig> configs = new ConcurrentHashMap<>();

    public ForgeConfigFactory() {
        logger.debug("Creating Jahia Forge Config Factory");
    }

    public String getName() {
        return "Jahia Forge Config Factory";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) {
        logger.info("Updating Jahia Forge configuration for pid: {}, config size: {}", pid, properties.size());
        final ForgeConfig config = ForgeConfig.build(pid, properties);
        configs.put(pid, config);
    }

    @Override
    public void deleted(String pid) {
        logger.info("Deleting Jahia Forge configuration for pid: {}", pid);
        configs.remove(pid);
    }

    public Collection<ForgeConfig> getConfigs() {
        return configs.values();
    }
}

