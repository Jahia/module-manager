/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jahia.modules.modulemanager.ModuleManagementException;
import org.jahia.modules.modulemanager.ModuleManager;
import org.jahia.modules.modulemanager.exception.MissingBundleKeyValueException;
import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;
import org.jahia.modules.modulemanager.payload.OperationResult;
import org.jahia.modules.modulemanager.payload.OperationResultImpl;
import org.jahia.modules.modulemanager.spi.ModuleManagerSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author bdjiba
 *
 */
public class ModuleManagerResource implements ModuleManagerSpi{

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
  @Override
  public Response install(InputStream bundleFileInputStream, FormDataContentDisposition fileDisposition, FormDataBodyPart fileBodyPart, String[] nodes) throws ModuleDeploymentException {
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
    return Response.ok(result).build();
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#uninstall(java.lang.String, java.lang.String[])
   */
  @Override
  public Response uninstall(String bundleKey, String nodes) throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "uninstall");
    log.debug("Uninstall bundle " + bundleKey + " on nodes " + StringUtils.defaultString(nodes, "all"));
    try{
        getModuleManager().uninstall(bundleKey, null);
      OperationResult result = OperationResultImpl.SUCCESS;
      return Response.ok(result).build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while uninstalling module " + bundleKey, mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage(), mmEx);
    }
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#start(java.lang.String, java.lang.String[])
   */
  @Override
  public Response start(String bundleKey, String nodes) throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "start");
    log.debug("Start bundle " + bundleKey + " on nodes " + StringUtils.defaultIfBlank(nodes,"all"));
    try{
        getModuleManager().start(bundleKey, null);
      OperationResult result = OperationResultImpl.SUCCESS;
      return Response.ok(result).build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while starting bundle " + bundleKey, mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#stop(java.lang.String, java.lang.String[])
   */
  @Override
  public Response stop(String bundleKey, String nodes) throws ModuleDeploymentException {
    log.debug("Stop bundle " + bundleKey + " on nodes " + StringUtils.defaultIfBlank(nodes, "all"));
    try{
      getModuleManager().stop(bundleKey, null);
      OperationResult result = OperationResultImpl.SUCCESS;
      return Response.ok(result).build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while stoping module.", mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  @Override
  public Response getBundleState(String bundleUniqueKey, String nodes) throws ModuleDeploymentException {
    log.debug("Get bundle state " + bundleUniqueKey);
    return Response.ok(getModuleManager().getBundleState(bundleUniqueKey, null)).build();
  }

  @Override
  public Response getNodesBundleStates(String nodes) throws ModuleDeploymentException {
    log.debug("Get bundle states  for nodes " + nodes);
    return Response.ok(getModuleManager().getNodesBundleStates(null)).build();
  }

  /**
   * Spring bridge method to access to the module manager bean
   * @return
   */
  private ModuleManager getModuleManager() {
      if (moduleManager == null) {
          moduleManager = ModuleManagerApplicationContext.getBean("ModuleManager", ModuleManager.class);
      }
      return moduleManager;
  }

}
