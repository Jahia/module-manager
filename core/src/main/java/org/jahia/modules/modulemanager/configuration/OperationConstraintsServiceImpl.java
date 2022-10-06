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
import org.osgi.service.component.annotations.Deactivate;
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
    private static final Map<String, OperationConstraints> constraints = Collections.synchronizedMap(new HashMap<>());

    @Activate
    @Modified
    public void activate(Map<String, String> props) {
        String pid = props.get("service.pid");
        logger.debug("Adding/updating configuration {}...", pid);
        clearConstraintsByPid(pid);
        parseConfig(props);
    }

    @Deactivate
    public void deactivate(Map<String, String> props) {
        String pid = props.get("service.pid");
        logger.debug("Removing configuration {}...", pid);
        clearConstraintsByPid(pid);
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
                OperationConstraints ops = constraints.get(constraint.getModuleId());
                if (ops == null) {
                    ops = new OperationConstraints();
                }
                ops.add(constraint);
                constraints.put(constraint.getModuleId(), ops);
            }
        }
        logger.debug("Configuration parsed");
    }

    private void clearConstraintsByPid(String pid) {
        if (pid != null) {
            /* Go through each OperationConstraints element and remove any constraint associated with 'pid' configuration.
             * If any OperationConstraints is empty after removal, then also remove them from constraints map as well. */
            Set<String> moduleIds = constraints.entrySet().stream()
                    .filter(e -> {
                        OperationConstraints ops = e.getValue();
                        ops.remove(pid);
                        return ops.isEmpty();
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            constraints.keySet().removeAll(moduleIds);
        }
    }

    @Override
    public OperationConstraints getConstraintForBundle(Bundle b) {
        return getConstraintForBundle(b.getSymbolicName(), b.getVersion());
    }

    @Override
    public OperationConstraints getConstraintForBundle(String symbolicName, Version version) {
        OperationConstraints c = constraints.get(symbolicName);
        return (c != null && c.inRange(version)) ? c : null;
    }
}
