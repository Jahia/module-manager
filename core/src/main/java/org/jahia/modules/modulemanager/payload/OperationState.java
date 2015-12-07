package org.jahia.modules.modulemanager.payload;

import java.io.Serializable;

/**
 * Represents the values of a Node Operation
 * @author achaabni
 */
public class OperationState implements Serializable{

    /**
     * Defines the name.
     */
    private String name;

    /**
     * Defines what action was called for this operation.
     */
    private String action;

    /**
     * Defines the information saved for the operation.
     */
    private String info;

    /**
     * Defines the state of the operation.
     */
    private String state;

    /**
     * Defines if an operation was completed or not.
     */
    private boolean completed;

    /**
     * Initializes an instance of this class.
     * @param name name
     * @param action action
     * @param info info
     * @param state state
     * @param completed <code>true</code> if the operation is closed
     */
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
