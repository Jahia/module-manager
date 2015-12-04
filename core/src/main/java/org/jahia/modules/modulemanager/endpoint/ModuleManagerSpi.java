package org.jahia.modules.modulemanager.endpoint;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
   * @param nodes the comma-separated string with list of the target nodes
   * @return the operation result
   * @throws ModuleDeploymentException when the operation fails
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("/_install")
  Response install(@FormDataParam("bundleFile") InputStream bundleFileInputStream, @FormDataParam("bundleFile") FormDataContentDisposition fileDisposition, @FormDataParam("bundleFile") FormDataBodyPart fileBodyPart, @FormDataParam("nodes") @DefaultValue("") String nodes) throws ModuleDeploymentException;
  
  /**
   * Uninstall the bundle on the target nodes or all the nodes if nodes param is missing.
   * @param bundleKey the target bundle
   * @param nodes the comma-separated string with list of the nodes to uninstall the bundle
   * @return the operation result
   * @throws ModuleDeploymentException
   */
  @POST
  @Path("/{bundleKey}/_uninstall")
  Response uninstall(@PathParam(value="bundleKey") String bundleKey, @FormParam("nodes") @DefaultValue("") String nodes) throws ModuleDeploymentException;
  
  /**
   * Starts the bundle which key is specified in the URL.
   * If the nodes part is missing, start it on all nodes.
   * @param bundleKey the bundle key
   * @param nodes the comma-separated string with list of the target nodes
   * @return the operation status
   * @throws ModuleDeploymentException
   */
  @POST
  @Path("/{bundleKey}/_start")
  Response start(@PathParam(value="bundleKey") String bundleKey,  @FormParam("nodes") @DefaultValue("") String nodes) throws ModuleDeploymentException;
  
  /**
   * Stops the bundle that key is given in parameters on the specified nodes.
   * If the node's ids are missing then will stop it on all existing nodes. 
   * @param bundleKey the bundle key
   * @param nodes the comma-separated string with list of the target nodes
   * @return the operation result or an error in case when the bundle is missing
   * @throws ModuleDeploymentException
   */
  @POST
  @Path("/{bundleKey}/_stop")
  Response stop(@PathParam(value="bundleKey") String bundleKey,  @FormParam("nodes") @DefaultValue("") String nodes) throws ModuleDeploymentException;


  /**
   * Get the state report of a bundle in a list of target nodes.
   * @param bundleUniqueKey bundle key
   * @param nodes the comma-separated string with list of the target nodes
   * @return  the state report of the bundle in the target nodes
   * @throws ModuleDeploymentException thrown exception
   */
  @GET
  @Path("/{bundleUniqueKey}/_state")
  @Produces(MediaType.APPLICATION_JSON)
  Response getBundleState(@PathParam("bundleUniqueKey") String bundleUniqueKey, @QueryParam("nodes") @DefaultValue("") String nodes) throws ModuleDeploymentException;

  /**
   * Get the state report of a list of nodes including their own bundles.
   * @param nodes the comma-separated string with list of the target nodes
   * @return  the state report of the bundle in the target nodes
   * @throws ModuleDeploymentException thrown exception
   */
  @GET
  @Path("/_states")
  @Produces(MediaType.APPLICATION_JSON)
  Response getNodesBundleStates(@QueryParam("nodes") @DefaultValue("") String nodes) throws ModuleDeploymentException;
}
