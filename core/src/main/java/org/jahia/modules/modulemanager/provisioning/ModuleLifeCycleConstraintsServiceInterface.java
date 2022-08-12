package org.jahia.modules.modulemanager.provisioning;

import org.osgi.framework.Bundle;

import javax.jcr.RepositoryException;

public interface ModuleLifeCycleConstraintsServiceInterface {
    boolean canDeploy(Bundle bundle);
    boolean canStop(Bundle bundle);
    boolean canStart(Bundle bundle);
    boolean canUndeploy(Bundle bundle);
    void addConstraint(ModuleLifeCycleConstraint moduleLifeCycleConstraint) throws RepositoryException;
    void readConstraintsFromJCR() throws RepositoryException;
}
