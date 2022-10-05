package org.jahia.modules.modulemanager.message;

import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.Severity;

import java.util.*;

/**
 * Service to manage custom messages displayed in module manager
 */
@Component(
        service = MessageService.class,
        immediate = true,
        name = "org.jahia.modules.modulemanager.message.admin"
)
public class MessageServiceImpl implements MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    private static final String CONFIG_KEY = "message";
    private static final List<CustomMessage> messages = new ArrayList<>();

    @Activate
    public void activate(Map<String, String> props) {
        logger.debug("Activating message service");
        clearConstraintsByPid(props.get("service.pid"));
        parseConfig(props);
    }

    private void parseConfig(Map<String, String> props) {
        logger.debug("Parsing configuration property values");
        String pid = props.get("service.pid");
        PropertiesManager pm = new PropertiesManager(props);
        PropertiesValues messageProp = pm.getValues().getValues(CONFIG_KEY);
        for (String key: messageProp.getKeys()) {
            String m = messageProp.getProperty(key);
            messages.add(new CustomMessage(pid, "customMessage", m, Severity.valueOf(key.toUpperCase())));
        }

        messages.sort(new MessageComparator());
        logger.debug("Configuration parsed");
    }

    @Modified
    public void modified(Map<String, String> props) {
        String pid = props.get("service.pid");
        logger.debug("Updating message configuration {}...", pid);
        clearConstraintsByPid(pid);
        parseConfig(props);
        logger.debug("Message configuration updated.");
    }

    private void clearConstraintsByPid(String pid) {
        if (pid != null) {
            messages.forEach(m -> {
                if (m.getPid().equals(pid)) {
                    messages.remove(m);
                }
            });
        }
    }

    @Override
    public List<CustomMessage> getAllMessages() {
        return messages;
    }

    private class MessageComparator implements Comparator<CustomMessage> {

        @Override
        public int compare(CustomMessage m1, CustomMessage m2) {
            return Integer.compare(m2.getSeverity().ordinal(), m1.getSeverity().ordinal());

        }
    }
}
