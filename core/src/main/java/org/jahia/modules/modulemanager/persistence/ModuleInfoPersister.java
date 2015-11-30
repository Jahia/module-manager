/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.2 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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
package org.jahia.modules.modulemanager.persistence;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.jahia.modules.modulemanager.impl.BundleServiceImpl;
import org.jahia.modules.modulemanager.model.BinaryFile;
import org.jahia.modules.modulemanager.model.Bundle;
import org.jahia.modules.modulemanager.model.ClusterNode;
import org.jahia.modules.modulemanager.model.ClusterNodeInfo;
import org.jahia.modules.modulemanager.model.ModuleManagement;
import org.jahia.modules.modulemanager.model.NodeBundle;
import org.jahia.modules.modulemanager.model.NodeOperation;
import org.jahia.modules.modulemanager.model.Operation;
import org.jahia.services.content.JCRAutoSplitUtils;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for managing the JCR structure and information about deployment of modules.
 * 
 * @author Sergiy Shyrkov
 */
public class ModuleInfoPersister {

    /**
     * Callback interface for operations, that are executed in OCM.
     * 
     * @author Sergiy Shyrkov
     *
     * @param <T>
     *            the result return type of the operation
     */
    public interface OCMCallback<T> {
        T doInOCM(ObjectContentManager ocm) throws RepositoryException;
    }

    private static final Logger logger = LoggerFactory.getLogger(ModuleInfoPersister.class);

    private static final String ROOT_NODE_PATH = "/module-management";

    private BundleServiceImpl bundleService;

    private ClusterNodeInfo clusterNodeInfo;

    private AnnotationMapperImpl mapper;

    private String operationLogAutoSplitConfig;

    /**
     * Checks if the specified bundle is already installed on the current node.
     * 
     * @param uniqueBundleKey
     *            the unique key of the bundle
     * @param checksum
     *            the bundle checksum
     * @return <code>true</code> if the specified bundle is already installed on the current node; <code>false</code> otherwise
     * @throws RepositoryException
     *             in case of a JCR error
     */
    public boolean alreadyInstalled(final String uniqueBundleKey, final String checksum) throws RepositoryException {
        return doExecute(new OCMCallback<Boolean>() {
            @Override
            public Boolean doInOCM(ObjectContentManager ocm) throws RepositoryException {
                Bundle existingBundle = (Bundle) ocm.getObject(Bundle.class,
                        ROOT_NODE_PATH + "/bundles/" + uniqueBundleKey);
                return (existingBundle != null && existingBundle.getChecksum() != null
                        && StringUtils.equals(existingBundle.getChecksum(), checksum) && ocm.objectExists(
                                ROOT_NODE_PATH + "/nodes/" + clusterNodeInfo.getId() + "/bundles/" + uniqueBundleKey));
            }
        });
    }

    /**
     * Executes the provided callback in the OCM context.
     * 
     * @param callback
     *            a callback to be executed
     * @return the result of the callback execution
     * @throws RepositoryException
     *             in case of a JCR/OCM error
     */
    public <T> T doExecute(final OCMCallback<T> callback) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<T>() {
            @Override
            public T doInJCR(JCRSessionWrapper session) throws RepositoryException {
                try {
                    return callback.doInOCM(new ObjectContentManagerImpl(session, getMapper()));
                } catch (RuntimeException e) {
                    throw new RepositoryException(e);
                }
            }
        });
    }

    /**
     * Returns a list of known cluster nodes.
     * 
     * @param ocm
     *            current instance of the object manager
     * @return a list of known cluster nodes
     * @throws RepositoryException
     *             in case of a JCR error
     */
    public List<ClusterNode> getClusterNodes(ObjectContentManager ocm) throws RepositoryException {
        NodeIterator it = ocm.getSession().getNode("/module-management/nodes").getNodes();

        List<ClusterNode> nodes = new LinkedList<>();
        while (it.hasNext()) {
            Node nextNode = it.nextNode();
            if (nextNode.isNodeType("jmm:node")) {
                nodes.add((ClusterNode) ocm.getObject(nextNode.getPath()));
            }
        }

        return nodes;
    }

    private Mapper getMapper() {
        // TODO replace annotations with XML descriptor to eliminate dependency of object model to jackrabbit-ocm?
        if (mapper == null) {
            ReflectionUtils.setClassLoader(getClass().getClassLoader());

            @SuppressWarnings("rawtypes")
            List<Class> classes = new LinkedList<Class>();
            classes.add(ModuleManagement.class);
            classes.add(Bundle.class);
            classes.add(BinaryFile.class);
            classes.add(Operation.class);
            classes.add(ClusterNode.class);
            classes.add(NodeBundle.class);
            classes.add(NodeOperation.class);

            mapper = new AnnotationMapperImpl(classes);
        }
        return mapper;
    }

    private ModuleManagement getModuleManagement(ObjectContentManager ocm) {
        ModuleManagement mgt = (ModuleManagement) ocm.getObject(ModuleManagement.class, ROOT_NODE_PATH);
        if (mgt == null) {
            logger.info("Creating initial JCR structure skeletong for " + ROOT_NODE_PATH);
            try {
                ocm.insert(new ModuleManagement(ROOT_NODE_PATH));
                Node opLog = ocm.getSession().getNode(ROOT_NODE_PATH).addNode("operationLog", "jmm:operations");
                JCRAutoSplitUtils.enableAutoSplitting((JCRNodeWrapper) opLog, operationLogAutoSplitConfig,
                        "jmm:operations");
                ocm.save();

                logger.info("Done creating initial JCR structure skeletong for " + ROOT_NODE_PATH);
            } catch (ObjectContentManagerException | RepositoryException e) {
                // is already created
            }
            mgt = (ModuleManagement) ocm.getObject(ModuleManagement.class, ROOT_NODE_PATH);
        }

        return mgt;
    }

    /**
     * Returns the next module operation (node-level) from the queue to be processed.
     *
     * @param clusterNodeId
     *            the identifier of the cluster node to check operations for
     * @return the next module operation (node level) from the queue to be processed
     * @throws RepositoryException
     *             in case of a repository access error
     */
    public NodeOperation getNextNodeOperation(final String clusterNodeId) throws RepositoryException {
        return doExecute(new OCMCallback<NodeOperation>() {
            @Override
            public NodeOperation doInOCM(ObjectContentManager ocm) throws RepositoryException {
                Node node = ocm.getSession().getNode(ROOT_NODE_PATH + "/nodes/" + clusterNodeId + "/operations");
                NodeIterator ops = node.getNodes();
                if (ops.hasNext()) {
                    return (NodeOperation) ocm.getObject(NodeOperation.class, ops.nextNode().getPath());
                }

                return null;
            }
        });
    }

    /**
     * Returns the next module operation (global level) from the queue to be processed.
     * 
     * @return the next module operation (global level) from the queue to be processed
     * @throws RepositoryException
     *             in case of a repository access error
     */
    public Operation getNextOperation() throws RepositoryException {
        return doExecute(new OCMCallback<Operation>() {
            @Override
            public Operation doInOCM(ObjectContentManager ocm) throws RepositoryException {
                Node node = ocm.getSession().getNode(ROOT_NODE_PATH + "/operations");
                NodeIterator ops = node.getNodes();
                if (ops.hasNext()) {
                    return (Operation) ocm.getObject(Operation.class, ops.nextNode().getPath());
                }

                return null;
            }
        });
    }

    public void setBundleService(BundleServiceImpl bundleService) {
        this.bundleService = bundleService;
    }

    public void setClusterNodeInfo(ClusterNodeInfo clusterNodeInfo) {
        this.clusterNodeInfo = clusterNodeInfo;
    }

    public void setOperationLogAutoSplitConfig(String operationLogAutoSplitConfig) {
        this.operationLogAutoSplitConfig = operationLogAutoSplitConfig;
    }

    protected void start() {
        try {
            doExecute(new OCMCallback<Object>() {
                @Override
                public Object doInOCM(ObjectContentManager ocm) throws RepositoryException {
                    validateJcrTreeStructure(ocm);
                    return null;
                }
            });
        } catch (RepositoryException e) {
            logger.error("Unable to validate module management JCR tree structure. Cause: " + e.getMessage(), e);
        }
    }

    private void validateJcrTreeStructure(ObjectContentManager ocm) throws RepositoryException {
        // 1) ensure module-management skeleton is created in JCR
        ModuleManagement mgt = getModuleManagement(ocm);

        Map<String, String> bundeStates = null;
        if (mgt.getBundles().isEmpty()) {
            // 2) populate information about available bundles
            logger.info("Start populating information about available module bundles...");
            long startTime = System.currentTimeMillis();

            bundeStates = bundleService.populateBundles(mgt);
            ocm.update(mgt);
            ocm.save();

            logger.info("Done populating information about available module bundles in {} ms",
                    System.currentTimeMillis() - startTime);
            mgt = getModuleManagement(ocm);
        }

        if (!mgt.getNodes().containsKey(clusterNodeInfo.getId())) {
            // 3) create cluster node
            ClusterNode cn = new ClusterNode(clusterNodeInfo.getId(), clusterNodeInfo.isProcessingServer());
            cn.setPath(ROOT_NODE_PATH + "/nodes/" + clusterNodeInfo.getId());
            ocm.insert(cn);
            Node opLog = ocm.getSession().getNode(cn.getPath()).addNode("operationLog", "jmm:nodeOperations");
            JCRAutoSplitUtils.enableAutoSplitting((JCRNodeWrapper) opLog, operationLogAutoSplitConfig,
                    "jmm:nodeOperations");

            // 4) populate the information about node bundles
            logger.info("Start populating information about module bundles for node {}...", clusterNodeInfo.getId());
            long startTime = System.currentTimeMillis();

            ClusterNode clusterNodeToUpdate = (ClusterNode) ocm.getObject(ClusterNode.class, cn.getPath());
            bundleService.populateNodeBundles(clusterNodeToUpdate, getModuleManagement(ocm).getBundles(), bundeStates);
            ocm.update(clusterNodeToUpdate);
            ocm.save();

            logger.info("Done populating information about module bundles for node {} in {} ms",
                    clusterNodeInfo.getId(), System.currentTimeMillis() - startTime);
        }
    }
}
