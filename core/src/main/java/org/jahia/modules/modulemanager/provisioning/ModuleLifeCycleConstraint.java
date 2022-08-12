package org.jahia.modules.modulemanager.provisioning;

import org.osgi.framework.Version;

import java.util.List;

public class ModuleLifeCycleConstraint {
    private String moduleId;
    private List<Version> versionRange;
    private List<String> disableOperations;
    private String info;
    private String warning;
    private String alert;
    private String error;

    public ModuleLifeCycleConstraint() {
    }

    public ModuleLifeCycleConstraint(String moduleId, List<Version> versionRange, List<String> disableOperations) {
        this.moduleId = moduleId;
        this.versionRange = versionRange;
        this.disableOperations = disableOperations;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public List<Version> getVersionRange() {
        return versionRange;
    }

    public void setVersionRange(List<Version> versionRange) {
        this.versionRange = versionRange;
    }

    public List<String> getDisableOperations() {
        return disableOperations;
    }

    public void setDisableOperations(List<String> disableOperations) {
        this.disableOperations = disableOperations;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public String getAlert() {
        return alert;
    }

    public void setAlert(String alert) {
        this.alert = alert;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
