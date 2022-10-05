package org.jahia.modules.modulemanager.message;

import org.springframework.binding.message.Message;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.binding.message.Severity;

import static org.springframework.binding.message.Severity.*;

public class CustomMessage extends Message {

    private String pid;

    public CustomMessage(String pid, Object source, String text, Severity severity) {
        super(source, text, severity);
        this.pid = pid;
    }

    public void addMessageToContext(MessageContext messageContext) {
        MessageBuilder mb = new MessageBuilder().source("customMessage").defaultText(this.getText());

        if (this.getSeverity().equals(INFO)) {
            messageContext.addMessage(mb.info().build());
        }

        if (this.getSeverity().equals(WARNING)) {
            messageContext.addMessage(mb.warning().build());
        }
    }

    public String getPid() {
        return pid;
    }
}
