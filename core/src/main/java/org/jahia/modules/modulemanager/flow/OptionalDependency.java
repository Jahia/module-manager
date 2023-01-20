package org.jahia.modules.modulemanager.flow;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.data.templates.ModuleState;

/**
 * Optional dependency model
 */
public class OptionalDependency {
    private JahiaTemplatesPackage jahiaTemplatesPackage;
    private String moduleId;
    private boolean isUnstarted;

    public OptionalDependency(String moduleId, JahiaTemplatesPackage jahiaTemplatesPackage) {
        this.moduleId = moduleId;
        this.jahiaTemplatesPackage = jahiaTemplatesPackage;
        this.isUnstarted = this.jahiaTemplatesPackage == null || !this.jahiaTemplatesPackage.getState().getState().equals(ModuleState.State.STARTED);
    }

    public JahiaTemplatesPackage getJahiaTemplatesPackage() {
        return jahiaTemplatesPackage;
    }

    public String getModuleId() {
        return moduleId;
    }

    public boolean getIsUnstarted() {
        return isUnstarted;
    }
}
