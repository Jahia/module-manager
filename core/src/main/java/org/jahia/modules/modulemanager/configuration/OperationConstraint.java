package org.jahia.modules.modulemanager.configuration;

import org.apache.commons.lang3.EnumUtils;
import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Instance of an operation constraint defined within the 'moduleLifeCycleConstraints' configuration
 * Optional parameters include version range and specific operations to disable
 *
 * If versionRange is not found, then constraint is applied to all versions of the module
 * If disableOperations is blank, then all operations are not allowed
 */
public class OperationConstraint {

    private static final Logger logger = LoggerFactory.getLogger(OperationConstraint.class);

    private final String moduleId;
    private VersionRange versionRange = null;
    private final Set<Operation> disableOperations = new HashSet<>();
    private final String pid;

    enum Operation {DEPLOY, UNDEPLOY, STOP, START};


    public OperationConstraint(String pid, String moduleId) {
        this.pid = pid;
        this.moduleId = moduleId;
    }

    public static OperationConstraint parse(String pid, PropertiesValues constraintConfig) {
        // parse moduleId
        String moduleId = constraintConfig.getProperty("moduleId");
        if (moduleId == null) {
            return null;
        }
        OperationConstraint constraint = new OperationConstraint(pid, moduleId);

        // parse version
        String vrStr = constraintConfig.getProperty("version");
        constraint.setVersionRange(vrStr);

        // parse constraints
        PropertiesList ops = constraintConfig.getList("disableOperations");
        for (int i = 0; i < ops.getSize(); i++) {
            String op = ops.getProperty(i);
            constraint.addOperation(op);
        }
        // if no operations, add all
        if (!constraint.hasOperations()) {
            Arrays.stream(Operation.values()).forEach(constraint::addOperation);
        }

        return constraint;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getPid() {
        return pid;
    }

    /**
     * Set/parse version range for this constraint, or null (all version) if it cannot be parsed
     * @param versionRange
     */
    public void setVersionRange(String versionRange) {
        VersionRange vr = null;
        try {
            vr = (versionRange == null) ? null : VersionRange.valueOf(versionRange);
        } catch (Exception e) {
            logger.warn("Unable to parse version range for module {}: {}", this.moduleId, versionRange);
        }
        this.versionRange = vr;
    }

    public void addOperation(String op) {
        Operation o = EnumUtils.getEnum(Operation.class, op);
        addOperation(o);
        if (o == null) {
            logger.warn("Skipping invalid operation constraint for module {} : {}", this.moduleId, op);
        }
    }

    public void addOperation(Operation op) {
        if (op != null) {
            disableOperations.add(op);
        }
    }

    public boolean hasOperations() {
        return !disableOperations.isEmpty();
    }

    public boolean inRange(Version v) {
        return (versionRange == null) || versionRange.includes(v);
    }

    public boolean canDeploy() {
        return !disableOperations.contains(Operation.DEPLOY);
    }

    public boolean canUndeploy() {
        return !disableOperations.contains(Operation.UNDEPLOY);
    }

    public boolean canStop() {
        return !disableOperations.contains(Operation.STOP);
    }

    public boolean canStart() {
        return !disableOperations.contains(Operation.START);
    }

}
