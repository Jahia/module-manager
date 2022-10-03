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

import org.jahia.osgi.BundleUtils;
import org.osgi.framework.Version;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;

import static org.jahia.services.modulemanager.models.JahiaDepends.toOsgiVersion;

public class OperationConstraintsUtil {

    /** Check for operation constraints in config and add to UI messages if disabled */
    public static boolean checkDeployConstraint(String symbolicName, String version, MessageContext context) {
        OperationConstraintsService opConstraintsService = BundleUtils.getOsgiService(OperationConstraintsService.class, null);
        if (opConstraintsService != null) {
            Version v = new Version(toOsgiVersion(version));
            OperationConstraints ops = opConstraintsService.getConstraintForBundle(symbolicName, v);
            if (ops != null && !ops.canDeploy(v)) {
                context.addMessage(new MessageBuilder().source("moduleFile")
                        .code("serverSettings.manageModules.install.module.constraint")
                        .args(symbolicName, version)
                        .error().build());
                return false;
            }
        }
        return true;
    }
}
