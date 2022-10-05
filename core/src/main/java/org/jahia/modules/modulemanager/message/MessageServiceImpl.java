package org.jahia.modules.modulemanager.message;

import org.jahia.modules.modulemanager.util.ConfigUtil;
import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.Severity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to manage custom messages displayed in module manager
 */
@Component(
        service = {MessageService.class, ManagedServiceFactory.class},
        property = "service.pid=org.jahia.modules.modulemanager.message",
        immediate = true
)
public class MessageServiceImpl implements MessageService, ManagedServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    private static final String CONFIG_KEY = "message";
    private static final List<CustomMessage> messages = new ArrayList<>();

    private void parseConfig(String pid, Dictionary<String, ?> props) {
        logger.debug("Parsing configuration property values");
        PropertiesManager pm = new PropertiesManager(ConfigUtil.getMap(props));
        PropertiesValues messageProp = pm.getValues().getValues(CONFIG_KEY);
        for (String key: messageProp.getKeys()) {
            String m = messageProp.getProperty(key);
            messages.add(new CustomMessage(pid, "customMessage", m, Severity.valueOf(key.toUpperCase())));
        }

        messages.sort(new MessageComparator());
        logger.debug("Configuration parsed");
    }

    private void clearConstraintsByPid(String pid) {
        if (pid != null) {
            messages.removeAll(messages.stream().filter(m -> m.getPid().equals(pid)).collect(Collectors.toList()));
        }
    }

    @Override
    public List<CustomMessage> getAllMessages() {
        return messages;
    }

    @Override
    public String getName() {
        return "Module Manager Message Config";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> dictionary) throws ConfigurationException {
        if (dictionary == null) {
            return;
        }

        logger.debug("Updating message configuration {}...", pid);
        clearConstraintsByPid(pid);
        parseConfig(pid, dictionary);
        logger.debug("Message configuration updated.");
    }

    @Override
    public void deleted(String pid) {
        clearConstraintsByPid(pid);
    }

    private class MessageComparator implements Comparator<CustomMessage> {

        @Override
        public int compare(CustomMessage m1, CustomMessage m2) {
            return Integer.compare(m2.getSeverity().ordinal(), m1.getSeverity().ordinal());

        }
    }
}
