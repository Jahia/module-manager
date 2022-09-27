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

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Version;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper object representing collection of <code>OperationConstraint</code>s for each given pid
 *
 * @author gflores
 */
public class OperationConstraints {

    Map<String, OperationConstraint> ops = Collections.synchronizedMap(new HashMap<>());

    /**
     * @param o Add to list of OperationConstraint.
     * Do nothing if it already exists i.e. need to remove first before adding.
     */
    public void add(OperationConstraint o) {
        ops.putIfAbsent(o.getPid(), o);
    }

    public void remove(String pid) {
        ops.remove(pid);
    }

    public boolean isEmpty() {
        return ops.isEmpty();
    }

    public boolean inRange(Version v) {
        return ops.values().stream().anyMatch(o -> o.inRange(v));
    }

    public boolean canDeploy(Version v) {
        return ops.values().stream()
                .allMatch(o -> !o.inRange(v) || o.canDeploy());
    }

    public boolean canUndeploy(Version v) {
        return ops.values().stream()
            .allMatch(o -> !o.inRange(v) || o.canUndeploy());
    }

    public boolean canStop(Version v) {
        return ops.values().stream()
                .allMatch(o -> !o.inRange(v) || o.canStop());
    }

    public boolean canStart(Version v) {
        return ops.values().stream()
                .allMatch(o -> !o.inRange(v) || o.canStart());
    }
}
