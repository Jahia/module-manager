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
 *     Copyright (C) 2002-2023 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.modulemanager.flow;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/** Wrapper class to assess any wiring change
 * Comparison is done through bundle capability and its provider state */
public class ModuleWiring {

    private static final Logger logger = LoggerFactory.getLogger(ModuleWiring.class);

    private final BundleCapability capability;
    private final String capabilityName;
    private final String providerState;
    private final String providerStr;

    private static final String WIRING_PKG_ATTR = "osgi.wiring.package";

    public ModuleWiring(BundleWire wire) {
        this.capability = wire.getCapability();
        this.capabilityName = (String) wire.getCapability().getAttributes().get(WIRING_PKG_ATTR);

        Bundle providerBundle = wire.getProvider().getBundle();
        this.providerStr = String.format("%s/%s[%s]",
                providerBundle.getSymbolicName(), providerBundle.getVersion(), providerBundle.getBundleId());
        this.providerState = toState(providerBundle.getState());
    }

    public BundleCapability getCapability() {
        return capability;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModuleWiring)) return false;
        return capability.equals(((ModuleWiring) obj).getCapability());
    }

    @Override
    public int hashCode() {
        return Objects.hash(capability, providerStr, providerState);
    }

    @Override
    public String toString() {
        return String.format("%s-%s<%s>", capabilityName, providerStr, providerState);
    }

    public String toState(int state) {
        switch (state) {
            case Bundle.UNINSTALLED:
                return "UNINSTALLED";
            case Bundle.INSTALLED:
                return "INSTALLED";
            case Bundle.RESOLVED:
                return "RESOLVED";
            case Bundle.STARTING:
                return "STARTING";
            case Bundle.STOPPING:
                return "STOPPING";
            case Bundle.ACTIVE:
                return "ACTIVE";
        }
        return "";
    }

    public static Map<String, ModuleWiring> getWirings(Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null) {
            List<ModuleWiring> wiringList = wiring.getRequiredWires(WIRING_PKG_ATTR).stream()
                    .map(ModuleWiring::new)
                    .collect(Collectors.toList());

            Map<String, ModuleWiring> result = new HashMap<>(wiringList.size());
            wiringList.forEach(w -> result.put(w.getCapabilityName(), w));
            return result;
        } else {
            logger.info("Unable to get wiring for bundle {}.", bundle);
        }
        return null;
    }
}
