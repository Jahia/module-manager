/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.2 - Community Distribution                   =
 * ==========================================================================================
 *
 * Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 * streamlining Enterprise digital projects across channels to truly control
 * time-to-market and TCO, project after project.
 * Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 * marketing teams to collaboratively and iteratively build cutting-edge
 * online business solutions.
 * These, in turn, are securely and easily deployed as modules and apps,
 * reusable across any digital projects, thanks to the Jahia Private App Store Software.
 * Each solution provided by Jahia stems from this overarching vision:
 * Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 * Founded in 2002 and headquartered in Geneva, Switzerland,
 * Jahia Solutions Group has its North American headquarters in Washington DC,
 * with offices in Chicago, Toronto and throughout Europe.
 * Jahia counts hundreds of global brands and governmental organizations
 * among its loyal customers, in more than 20 countries across the globe.
 *
 * For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 * Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 * THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 * 1/GPL OR 2/JSEL
 *
 * 1/ GPL
 * ==========================================================
 *
 * IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 * "This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license"
 *
 * 2/ JSEL - Commercial and Supported Versions of the program
 * ==========================================================
 *
 * IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 * Alternatively, commercial and supported versions of the program - also known as
 * Enterprise Distributions - must be used in accordance with the terms and conditions
 * contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.modulemanager.persistence;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.jahia.modules.modulemanager.model.*;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jcr.RepositoryException;
import java.util.*;

/**
 * Responsible for managing the JCR structure and information about deployment of modules.
 *
 * @author Sergiy Shyrkov
 */
public class ModuleInfoPersister {

    /**
     * Callback interface for operations, that are executed in OCM.
     *
     * @param <T> the result return type of the operation
     * @author Sergiy Shyrkov
     */
    interface OCMCallback<T> {
        T doInOCM(ObjectContentManager ocm) throws RepositoryException;
    }

    private static final Logger logger = LoggerFactory.getLogger(ModuleInfoPersister.class);

    private static final String ROOT_NODE_PATH = "/module-management";

    private ClusterNodeInfo clusterNode;

    private AnnotationMapperImpl mapper;

    @Autowired
    private ModuleInfoInitializer moduleInfoInitializer;

    public void setModuleInfoInitializer(ModuleInfoInitializer moduleInfoInitializer) {
        this.moduleInfoInitializer = moduleInfoInitializer;
    }

    private <T> T doExecute(final OCMCallback<T> callback) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<T>() {
            @Override
            public T doInJCR(JCRSessionWrapper session) throws RepositoryException {
                ObjectContentManager ocm = new ObjectContentManagerImpl(session, getMapper());

                try {
                    return callback.doInOCM(ocm);
                } catch (RuntimeException e) {
                    throw new RepositoryException(e);
                }
            }
        });
    }

    private Mapper getMapper() {
        // TODO replace annotations with XML descriptor to eliminate dependency of object model to jackrabbit-ocm?
        if (mapper == null) {
            @SuppressWarnings("rawtypes")
            List<Class> classes = new LinkedList<Class>();
            classes.add(ModuleManagement.class);
            classes.add(Bundle.class);
            classes.add(BinaryFile.class);
            classes.add(Operation.class);
            classes.add(ClusterNode.class);
            classes.add(BundleReference.class);

            mapper = new AnnotationMapperImpl(classes);
        }
        return mapper;
    }

    public ModuleManagement getModuleManagement() throws RepositoryException {
        return doExecute(new OCMCallback<ModuleManagement>() {
            @Override
            public ModuleManagement doInOCM(ObjectContentManager ocm) throws RepositoryException {

                ModuleManagement mgt = (ModuleManagement) ocm.getObject(ModuleManagement.class, ROOT_NODE_PATH);

                return mgt;
            }
        });
    }

    private ModuleManagement getModuleManagement(ObjectContentManager ocm) {
        ModuleManagement mgt = (ModuleManagement) ocm.getObject(ModuleManagement.class, ROOT_NODE_PATH);
        if (mgt == null) {
            logger.info("Creating initial JCR structure skeletong for " + ROOT_NODE_PATH);
            try {
                ocm.insert(new ModuleManagement(ROOT_NODE_PATH));
                ocm.save();

                logger.info("Done creating initial JCR structure skeletong for " + ROOT_NODE_PATH);
            } catch (ObjectContentManagerException e) {
                // is already created
            }
            mgt = (ModuleManagement) ocm.getObject(ModuleManagement.class, ROOT_NODE_PATH);
        }

        return mgt;
    }

    public void setClusterNode(ClusterNodeInfo clusterNode) {
        this.clusterNode = clusterNode;
    }

    public void start() {
        validateJcrTreeStructure();
    }

    private void validateJcrTreeStructure() {
        try {
            doExecute(new OCMCallback<Object>() {
                @Override
                public Object doInOCM(ObjectContentManager ocm) throws RepositoryException {
                    // ensure module-management skeleton is created in JCR
                    ModuleManagement mgt = getModuleManagement(ocm);
                    TreeMap<String, Bundle> bundleWithStateMap = null;
                    if (mgt.getBundles().isEmpty()) {
                        logger.info("Start populating information about module bundles...");
                        long startTime = System.currentTimeMillis();
                        Map<String, String> bundeStateMap = new HashMap<String, String>();
                        moduleInfoInitializer.populateBundles(mgt);
                        bundleWithStateMap = mgt.getBundles();
                        ocm.update(mgt);

                        logger.info("Done populating information about module bundles in {} ms",
                                System.currentTimeMillis() - startTime);
                        mgt = getModuleManagement(ocm);
                    }

                    if (!mgt.getNodes().containsKey(clusterNode.getId())) {
                        ClusterNode cn = new ClusterNode(clusterNode.getId(), clusterNode.isProcessingServer());
                        cn.setPath(ROOT_NODE_PATH + "/nodes/" + clusterNode.getId());
                        cn.setState("online");
                        ocm.insert(cn);

                        logger.info("Start populating information about module bundles for node {}...",
                                clusterNode.getId());
                        long startTime = System.currentTimeMillis();

                        ClusterNode clusterNodeToUpdate = (ClusterNode) ocm.getObject(ClusterNode.class, cn.getPath());
                        mgt = getModuleManagement(ocm);
                        TreeMap<String, Bundle> mgtBundleMap = mgt.getBundles();
                        // TODO : update the status of the node bundles reference.
                        //synchronizeBundleMapWithState(mgtBundleMap, bundleWithStateMap);
                        moduleInfoInitializer.populateNodeBundles(
                                clusterNodeToUpdate, mgtBundleMap);
                        ocm.update(clusterNodeToUpdate);
                        ocm.save();

                        logger.info("Done populating information about module bundles for node {} in {} ms",
                                clusterNode.getId(), System.currentTimeMillis() - startTime);
                    }

                    return null;
                }
            });
        } catch (RepositoryException e) {
            logger.error("Unable to validate module management JCR tree structure. Cause: " + e.getMessage(), e);
        }
    }

    private void synchronizeBundleMapWithState(TreeMap<String, Bundle> mgtBundleMap, TreeMap<String, Bundle> bundleWithStateMap) {
        for (String bundleName : bundleWithStateMap.keySet()) {
            Bundle mgtBundle = mgtBundleMap.get(bundleName);
            Bundle bundleToSynchronize = bundleWithStateMap.get(bundleName);
            mgtBundle.setState(bundleToSynchronize.getState());
            mgtBundleMap.put(bundleName, mgtBundle);
        }
    }
}
