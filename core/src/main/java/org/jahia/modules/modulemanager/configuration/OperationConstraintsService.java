package org.jahia.modules.modulemanager.configuration;

import org.osgi.framework.Bundle;

/**
 * Service for processing operation constraints specified through configuration
 */
public interface OperationConstraintsService {
    OperationConstraints getConstraintForBundle(Bundle bundle);
}
