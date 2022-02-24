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
package org.jahia.test.services.modulemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.RepositoryException;

import org.jahia.osgi.BundleUtils;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.modulemanager.ModuleManager;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.templates.ModuleVersion;
import org.jahia.test.JahiaTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * ModuleManagerDeploymentTest
 */
public class ModuleManagerDeploymentTest extends JahiaTestCase {

    private static JahiaTemplateManagerService managerService = ServicesRegistry.getInstance()
            .getJahiaTemplateManagerService();

    private static JCRSessionWrapper session;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        session = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession("default", null, null);
        uninstallTestModule();
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        uninstallTestModule();
    }

    private static void uninstallTestModule() throws BundleException {
        Bundle bundle = BundleUtils.getBundleBySymbolicName("dummy1", "2.0.0");
        if (bundle != null) {
            bundle.uninstall();
        }
    }

    private void assertModuleState(int expectedState) {
        Bundle bundle = BundleUtils.getBundleBySymbolicName("dummy1", "2.0.0");
        assertNotNull(bundle);
        assertEquals(expectedState, bundle.getState());
    }

    private ModuleManager getModuleManager() {
        return (ModuleManager) SpringContextSingleton.getBean("ModuleManager");
    }

    private void installModule() {
        getModuleManager().install(
                managerService.getTemplatePackageById("jahia-test-module").getResource("dummy1-2.0.0.jar"), null);

        assertModuleState(Bundle.INSTALLED);
    }

    private void startModule() {
        getModuleManager().start("org.jahia.modules/dummy1/2.0.0", null);

        assertModuleState(Bundle.ACTIVE);
        assertTrue(managerService.getTemplatePackageRegistry().getAvailableVersionsForModule("dummy1")
                .contains(new ModuleVersion("2.0.0")));
    }

    private void stopModule() {
        getModuleManager().stop("org.jahia.modules/dummy1/2.0.0", null);

        assertModuleState(Bundle.RESOLVED);
    }

    public void testBundlesNodePresence() throws RepositoryException {
        assertNotNull(session.getNode("/module-management/bundles"));
    }

    @Test
    public void testModuleManagementNodePresence() throws RepositoryException {
        assertNotNull(session.getNode("/module-management"));
    }

    @Test
    public void testModuleOperations() {
        installModule();

        startModule();

        stopModule();

        uninstallModule();
    }

    private void uninstallModule() {
        getModuleManager().uninstall("org.jahia.modules/dummy1/2.0.0", null);

        assertNull(BundleUtils.getBundleBySymbolicName("dummy1", "2.0.0"));
    }
}
