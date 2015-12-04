package org.jahia.modules.modulemanager.payload;

import java.io.Serializable;

/**
 * @author achaabni
 */
public class OperationState implements Serializable{

    /**
     * name
     */
    private String name;

    /**
     * action
     */
    private String action;

    /**
     * info
     */
    private String info;

    /**
     * state
     */
    private String state;

    /**
     * completed
     */
    private boolean completed;

    public OperationState(String name, String action, String info, String state, boolean completed) {
        this.name = name;
        this.action = action;
        this.info = info;
        this.state = state;
        this.completed = completed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
