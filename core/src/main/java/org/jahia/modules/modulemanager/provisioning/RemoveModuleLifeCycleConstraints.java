package org.jahia.modules.modulemanager.provisioning;

import org.jahia.services.provisioning.ExecutionContext;
import org.jahia.services.provisioning.Operation;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component(service = Operation.class, property = "type=removeModuleLifeCycleConstraints")
public class RemoveModuleLifeCycleConstraints implements Operation {

    private static final Logger logger = LoggerFactory.getLogger(RemoveModuleLifeCycleConstraints.class);

    private static final String OPERATION_KEY = "removeModuleLifeCycleConstraints";
    private ModuleLifeCycleConstraintsService moduleLifeCycleConstraintsService;

    @Activate
    public void activate() {
        logger.info("Activated AddModuleLifeCycleConstraints operation");
    }

    @Reference
    public void setModuleLifeCycleConstraintsService(ModuleLifeCycleConstraintsServiceInterface moduleLifeCycleConstraintsService) {
        this.moduleLifeCycleConstraintsService = (ModuleLifeCycleConstraintsService) moduleLifeCycleConstraintsService;
    }

    @Override
    public boolean canHandle(Map<String, Object> map) {
        return map.containsKey(OPERATION_KEY);
    }

    @Override
    public void perform(Map<String, Object> map, ExecutionContext executionContext) {
        if (!SettingsBean.getInstance().isProcessingServer()) {
            logger.info("Operation {} can only be performed on processing server", OPERATION_KEY);
            return;
        }

        String moduleId = (String) map.get("moduleId");
        String version = (String) map.get("version");

        if (moduleId == null || version == null) {
            // Log properly
            logger.error("Operation {} cannot be be performed as some or all parameters are missing", OPERATION_KEY);
            return;
        }

        String[] range = version.replaceAll("[\\[\\]]", "").split(",");
        List<Version> versionRange = Arrays.asList(new Version(range[0].trim()), new Version(range[1].trim()));
        try {
            ModuleLifeCycleConstraint mc = new ModuleLifeCycleConstraint(moduleId, versionRange, null);
            moduleLifeCycleConstraintsService.removeConstraint(mc);
        } catch (RepositoryException e) {
            logger.error("Failed to remove constraint while performing operation {}: ", OPERATION_KEY, e);
        }
    }
}
