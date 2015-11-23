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
