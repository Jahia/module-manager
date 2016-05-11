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
package org.jahia.modules.modulemanager.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.modules.modulemanager.rest.exception.MissingBundleKeyValueException;
import org.jahia.modules.modulemanager.rest.exception.ModuleDeploymentException;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.modulemanager.ModuleManagementException;
import org.jahia.services.modulemanager.ModuleManager;
import org.jahia.services.modulemanager.OperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * The REST service implementation for module manager API.
 * 
 * @author bdjiba
 */
@Path("/api/bundles")
@Produces({MediaType.APPLICATION_JSON})
public class ModuleManagerService {
  
  private static final Logger log = LoggerFactory.getLogger(ModuleManagerService.class);
  
  private ModuleManager moduleManager;
  
  private Resource getUploadedFileAsResource(InputStream inputStream, String filename) throws ModuleDeploymentException {
    File tempFile = null;
    try {
        tempFile = File.createTempFile(FilenameUtils.getBaseName(filename) + "-", "." + FilenameUtils.getExtension(filename), FileUtils.getTempDirectory());
        FileUtils.copyInputStreamToFile(inputStream, tempFile);
    } catch (IOException ioex) {
        log.error("Error copy uploaded stream to local temp file for " + filename, ioex);
        throw new ModuleDeploymentException(Response.Status.INTERNAL_SERVER_ERROR, "Error while deploying bundle " + filename, ioex);
    } finally {
        IOUtils.closeQuietly(inputStream);
    }

    return new FileSystemResource(tempFile);
  }
  
  private void validateBundleOperation(String bundleKey, String serviceOperation) throws MissingBundleKeyValueException {
    if(StringUtils.isBlank(bundleKey)) {
      throw new MissingBundleKeyValueException("Bundle key is mandatory for " + serviceOperation + " operation.");
    }
  }

  @GET
  @Path("/ping")
  public String getInfo() {
      return "{\"pong\":\"ok\"}";
  }
  
    /**
     * Install the given bundle on the specified group of nodes. If nodes parameter is empty then deploy to default group.
     * 
     * @param bundleInputStream
     *            the bundle to deploy file input stream
     * @param target
     *            the group of cluster nodes targeted by the install operation
     * @param start
     *            <code>true</code> if the installed bundle should be started right away; <code>false</code> to keep it in the installed
     *            stated
     * @return the operation result
     * @throws ModuleDeploymentException
     *             when the operation fails
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/")
    public Response install(@FormDataParam("bundle") InputStream bundleInputStream,
            @FormDataParam("bundle") FormDataContentDisposition fileDisposition, @FormDataParam("target") String target,
            @FormDataParam("start") boolean start) throws ModuleDeploymentException {
        long startTime = System.currentTimeMillis();
        if (bundleInputStream == null) {
            throw new ModuleDeploymentException(Response.Status.BAD_REQUEST, "The bundle file could not be null");
        }

        log.info("Received request to install bundle {} on target {}. Should start the bundle after: {}",
                new Object[] { fileDisposition.getFileName(), target, start });

        Resource bundleResource = null;

        try {
            bundleResource = getUploadedFileAsResource(bundleInputStream, fileDisposition.getFileName());

            OperationResult result = getModuleManager().install(bundleResource, target, start);

            return Response.ok(result).build();
        } catch (ModuleManagementException ex) {
            log.error("Module management exception when installing module.", ex);
            throw new ModuleDeploymentException(Response.Status.EXPECTATION_FAILED, ex.getMessage(), ex);
        } catch (Exception bex) {
            log.error("An Exception occured during module installation.", bex.getMessage(), bex);
            throw new ModuleDeploymentException(Response.Status.EXPECTATION_FAILED, bex.getMessage(), bex);
        } finally {
            log.info("Operation completed in {} ms", System.currentTimeMillis() - startTime);
            if (bundleResource != null) {
                try {
                    File bundleFile = bundleResource.getFile();
                    FileUtils.deleteQuietly(bundleFile);
                } catch (IOException ioex) {
                    log.debug("Unable to clean installed bundle file", ioex);
                }
            }
        }
    }

//  @Override
//  public Response uninstall(String bundleKey, ClusterNodesPostParam nodes) throws ModuleDeploymentException {
//    validateBundleOperation(bundleKey, "uninstall");
//    if(log.isDebugEnabled()) {
//      log.debug("Uninstall bundle {}  on nodes {}", bundleKey, nodes);
//    }
//    try{
//      OperationResult result = getModuleManager().uninstall(bundleKey, nodes.getNodeIds().toString());
//      return Response.ok(result).build();      
//    } catch(ModuleManagementException mmEx){
//      log.error("Error while uninstalling module " + bundleKey, mmEx);
//      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage(), mmEx);
//    }
//  }
//
    /**
     * Starts the specified bundle.
     * 
     * @param bundleKey
     *            the bundle key
     * @param target
     *            the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws ModuleDeploymentException
     *             in case of an error during start operation
     */
    @POST
    @Path("/{bundleKey:.*}/_start")
    public Response start(@PathParam(value = "bundleKey") String bundleKey, @FormParam("target") String target)
            throws ModuleDeploymentException {
        validateBundleOperation(bundleKey, "start");
        log.info("Received request to start bundle {} on target {}", new Object[] { bundleKey, target });

        try {
            OperationResult result = getModuleManager().start(bundleKey, target);
            return Response.ok(result).build();
        } catch (ModuleManagementException mmEx) {
            log.error("Error while starting bundle " + bundleKey, mmEx);
            throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
        }
    }

    /**
     * Stops the specified bundle.
     * 
     * @param bundleKey
     *            the bundle key
     * @param target
     *            the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws ModuleDeploymentException
     *             in case of an error during stop operation
     */
    @POST
    @Path("/{bundleKey:.*}/_stop")
    public Response stop(@PathParam(value = "bundleKey") String bundleKey, @FormParam("target") String target)
            throws ModuleDeploymentException {
        validateBundleOperation(bundleKey, "stop");
        log.info("Received request to stop bundle {} on target {}", new Object[] { bundleKey, target });

        try {
            OperationResult result = getModuleManager().stop(bundleKey, target);
            return Response.ok(result).build();
        } catch (ModuleManagementException mmEx) {
            log.error("Error while stopping bundle " + bundleKey, mmEx);
            throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
        }
    }


    /**
     * Uninstalls the specified bundle.
     * 
     * @param bundleKey
     *            the bundle key
     * @param target
     *            the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws ModuleDeploymentException
     *             in case of an error during uninstall operation
     */
    @POST
    @Path("/{bundleKey:.*}/_uninstall")
    public Response uninstall(@PathParam(value = "bundleKey") String bundleKey, @FormParam("target") String target)
            throws ModuleDeploymentException {
        validateBundleOperation(bundleKey, "stop");
        log.info("Received request to uninstall bundle {} on target {}", new Object[] { bundleKey, target });

        try {
            OperationResult result = getModuleManager().uninstall(bundleKey, target);
            return Response.ok(result).build();
        } catch (ModuleManagementException mmEx) {
            log.error("Error while uninstalling bundle " + bundleKey, mmEx);
            throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
        }
    }
//    
//  @Override
//  public Response stop(String bundleKey, ClusterNodesPostParam nodes) throws ModuleDeploymentException {
//    validateBundleOperation(bundleKey, "stop");
//    if(log.isDebugEnabled()) {
//      log.debug("Stoping bundle {} on nodes {}", bundleKey, nodes);
//    }
//    try{
//      OperationResult result = getModuleManager().stop(bundleKey, nodes.getNodeIds().toString());
//      return Response.ok(result).build();      
//    } catch(ModuleManagementException mmEx){
//      log.error("Error while stoping module.", mmEx);
//      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
//    }
//  }
//
//  @Override
//  public Response getBundleState(String bundleUniqueKey, ClusterNodesGetParam nodes) throws ModuleDeploymentException {
//    if(log.isDebugEnabled()) {
//      log.debug("Get bundle state {}", bundleUniqueKey);
//    }
////    return Response.ok(getModuleManager().getBundleState(bundleUniqueKey, nodes.getNodeIds())).build();
//    return null;
//  }
//
//  @Override
//  public Response getNodesBundleStates(ClusterNodesGetParam nodes) throws ModuleDeploymentException {
//    if(log.isDebugEnabled()) {
//      log.debug("Get bundle states for nodes {}", nodes);
//    }
////    return Response.ok(getModuleManager().getNodesBundleStates(nodes.getNodeIds())).build();
//    return null;
//  }

  /**
   * Spring bridge method to access to the module manager bean.
   * 
   * @return an instance of the module manager service
   */
  private ModuleManager getModuleManager() {
      if (moduleManager == null) {
          moduleManager = (ModuleManager) SpringContextSingleton.getBean("ModuleManager");
      }
      return moduleManager;
  }

}
