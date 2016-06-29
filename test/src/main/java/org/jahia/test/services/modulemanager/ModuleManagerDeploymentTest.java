/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.io.FileUtils;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.modulemanager.ModuleManager;
import org.jahia.services.modulemanager.ModuleManagerHelper;
import org.jahia.services.modulemanager.util.ModuleUtils;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.test.JahiaTestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

import javax.jcr.RepositoryException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * ModuleManagerDeploymentTest
 */
public class ModuleManagerDeploymentTest extends JahiaTestCase {

    private static JCRSessionWrapper session;

    private ModuleManager getModuleManager() {
        return (ModuleManager) SpringContextSingleton.getBean("ModuleManager");
    }

    private static JahiaTemplateManagerService managerService = ServicesRegistry.getInstance().getJahiaTemplateManagerService();

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        session = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession("default", null, null);
    }


    @Test
    public void testModuleManagementNodePresence() throws RepositoryException {
        assertNotNull(session.getNode("/module-management"));
    }

    public void testBundlesNodePresence() throws RepositoryException {
        assertNotNull(session.getNode("/module-management/bundles"));
    }

    @Test
    public void testInstallArticle() throws RepositoryException {
        try {
            File tmpFile = File.createTempFile("module",".jar");
            InputStream stream = managerService.getTemplatePackageById("jahia-test-module")
                    .getResource("dummy1-1.0.jar").getInputStream();
            FileUtils.copyInputStreamToFile(ModuleUtils.addModuleDependencies(stream), tmpFile);
            getModuleManager().install(new FileSystemResource(tmpFile),"");
            tmpFile.delete();
        } catch (IOException e) {
            fail(e.toString());
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {

        }
        assertTrue(ModuleManagerHelper.isModuleExists(managerService.getTemplatePackageRegistry(), "dummy1", "1.0", null));

    }

    @Test
    public void testStopArticle() throws RepositoryException {
        getModuleManager().stop("org.jahia.modules/dummy1/1.0.0","");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }
    }

    @Test
    public void testStartArticle() throws RepositoryException {
        getModuleManager().start("org.jahia.modules/dummy1/1.0.0", "");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }
    }

    @Test
    public void testUninstallArticle() throws RepositoryException {
        getModuleManager().uninstall("org.jahia.modules/dummy1/1.0.0","");
    }
}
