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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.jahia.modules.modulemanager.ModuleManagementException;
import org.jahia.modules.modulemanager.model.ClusterNode;
import org.jahia.modules.modulemanager.model.ModuleManagement;
import org.jahia.modules.modulemanager.model.NodeOperation;
import org.jahia.modules.modulemanager.model.Operation;
import org.jahia.modules.modulemanager.persistence.ModuleInfoPersister;
import org.jahia.modules.modulemanager.persistence.ModuleInfoPersister.OCMCallback;
import org.jahia.services.content.JCRContentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module global operation processor, that is responsible for controlling the operation lifecycle and creating corresponding cluster-node
 * level operations.
 * 
 * @author Sergiy Shyrkov
 */
public class OperationProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OperationProcessor.class);

    private ModuleInfoPersister persister;

    private void createNodeOperations(final Operation op, ObjectContentManager ocm)
            throws PathNotFoundException, RepositoryException {
        ModuleManagement mgt = (ModuleManagement) ocm.getObject(ModuleManagement.class, "/module-management");
        List<NodeOperation> dependsOn = new LinkedList<>();

        // first iterate over non-processing nodes
        for (ClusterNode cn : mgt.getNodes().values()) {
            if (cn.isProcessingServer()) {
                continue;
            }
            NodeOperation nodeOp = new NodeOperation();
            nodeOp.setName(JCRContentUtils.findAvailableNodeName(ocm.getSession().getNode(cn.getPath()), op.getName()));
            nodeOp.setOperation(op);
            // TODO verify the way we detect if the node is currently started or not
            if (cn.isStarted()) {
                dependsOn.add(nodeOp);
            }
            cn.getOperations().put(nodeOp.getName(), nodeOp);
        }
        ocm.update(mgt);

        // now the processing node
        for (ClusterNode cn : mgt.getNodes().values()) {
            if (!cn.isProcessingServer()) {
                continue;
            }
            NodeOperation nodeOp = new NodeOperation();
            nodeOp.setName(op.getName());
            nodeOp.setOperation(op);
            if (!dependsOn.isEmpty()) {
                // we have to wait for active non-processing nodes, so store the list of dependent operations
                List<String> uuids = new LinkedList<>();
                for (NodeOperation dependency : dependsOn) {
                    uuids.add(dependency.getIdentifier());
                }
                nodeOp.setDependsOn(uuids);
            }
            cn.getOperations().put(nodeOp.getName(), nodeOp);
        }
        ocm.update(mgt);

        ocm.save();
    }

    /**
     * Checks for the next open operation and starts it by changing its state and creating corresponding cluster node level operations.
     * 
     * @throws ModuleManagementException
     *             in case of an error
     */
    public void process() throws ModuleManagementException {
        logger.debug("Checking for available module operations");
        try {
            Operation op = persister.getNextOperation();
            if (op == null) {
                // no operations to be processed found -> return
                logger.debug("No module operations to be processed found");
                return;
            }
            if ("open".equals(op.getState())) {
                // we can start the operation now
                logger.info("Found open module operation to be started: {}", op);
                long startTime = System.currentTimeMillis();
                startOperation(op);
                logger.info("Module operation {} processed in {} ms", op.getName(),
                        System.currentTimeMillis() - startTime);
            }
        } catch (RepositoryException e) {
            throw new ModuleManagementException(e);
        }
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

    /**
     * Starts the operation by changing its state and creating corresponding cluster node level operations.
     * 
     * @param op
     *            the operation to be started
     * @throws RepositoryException
     *             in case of errors
     */
    private void startOperation(final Operation op) throws RepositoryException {
        persister.doExecute(new OCMCallback<Void>() {
            @Override
            public Void doInOCM(ObjectContentManager ocm) throws RepositoryException {
                // update operation state to "processing"
                op.setState("processing");
                ocm.update(op);
                ocm.save();

                try {
                    // delegate operation to cluster nodes
                    createNodeOperations(op, ocm);
                } catch (Exception e) {
                    // change the state of the operation to failed, providing the failure cause
                    op.setState("failed");
                    op.setInfo("Cause: " + ExceptionUtils.getMessage(e) + "\nRoot cause: "
                            + ExceptionUtils.getRootCauseMessage(e) + "\n" + ExceptionUtils.getFullStackTrace(e));
                    ocm.update(op);
                    ocm.save();

                    // archive the operation
                    ocm.move(op.getPath(), "/module-management/operationLog");
                    ocm.save();

                    // re-throw the cause
                    if (e instanceof RepositoryException) {
                        throw (RepositoryException) e;
                    } else if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException(e);
                    }
                }

                return null;
            }

        });
    }
}
