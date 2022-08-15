package org.jahia.modules.modulemanager.provisioning;

import org.jahia.services.content.*;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component(service = ModuleLifeCycleConstraintsServiceInterface.class, immediate = true)
public class ModuleLifeCycleConstraintsService implements ModuleLifeCycleConstraintsServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(ModuleLifeCycleConstraintsService.class);

    // TODO get module path dynamically or put it somewhere else depending on functional requirements
    private static final String MODULE_PATH = "/modules/module-manager/2.6.0-SNAPSHOT";
    private static final String CONSTRAINTS_NODE = "module-lifecycle-constraints";

    private List<ModuleLifeCycleConstraint> constraints = new ArrayList<>();


    @Activate
    public void activate() {
        logger.info("Activated ModuleLifeCycleConstraintsService");
        try {
            readConstraintsFromJCR();
            logger.info("Read module lifecycle constraint from JCR");
        } catch (RepositoryException e) {
            logger.error("Failed to read lifecycle module constraint from JCR");
        }
    }

    @Override
    public boolean canDeploy(Bundle bundle) {
        // TODO this is a hack since we can't have rules in none processing server, maybe we can have a listener
        // or implement a dedicated update method if we are on none processing server
        if (!SettingsBean.getInstance().isProcessingServer()) {
            try {
                constraints.clear();
                readConstraintsFromJCR();
                logger.info("Constraints have been read: {}", constraints.size());
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }

        Optional<ModuleLifeCycleConstraint> opt = constraints.stream().filter(con -> con.getModuleId().equals(bundle.getSymbolicName()) && versionWithinRange(bundle, con)).findFirst();

        if (opt.isPresent()) {
            ModuleLifeCycleConstraint c = opt.get();
            return !c.getDisableOperations().contains("deploy");
        }

        return true;
    }

    @Override
    public boolean canStop(Bundle bundle) {
        Optional<ModuleLifeCycleConstraint> opt = constraints.stream().filter(con -> con.getModuleId().equals(bundle.getSymbolicName()) && versionWithinRange(bundle, con)).findFirst();

        if (opt.isPresent()) {
            ModuleLifeCycleConstraint c = opt.get();
            return !c.getDisableOperations().contains("stop");
        }

        return true;
    }

    @Override
    public boolean canStart(Bundle bundle) {
        Optional<ModuleLifeCycleConstraint> opt = constraints.stream().filter(con -> con.getModuleId().equals(bundle.getSymbolicName()) && versionWithinRange(bundle, con)).findFirst();

        if (opt.isPresent()) {
            ModuleLifeCycleConstraint c = opt.get();
            return !c.getDisableOperations().contains("start");
        }

        return true;
    }

    @Override
    public boolean canUndeploy(Bundle bundle) {
        Optional<ModuleLifeCycleConstraint> opt = constraints.stream().filter(con -> con.getModuleId().equals(bundle.getSymbolicName()) && versionWithinRange(bundle, con)).findFirst();

        if (opt.isPresent()) {
            ModuleLifeCycleConstraint c = opt.get();
            return !c.getDisableOperations().contains("undeploy");
        }

        return true;
    }

    @Override
    public ModuleLifeCycleConstraint getConstraintForBundle(Bundle bundle) {
        Optional<ModuleLifeCycleConstraint> opt = constraints.stream()
                .filter(con -> con.getModuleId().equals(bundle.getSymbolicName()) && versionWithinRange(bundle, con))
                .findFirst();
        return opt.orElse(null);

    }

    @Override
    public void addConstraint(ModuleLifeCycleConstraint moduleLifeCycleConstraint) throws RepositoryException {
        boolean result = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {

            @Override
            public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper folder = session.getNode(MODULE_PATH + "/" + CONSTRAINTS_NODE);
                // TODO make sure nodename is OK
                String nodeName = getNodeNameForConstraint(moduleLifeCycleConstraint);
                if (folder.hasNode(nodeName)) {
                    folder.getNode(nodeName).remove();
                }
                // TODO check for null
                JCRNodeWrapper constraint = folder.addNode(nodeName, "jnt:moduleLifeCycleConstraint");
                constraint.setProperty("moduleId", moduleLifeCycleConstraint.getModuleId());
                constraint.setProperty("versionRange", moduleLifeCycleConstraint.getVersionRange().stream().map(Version::toString).toArray(String[]::new));
                constraint.setProperty("disableOperations", moduleLifeCycleConstraint.getDisableOperations().toArray(new String[0]));
                constraint.setProperty("warning", moduleLifeCycleConstraint.getWarning());
                constraint.setProperty("info", moduleLifeCycleConstraint.getInfo());
                constraint.setProperty("alert", moduleLifeCycleConstraint.getAlert());
                constraint.setProperty("error", moduleLifeCycleConstraint.getError());
                session.save();
                return true;
            }
        });

        if (result) {
            addConstraintToList(moduleLifeCycleConstraint);
        }
    }

    @Override
    public void removeConstraint(ModuleLifeCycleConstraint moduleLifeCycleConstraint) throws RepositoryException {
        boolean result = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {

            @Override
            public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper folder = session.getNode(MODULE_PATH + "/" + CONSTRAINTS_NODE);
                String nodeName = getNodeNameForConstraint(moduleLifeCycleConstraint);
                if (folder.hasNode(nodeName)) {
                    folder.getNode(nodeName).remove();
                }
                session.save();
                return true;
            }
        });

        if (result) {
            removeConstraintFromList(moduleLifeCycleConstraint);
        }
    }

    @Override
    public void readConstraintsFromJCR() throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {

            @Override
            public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper folder = session.getNode(MODULE_PATH + "/" + CONSTRAINTS_NODE);
                JCRContentUtils.getChildrenOfType(folder, "jnt:moduleLifeCycleConstraint").forEach(mc -> {
                    ModuleLifeCycleConstraint newMC = new ModuleLifeCycleConstraint();
                    newMC.setModuleId(mc.getPropertyAsString("moduleId"));
                    newMC.setWarning(mc.getPropertyAsString("warning"));
                    newMC.setInfo(mc.getPropertyAsString("info"));
                    newMC.setAlert(mc.getPropertyAsString("alert"));
                    newMC.setError(mc.getPropertyAsString("error"));

                    try {

                        JCRValueWrapper[] values = mc.getProperty("versionRange").getValues();
                        newMC.setVersionRange(Arrays.asList(new Version(values[0].getString()), new Version(values[1].getString())));

                        JCRValueWrapper[] disabled = mc.getProperty("disableOperations").getValues();
                        List<String> operations = new ArrayList<>();
                        for (JCRValueWrapper value : disabled) {
                            operations.add(value.getString());
                        }
                         newMC.setDisableOperations(operations);
                    } catch (Exception e) {
                        // TODO say something
                        logger.error(e.getMessage(), e);
                    }

                    addConstraintToList(newMC);
                });
                return true;
            }
        });
    }

    private void addConstraintToList(ModuleLifeCycleConstraint moduleLifeCycleConstraint) {
        constraints.removeIf(c -> c.idAndVersionRangeIsSame(moduleLifeCycleConstraint));
        constraints.add(moduleLifeCycleConstraint);
    }

    private void removeConstraintFromList(ModuleLifeCycleConstraint moduleLifeCycleConstraint) {
        constraints.removeIf(c -> c.idAndVersionRangeIsSame(moduleLifeCycleConstraint));
    }

    private boolean versionWithinRange(Bundle bundle, ModuleLifeCycleConstraint moduleLifeCycleConstraint) {
        List<Version> versionRange = moduleLifeCycleConstraint.getVersionRange();
        return bundle.getVersion().compareTo(versionRange.get(0)) >= 0 && bundle.getVersion().compareTo(versionRange.get(1)) <= 0;
    }

    private String getNodeNameForConstraint(ModuleLifeCycleConstraint moduleLifeCycleConstraint) {
        return String.format("%s-%s-%s",
                moduleLifeCycleConstraint.getModuleId(),
                moduleLifeCycleConstraint.getVersionRange().get(0).toString(),
                moduleLifeCycleConstraint.getVersionRange().get(1).toString());
    }
}
