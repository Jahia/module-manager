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
package org.jahia.modules.modulemanager;

import java.util.Set;

import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;
import org.jahia.modules.modulemanager.payload.BundleStateReport;
import org.jahia.modules.modulemanager.payload.NodeStateReport;
import org.springframework.core.io.Resource;

/**
 * Entry point interface for the module management service, providing functionality for module deployment, undeployment, start and stop
 * operations, which are performed in a seamless way on a standalone installation as well as across the platform cluster.
 * 
 * @author Sergiy Shyrkov
 */
public interface ModuleManager {

    /**
     * Install a bundle in a list of cluster nodes.
     * @param bundleResource the resource, representing a bundle to install
     * @param nodes set of cluster nodes id targeted by the install operation
     * @return the result of the install operation
     * @throws ModuleManagementException thrown exception is case of problems
     */
    OperationResult install(Resource bundleResource, String... nodes) throws ModuleManagementException;

    /**
     * Start a bundle on a list of cluster nodes.
     * @param bundleKey bundle key to start
     * @param nodes list of cluster nodes targeted by the start operation
     * @return the result of the start operation
     * @throws ModuleManagementException thrown exception is case of problems
     */
    OperationResult start(String bundleKey, String... nodes) throws ModuleManagementException;

    /**
     * Stop a bundle on a list of cluster nodes.
     * @param bundleKey bundle key to stop
     * @param nodes a set of cluster node id targeted by the stop operation
     * @return the result of the stop operation
     * @throws ModuleManagementException thrown exception is case of problems
     */
    OperationResult stop(String bundleKey, String... nodes) throws ModuleManagementException;

    /**
     * Uninstall a bundle on a list of cluster nodes.
     * @param bundleKey bundle key to uninstall
     * @param nodes set of cluster nodes id targeted by the uninstall operation
     * @return the result of the uninstall operation
     * @throws ModuleManagementException thrown exception is case of problems
     */
    OperationResult uninstall(String bundleKey, String... nodes) throws ModuleManagementException;

    /**
     * Get the state report of a bundle in a list of target nodes
     * @param bundleKey bundle key
     * @param targetNodes list of target nodes
     * @return  the state report of the bundle in the target nodes
     * @throws ModuleDeploymentException thrown exception
     */
    BundleStateReport getBundleState(String bundleKey, String... targetNodes) throws ModuleDeploymentException;

    /**
     * Get the state report of a list of Nodes including their own bundles
     * @param targetNodes list of target nodes
     * @return  the state report of the bundle in the target nodes
     * @throws ModuleDeploymentException thrown exception
     */
    Set<NodeStateReport> getNodesBundleStates(String... targetNodes)  throws ModuleDeploymentException;
}
