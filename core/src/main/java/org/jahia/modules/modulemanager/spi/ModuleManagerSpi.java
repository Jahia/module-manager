/**
 * 
 */
package org.jahia.modules.modulemanager.spi;

import java.io.InputStream;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The Module manager Service Provider Interface
 * Accepted headers: all or only application/json ??
 * Produce json string result
 * 
 * @author bdjiba
 *
 */
@Path("/bundles")
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
  Response install(@FormDataParam("bundleFile") InputStream bundleFileInputStream, @FormDataParam("bundleFile") FormDataContentDisposition fileDisposition, @FormDataParam("bundleFile") FormDataBodyPart fileBodyPart, @FormDataParam("nodes") String nodes) throws ModuleDeploymentException;
  
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
  
  @GET
  @Path("test")
  Response check() throws ModuleDeploymentException;
}
