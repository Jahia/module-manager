/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2022 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.modulemanager.configuration;

import org.jahia.services.modulemanager.util.PropertiesList;
import org.jahia.services.modulemanager.util.PropertiesManager;
import org.jahia.services.modulemanager.util.PropertiesValues;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Component(
        service = OperationConstraintsService.class,
        immediate = true,
        name = "org.jahia.modules.modulemanager.configuration.constraints"
)
public class OperationConstraintsServiceImpl implements OperationConstraintsService {

    private static final Logger logger = LoggerFactory.getLogger(OperationConstraintsServiceImpl.class);
    private static final String CONSTRAINTS_CONFIG_KEY = "moduleLifeCycleConstraints";

    private static final Map<String, OperationConstraint> constraints = new HashMap<>();

    @Activate
    public void activate(Map<String, String> props) {
        logger.debug("Activating configuration service");
        clearConstraintsByPid(props.get("service.pid"));
        parseConfig(props);
    }

    /**
     * Add constraint to list of definitions.
     * Replace constraint if it exists.
     */
    private void parseConfig(Map<String, String> props) {
        logger.debug("Parsing configuration property values");
        String pid = props.get("service.pid");
        PropertiesManager pm = new PropertiesManager(props);
        PropertiesList constraintsProp = pm.getValues().getList(CONSTRAINTS_CONFIG_KEY);
        for (int i = 0; i < constraintsProp.getSize(); i++) {
            PropertiesValues constraintProp = constraintsProp.getValues(i);
            OperationConstraint constraint = OperationConstraint.parse(pid, constraintProp);
            if (constraint != null) {
                constraints.put(constraint.getModuleId(), constraint);
            }
        }
        logger.debug("Configuration parsed");
    }

    @Modified
    public void modified(Map<String,String> props) {
        String pid = props.get("service.pid");
        logger.debug("Updating configuration {}...", pid);
        clearConstraintsByPid(pid);
        parseConfig(props);
        logger.debug("Configuration updated.");
    }

    private void clearConstraintsByPid(String pid) {
        if (pid != null) {
            Set<String> moduleIds = constraints.entrySet().stream()
                    .filter(c -> c.getValue().getPid().equals(pid))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            constraints.keySet().removeAll(moduleIds);
        }
    }

    public OperationConstraint getConstraintForBundle(Bundle b) {
        String moduleId = b.getSymbolicName();
        Version version = b.getVersion();

        OperationConstraint c = constraints.get(moduleId);
        return (c != null && c.inRange(version)) ? c : null;
    }
}
