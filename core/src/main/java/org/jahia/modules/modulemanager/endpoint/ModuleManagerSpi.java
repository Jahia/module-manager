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
package org.jahia.modules.modulemanager.endpoint;

import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;

/**
 * The Module manager Service Provider Interface.
 * Produces json string result.
 * 
 * @author bdjiba
 */
@Path("/api/bundles")
@Produces({"application/hal+json"})
public interface ModuleManagerSpi {
  
  /**
   * Install the given bundle on the specified nodes.
   * If nodes parameter is empty then deploy on all available nodes.
   * @param bundleFileInputStream the bundle to deploy file input stream
   * @param fileDisposition the file content disposition
   * @param fileBodyPart the file body part
   * @param nodes the cluster node identifiers the target nodes
   * @return the operation result
   * @throws ModuleDeploymentException when the operation fails
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("/_install{nodes : (/.*)?}")
  Response install(@FormDataParam("bundleFile") InputStream bundleFileInputStream, @FormDataParam("bundleFile") FormDataContentDisposition fileDisposition, @FormDataParam("bundleFile") FormDataBodyPart fileBodyPart, @BeanParam ClusterNodesMultiPartParam nodes) throws ModuleDeploymentException;
  
  /**
   * Uninstall the bundle on the target nodes or all the nodes if nodes param is missing.
   * @param bundleKey the target bundle
   * @param nodes the list of the nodes to uninstall the bundle
   * @return the operation result
   * @throws ModuleDeploymentException
   */
  @POST
  @Path("/{bundleKey}/_uninstall{nodes : (/.*)?}")
  Response uninstall(@PathParam(value="bundleKey") String bundleKey, @BeanParam ClusterNodesPostParam nodes) throws ModuleDeploymentException;
  
  /**
   * Starts the bundle which key is specified in the URL.
   * If the nodes part is missing, start it on all nodes.
   * @param bundleKey the bundle key
   * @param nodes the list of cluster node identifier of the target nodes
   * @return the operation status
   * @throws ModuleDeploymentException
   */
  @POST
  @Path("/{bundleKey}/_start{nodes : (/.*)?}")
  @Inject
  Response start(@PathParam(value="bundleKey") String bundleKey,  @BeanParam ClusterNodesPostParam nodes) throws ModuleDeploymentException;
  
  /**
   * Stops the bundle that key is given in parameters on the specified nodes.
   * If the node's ids are missing then will stop it on all existing nodes. 
   * @param bundleKey the bundle key
   * @param nodes the list of the target cluster nodes
   * @return the operation result or an error in case when the bundle is missing
   * @throws ModuleDeploymentException
   */
  @POST
  @Path("/{bundleKey}/_stop{nodes : (/.*)?}")
  Response stop(@PathParam(value="bundleKey") String bundleKey,  @BeanParam ClusterNodesPostParam nodes) throws ModuleDeploymentException;


  /**
   * Get the state report of a bundle in a list of target nodes.
   * @param bundleUniqueKey bundle key
   * @param nodes the list of the target cluster nodes
   * @return the state report of the bundle in the target nodes
   * @throws ModuleDeploymentException thrown exception
   */
  @GET
  @Path("/{bundleUniqueKey}/_state{nodes : (/.*)?}")
  @Produces(MediaType.APPLICATION_JSON)
  Response getBundleState(@PathParam("bundleUniqueKey") String bundleUniqueKey, @BeanParam ClusterNodesGetParam nodes) throws ModuleDeploymentException;

  /**
   * Get the state report of a list of nodes including their own bundles.
   * @param nodes the list of the target cluster nodes
   * @return the state report of the bundle in the target nodes
   * @throws ModuleDeploymentException thrown exception
   */
  @GET
  @Path("/_states{nodes : (/.*)?}")
  @Produces(MediaType.APPLICATION_JSON)
  Response getNodesBundleStates(@BeanParam ClusterNodesGetParam nodes) throws ModuleDeploymentException;

  /**
   * Get the state of a specific operation by its uuid
   * @param operationUuid the uuid of the operation
   * @return the operation infos
   * @throws ModuleDeploymentException thrown exception
   */
  @GET
  @Path("operation/{operationUuid}/_state")
  @Produces(MediaType.APPLICATION_JSON)
  Response getOperationState(@PathParam(value="operationUuid") String operationUuid)throws ModuleDeploymentException;
}
