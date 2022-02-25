/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.modulemanager.flow;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * State information for a module version, including possible actions.
 * 
 * @author Sergiy Shyrkov
 */
public class ModuleVersionState implements Serializable {

    private static final long serialVersionUID = 7311222686981884149L;

    private boolean isInstalled = true;

    private boolean canBeStarted;

    private boolean canBeStopped;

    private boolean canBeUninstalled;

    private boolean canBeReinstalled = false;

    private Set<String> dependencies = new TreeSet<String>();

    private boolean systemDependency;

    private Set<String> unresolvedDependencies = new TreeSet<String>();

    private Set<String> usedInSites = new TreeSet<String>();

    public Set<String> getDependencies() {
        return dependencies;
    }

    public Set<String> getUnresolvedDependencies() {
        return unresolvedDependencies;
    }

    public Set<String> getUsedInSites() {
        return usedInSites;
    }

    public boolean isCanBeStarted() {
        return canBeStarted;
    }

    public boolean isCanBeStopped() {
        return canBeStopped;
    }

    public boolean isCanBeUninstalled() {
        return canBeUninstalled;
    }

    public boolean isCanBeReinstalled() {
        return canBeReinstalled;
    }

    public boolean isSystemDependency() {
        return systemDependency;
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    public void setInstalled(boolean isInstalled) {
        this.isInstalled = isInstalled;
    }

    public void setCanBeStarted(boolean canBeStarted) {
        this.canBeStarted = canBeStarted;
    }

    public void setCanBeStopped(boolean canBeStopped) {
        this.canBeStopped = canBeStopped;
    }

    public void setCanBeUninstalled(boolean canBeUninstalled) {
        this.canBeUninstalled = canBeUninstalled;
    }

    public void setCanBeReinstalled(boolean canBeReinstalled) {
        this.canBeReinstalled = canBeReinstalled;
    }

    public void setSystemDependency(boolean systemDependency) {
        this.systemDependency = systemDependency;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}