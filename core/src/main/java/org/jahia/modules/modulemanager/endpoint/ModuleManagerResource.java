/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.modules.modulemanager.ModuleManagementException;
import org.jahia.modules.modulemanager.ModuleManager;
import org.jahia.modules.modulemanager.exception.MissingBundleKeyValueException;
import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;
import org.jahia.modules.modulemanager.payload.OperationResult;
import org.jahia.modules.modulemanager.payload.OperationResultImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * @author bdjiba
 *
 */
@Component
@Path("/bundles")
@Produces({"application/json"})
public class ModuleManagerResource {

    private static final Logger log = LoggerFactory.getLogger(ModuleManagerResource.class);
  
    private ModuleManager moduleManager;
  
  /// internal
  private Resource getUploadedFileAsResource(InputStream uploadedFileIs, String filename) throws ModuleDeploymentException {
    // create internal temp file
    File tempFile = new File(FileUtils.getTempDirectory(), filename);
    while (tempFile.exists()) {
        tempFile = new File(FileUtils.getTempDirectory(), tempFile.getName() + "-1");
    }
    FileOutputStream fileOutputStream = null;
    try {
        fileOutputStream = new FileOutputStream(tempFile);
        IOUtils.copy(uploadedFileIs, fileOutputStream);
    } catch (IOException ioex) {
        log.error("Error copying stream to file " + tempFile, ioex);
        throw new ModuleDeploymentException(Response.Status.INTERNAL_SERVER_ERROR, "Error while deploying bundle " + filename, ioex);
    } finally {
        IOUtils.closeQuietly(fileOutputStream);
        IOUtils.closeQuietly(uploadedFileIs);
    }
    // set the file as resource to forward
    Resource bundleResource = new FileSystemResource(tempFile);
    return bundleResource;
  }
  
  private void validateBundleOperation(String bundleKey, String serviceOperation) throws MissingBundleKeyValueException {
    if(StringUtils.isBlank(bundleKey)) {
      throw new MissingBundleKeyValueException("Bundle key is mandatory for " + serviceOperation + " operation.");
    }
  }

  ///
  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#install(java.io.InputStream, org.glassfish.jersey.media.multipart.FormDataContentDisposition, org.glassfish.jersey.media.multipart.FormDataBodyPart, java.lang.String[])
   */
  //@Override
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("{v:(^$|_install)}")
  public Response install(@FormDataParam("bundleFile") InputStream bundleFileInputStream, @FormDataParam("bundleFile") FormDataContentDisposition fileDisposition, @FormDataParam("bundleFile") FormDataBodyPart fileBodyPart, @FormDataParam("nodes") String[] nodes) throws ModuleDeploymentException {
    if(bundleFileInputStream == null || fileDisposition == null || StringUtils.isEmpty(fileDisposition.getFileName())) {
      throw new ModuleDeploymentException(Response.Status.BAD_REQUEST, "The bundle file could not be null");
    }
    
    
    log.info("[AFAC] - media type " + fileBodyPart.getMediaType().getType());
    // FIXME: fileBodyPart.getMediaType().getType() is always null !!!
    /*if(!StringUtils.equalsIgnoreCase("application/java-archive", fileBodyPart.getMediaType().getType())) {
      throw new ModuleDeploymentException(Response.Status.BAD_REQUEST, "Expected bundle file should be java archive. Current is " + fileBodyPart.getMediaType().getType());
    }*/
    
    log.debug("Install bundle " + fileDisposition.getName() + " filename: " + fileDisposition.getFileName() + " content-type: " + fileBodyPart.getMediaType().getType());
    Resource bundleResource = getUploadedFileAsResource(bundleFileInputStream, fileDisposition.getFileName());
    getModuleManager().install(bundleResource, nodes);
    OperationResult result = OperationResultImpl.SUCCESS;
    return Response.ok().build();
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#uninstall(java.lang.String, java.lang.String[])
   */
  //@Override
  @POST
  @Path("{bundleKey}/_uninstall{nodes : (/nodes)?}")
  public Response uninstall(@PathParam(value="bundleKey") String bundleKey/*, @PathParam(value = "nodes")String[] nodes*/) throws ModuleDeploymentException {
    String [] nodes = null;
    validateBundleOperation(bundleKey, "uninstall");
    log.debug("Uninstall bundle " + bundleKey + " on nodes " + StringUtils.defaultIfBlank(StringUtils.join(nodes, ","), "all"));
    try{
        getModuleManager().uninstall(bundleKey, nodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return Response.ok().build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while uninstalling module " + bundleKey, mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage(), mmEx);
    }
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#start(java.lang.String, java.lang.String[])
   */
  //@Override
  @POST
  @Path("{bundleKey}/_start{nodes : (/nodes)?}")
  public Response start(@PathParam(value="bundleKey") String bundleKey /*, @PathParam(value = "nodes")String[] nodes*/) throws ModuleDeploymentException {
    String [] nodes = null;
    validateBundleOperation(bundleKey, "start");
    log.debug("Start bundle " + bundleKey + " on nodes " + StringUtils.defaultIfBlank(StringUtils.join(nodes, ","), "all"));
    try{
        getModuleManager().start(bundleKey, nodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return Response.ok().build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while starting bundle " + bundleKey, mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#stop(java.lang.String, java.lang.String[])
   */
  //@Override
  @POST
  @Path("{bundleKey}/_stop{nodes : (/nodes)?}")
  public Response stop(@PathParam(value="bundleKey") String bundleKey/*, @PathParam(value = "nodes")String[] nodes*/) throws ModuleDeploymentException {
    String [] nodes = null;
    log.debug("Stop bundle " + bundleKey + " on nodes " + StringUtils.defaultIfBlank(StringUtils.join(nodes, ","), "all"));
    try{
      getModuleManager().stop(bundleKey, nodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return Response.ok().build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while stoping module.", mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#check()
   */
  //@Override
  @GET
  @Path("test")
  public Response check() throws ModuleDeploymentException {
    log.info("[AFAC] Test done !");
    return Response.ok().build();
  }

    public ModuleManager getModuleManager() {
        if (moduleManager == null) {
            moduleManager = ModuleManagerApplicationContext.getBean("ModuleManager", ModuleManager.class);
        }
        return moduleManager;
    }
}
