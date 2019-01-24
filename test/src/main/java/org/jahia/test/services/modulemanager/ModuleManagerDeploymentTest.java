/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
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
        Bundle bundle = BundleUtils.getBundleBySymbolicName("dummy1", "1.0.0");
        if (bundle != null) {
            bundle.uninstall();
        }
    }

    private void assertModuleState(int expectedState) {
        Bundle bundle = BundleUtils.getBundleBySymbolicName("dummy1", "1.0.0");
        assertNotNull(bundle);
        assertEquals(expectedState, bundle.getState());
    }

    private ModuleManager getModuleManager() {
        return (ModuleManager) SpringContextSingleton.getBean("ModuleManager");
    }

    private void installModule() {
        getModuleManager().install(
                managerService.getTemplatePackageById("jahia-test-module").getResource("dummy1-1.0.jar"), null);

        assertModuleState(Bundle.INSTALLED);
    }

    private void startModule() {
        getModuleManager().start("org.jahia.modules/dummy1/1.0.0", null);

        assertModuleState(Bundle.ACTIVE);
        assertTrue(managerService.getTemplatePackageRegistry().getAvailableVersionsForModule("dummy1")
                .contains(new ModuleVersion("1.0")));
    }

    private void stopModule() {
        getModuleManager().stop("org.jahia.modules/dummy1/1.0.0", null);

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
        getModuleManager().uninstall("org.jahia.modules/dummy1/1.0.0", null);

        assertNull(BundleUtils.getBundleBySymbolicName("dummy1", "1.0.0"));
    }
}
