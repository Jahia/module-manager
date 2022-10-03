package org.jahia.modules.modulemanager.configuration;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Service for processing operation constraints specified through configuration
 */
public interface OperationConstraintsService {

    /**
     * @param bundle The bundle object to match constraint against.
     * @return OperationConstraints for a given bundle/version, or null if it doesn't exist.
     */
    OperationConstraints getConstraintForBundle(Bundle bundle);

    /**
     * @param symbolicName bundle symbolic name to check.
     * @param version specific version of bundle to check.
     * @return OperationConstraints for a given bundle/version, or null if it doesn't exist.
     */
    OperationConstraints getConstraintForBundle(String symbolicName, Version version);
}
