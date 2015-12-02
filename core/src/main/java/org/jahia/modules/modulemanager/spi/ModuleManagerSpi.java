/**
 * 
 */
package org.jahia.modules.modulemanager.spi;

import java.io.InputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;

/**
 * The Module manager Service Provider Interface
 * Accepted headers: all or only application/json ??
 * Produce json string result
 * 
 * @author bdjiba
 *
 */
@Path("/api/bundles")
@Produces({"application/hal+json"})
public interface ModuleManagerSpi {
  
  /**
   * Install the given bundle on the specified nodes.
   * If nodes parameter is empty then deploy on all available nodes
   * @param bundleFile the bundle file to deploy
   * @param targetNodes the target node
   * @return the operation result
   * @throws ModuleDeploymentException
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("{v:(^$|_install)}")
  Response install(@FormDataParam("bundleFile") InputStream bundleFileInputStream, @FormDataParam("bundleFile") FormDataContentDisposition fileDisposition, @FormDataParam("bundleFile") FormDataBodyPart fileBodyPart, @FormDataParam("nodes") String[] nodes) throws ModuleDeploymentException;
  
  /**
   * Uninstall the bundle on the target nodes or all the nodes if nodes param is missing
   * @param bundleKey the target bundle
   * @param nodes the target nodes
   * @return the operation result
   * @throws ModuleDeploymentException
   */
  @POST
  @Path("{bundleKey}/_uninstall{nodes : (/nodes)?}")
  Response uninstall(@PathParam(value="bundleKey") String bundleKey, @PathParam(value = "nodes")String nodes) throws ModuleDeploymentException;
  
  /**
   * Starts the bundle which key is specified in the URL.
   * If the nodes part is missing, start it on all nodes
   * @param bundleKey the bundle key
   * @param nodes the target nodes
   * @return the operation status
   * @throws ModuleDeploymentException
   */
  @POST
  @Path("{bundleKey}/_start{nodes : (/nodes)?}")
  Response start(@PathParam(value="bundleKey") String bundleKey, @PathParam(value = "nodes") String nodes) throws ModuleDeploymentException;
  
  /**
   * Stops the bundle that key is given in parameters on the specified nodes.
   * If the node's ids are missing then will stop it on all existing nodes 
   * @param bundleKey the bundle key
   * @param nodes the target nodes
   * @return the operation result or an error in case when the bundle is missing
   * @throws ModuleDeploymentException
   */
  @POST
  @Path("{bundleKey}/_stop{nodes : (/nodes)?}")
  Response stop(@PathParam(value="bundleKey") String bundleKey, @PathParam(value = "nodes") String nodes) throws ModuleDeploymentException;


  /**
   * Get the state report of a bundle in a list of target nodes
   * @param bundleUniqueKey bundle key
   * @param nodes list of target nodes
   * @return  the state report of the bundle in the target nodes
   * @throws ModuleDeploymentException thrown exception
   */
  @GET
  @Path("{bundleUniqueKey}/_state{nodes : (/nodes)?}")
  @Produces(MediaType.APPLICATION_JSON)
  Response getBundleState(@PathParam("bundleUniqueKey") String bundleUniqueKey, @PathParam(value = "nodes") String nodes) throws ModuleDeploymentException;

  /**
   * Get the state report of a list of nodes including their own bundles
   * @param nodes list of target nodes
   * @return  the state report of the bundle in the target nodes
   * @throws ModuleDeploymentException thrown exception
   */
  @GET
  @Path("_states{nodes : (/nodes)?}")
  @Produces(MediaType.APPLICATION_JSON)
  Response getNodesBundleStates(@PathParam(value = "nodes") String nodes) throws ModuleDeploymentException;
}
