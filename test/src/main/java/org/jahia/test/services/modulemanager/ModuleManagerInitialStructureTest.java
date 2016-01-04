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

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.test.JahiaTestCase;
import org.junit.*;

import javax.jcr.RepositoryException;

import static org.junit.Assert.*;

/**
 * Created by achaabni on 20/11/15.
 */
public class ModuleManagerInitialStructureTest extends JahiaTestCase {

    private static JCRSessionWrapper session;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        session = JCRTemplate.getInstance().getSessionFactory().getCurrentSystemSession("default", null, null);
    }


    @Test
    public void testModuleManagementNodePresence() throws RepositoryException {
        assertNotNull(session.getNode("/module-management"));
    }

    @Test
    public void testBundlesNodePresence() throws RepositoryException {
        assertNotNull(session.getNode("/module-management/bundles"));
    }

    @Test
    public void testClusterNodesNodePresence() throws RepositoryException {
        assertNotNull(session.getNode("/module-management/nodes"));
    }

    @Test
    public void testOperationsNodePresence() throws RepositoryException {
        assertNotNull(session.getNode("/module-management/operations"));
    }

    @Test
    public void testOperationLogNodePresence() throws RepositoryException {
        assertNotNull(session.getNode("/module-management/operationLog"));
    }


    @Test
    public void testBundleChildrenNotEmpty() throws RepositoryException {
        assertTrue(session.getNode("/module-management/bundles").hasNodes());
    }

    @Test
    public void testClusterNodesChildrenNotEmpty() throws RepositoryException {
        assertTrue(session.getNode("/module-management/nodes").hasNodes());
    }

    @Test
    public void testEqualBundleSize() throws RepositoryException {

            long bundlesSize = session.getNode("/module-management/bundles").getNodes().getSize();
            JCRNodeIteratorWrapper nodes = session.getNode("/module-management/nodes").getNodes();
            while (nodes.hasNext()) {
                String clusterNodePath = ((JCRNodeWrapper) nodes.next()).getPath();
                JCRNodeIteratorWrapper clusterBundleNodes = session.getNode(clusterNodePath + "/bundles").getNodes();
                assertEquals(bundlesSize, clusterBundleNodes.getSize());
            }
    }
}
