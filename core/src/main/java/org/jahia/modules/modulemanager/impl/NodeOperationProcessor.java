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
package org.jahia.modules.modulemanager.impl;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.jahia.modules.modulemanager.ModuleManagementException;
import org.jahia.modules.modulemanager.model.ClusterNodeInfo;
import org.jahia.modules.modulemanager.model.NodeOperation;
import org.jahia.modules.modulemanager.model.Operation;
import org.jahia.modules.modulemanager.persistence.ModuleInfoPersister;
import org.jahia.modules.modulemanager.persistence.ModuleInfoPersister.OCMCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster node level operation processor, that is responsible for executing module operations on the current cluster node.
 * 
 * @author Sergiy Shyrkov
 */
public class NodeOperationProcessor {

    private static final Logger logger = LoggerFactory.getLogger(NodeOperationProcessor.class);

    private ClusterNodeInfo clusterNodeInfo;

    private String operationLogPath;

    private ModuleInfoPersister persister;

    /**
     * Performs the check if the operation can be started or not, which is based on the dependencies of it.
     * 
     * @param op
     *            the operation to check
     * @return <code>true</code> if the operation could be started; <code>false</code> otherwise
     * @throws RepositoryException
     */
    private boolean canStart(final NodeOperation op) throws RepositoryException {
        final List<String> dependsOn = op.getDependsOn();
        final String opPath = op.getPath();
        if (dependsOn == null || dependsOn.isEmpty()) {
            // this operation does not depend on others -> give it a go
            logger.info("No dependencies for operation {}. It can be started immediately.", opPath);
            return true;
        }
        logger.info("Checking prerequisites for operation {}", opPath);
        return persister.doExecute(new OCMCallback<Boolean>() {
            @Override
            public Boolean doInOCM(ObjectContentManager ocm) throws RepositoryException {
                for (String dependency : dependsOn) {
                    NodeOperation dependentOp = (NodeOperation) ocm.getObjectByUuid(dependency);
                    if (dependentOp == null) {
                        logger.warn("No dependent operation ({}) found for node operation {}", dependency, op);
                        continue;
                    }
                    if (!dependentOp.isCompleted()) {
                        logger.info("Operation {} is still waiting for the dependent one {} to be completed", opPath,
                                dependentOp);
                        return Boolean.FALSE;
                    }
                }
                logger.info("Operation {} can be started", opPath);
                return Boolean.TRUE;
            }
        });
    }

    protected void completeGlobalOperationFor(NodeOperation op, ObjectContentManager ocm) {
        // update the state of the global operation
        Operation globalOp = op.getOperation();
        globalOp.setState(op.getState());
        globalOp.setInfo(op.getInfo());
        ocm.update(globalOp);

        // archive the global operation
        ocm.move(globalOp.getPath(), "/module-management/operationLog/" + globalOp.getName());
    }

    protected boolean performAction(NodeOperation op) {
        boolean success = true;
        long startTime = System.currentTimeMillis();
        logger.info("Start performing node operation {}", logger.isDebugEnabled() ? op : op.getPath());

        switch (op.getOperation().getAction()) {
            case "install":
                performActionInstall(op);
                break;

            default:
                logger.info("Unknown action {} for node operation {}. Refusing to execute it.", op.getPath());
                success = false;
                break;
        }
        logger.info("Done performing node operation {} with status {} in {} ms",
                new Object[] { logger.isDebugEnabled() ? op : op.getPath(), success ? "success" : "failure",
                        System.currentTimeMillis() - startTime });
        return success;
    }

    private void performActionInstall(NodeOperation op) {
        // TODO Auto-generated method stub
        
    }

    /**
     * Checks for the next open operation and processes it.
     * 
     * @throws ModuleManagementException
     *             in case of an error
     */
    public void process() throws ModuleManagementException {
        logger.debug("Checking for available node-level module operations");
        try {
            NodeOperation op = persister.getNextNodeOperation(clusterNodeInfo.getId());
            if (op == null) {
                // no operations to be processed found -> return
                logger.debug("No node-levelmodule operations to be processed found");
                return;
            }
            if ("open".equals(op.getState())) {
                // we can start the operation now
                logger.info("Found open node-level module operation to be started: {}",
                        logger.isDebugEnabled() ? op : op.getPath());
                long startTime = System.currentTimeMillis();
                if (canStart(op)) {
                    processOperation(op);
                    logger.info("Node-level module operation {} processed in {} ms", op.getPath(),
                            System.currentTimeMillis() - startTime);
                }
            }
        } catch (RepositoryException e) {
            throw new ModuleManagementException(e);
        }
    }

    /**
     * Starts the operation by changing its state and processing the required action.
     * 
     * @param op
     *            the operation to be started
     * @throws RepositoryException
     *             in case of errors
     */
    private void processOperation(final NodeOperation op) throws RepositoryException {
        persister.doExecute(new OCMCallback<Void>() {
            @Override
            public Void doInOCM(ObjectContentManager ocm) throws RepositoryException {
                // update operation state to "processing"
                op.setState("processing");
                ocm.update(op);
                ocm.save();

                try {
                    // execute the action and update operation state depending on the result
                    op.setState(performAction(op) ? "successful" : "failed");
                } catch (Exception e) {
                    // change the state of the operation to failed, providing the failure cause
                    op.setState("failed");
                    op.setInfo("Cause: " + ExceptionUtils.getMessage(e) + "\nRoot cause: "
                            + ExceptionUtils.getRootCauseMessage(e) + "\n" + ExceptionUtils.getFullStackTrace(e));

                    // re-throw the cause
                    if (e instanceof RepositoryException) {
                        throw (RepositoryException) e;
                    } else if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException(e);
                    }
                } finally {
                    ocm.update(op);

                    if (clusterNodeInfo.isProcessingServer() && !op.getOperation().isCompleted()) {
                        // we have to complete the global operation as this node is the last one in processing chain
                        completeGlobalOperationFor(op, ocm);
                    }
                    ocm.save();

                    // archive this node operation
                    ocm.move(op.getPath(), new StringBuilder(operationLogPath.length() + op.getName().length())
                            .append(operationLogPath).append(op.getName()).toString());
                    ocm.save();
                }

                return null;
            }

        });
    }

    public void setClusterNodeInfo(ClusterNodeInfo clusterNodeInfo) {
        this.clusterNodeInfo = clusterNodeInfo;
        operationLogPath = clusterNodeInfo != null
                ? "/module-management/nodes/" + clusterNodeInfo.getId() + "/operationLog/" : null;
    }

    /**
     * Injects an instance of the persistence service.
     * 
     * @param persister
     *            an instance of the persistence service
     */
    public void setPersister(ModuleInfoPersister persister) {
        this.persister = persister;
    }
}
