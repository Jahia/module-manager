package org.jahia.modules.modulemanager.endpoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jahia.modules.modulemanager.ModuleManagementException;
import org.jahia.modules.modulemanager.ModuleManager;
import org.jahia.modules.modulemanager.OperationResult;
import org.jahia.modules.modulemanager.exception.MissingBundleKeyValueException;
import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * The REST service implementation for module manager API.
 * 
 * @author bdjiba
 */
public class ModuleManagerResource implements ModuleManagerSpi {

    private static final Logger log = LoggerFactory.getLogger(ModuleManagerResource.class);
  
    private ModuleManager moduleManager;
  
  private Resource getUploadedFileAsResource(InputStream uploadedFileIs, String filename) throws ModuleDeploymentException {
    // create internal temp file
    FileOutputStream fileOutputStream = null;
    File tempFile = null;
    try {
        tempFile = File.createTempFile(FilenameUtils.getBaseName(filename), FilenameUtils.getExtension(filename), FileUtils.getTempDirectory());
        fileOutputStream = new FileOutputStream(tempFile);
        IOUtils.copy(uploadedFileIs, fileOutputStream);
    } catch (IOException ioex) {
        log.error("Error copy uploaded stream to local temp file for " + filename, ioex);
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

  @Override
  public Response install(InputStream bundleFileInputStream, FormDataContentDisposition fileDisposition, FormDataBodyPart fileBodyPart, Set<String> nodeSet) throws ModuleDeploymentException {
    if(bundleFileInputStream == null || fileDisposition == null || StringUtils.isEmpty(fileDisposition.getFileName())) {
      throw new ModuleDeploymentException(Response.Status.BAD_REQUEST, "The bundle file could not be null");
    }
    
    // INFO: fileBodyPart.getMediaType().getType() is always null use toSring() !!!
    if(!StringUtils.equalsIgnoreCase("application/java-archive", fileBodyPart.getMediaType().toString())) {
      throw new ModuleDeploymentException(Response.Status.BAD_REQUEST, "Expected bundle file should be java archive. Current is " + fileBodyPart.getMediaType().getType());
    }
    Resource bundleResource = null;
    try{
      log.debug("Installing bundle {} on nodes {}", new Object[] {fileDisposition.getFileName(), nodeSet});
      bundleResource = getUploadedFileAsResource(bundleFileInputStream, fileDisposition.getFileName());
      OperationResult result = getModuleManager().install(bundleResource, nodeSet);
      return Response.ok(result).build();
    }finally {
      if(bundleResource != null) {
        try{
          File bundleFile = bundleResource.getFile();
          FileUtils.deleteQuietly(bundleFile);
        } catch(IOException ioex){
          log.trace("Unable to clean installed bundle file", ioex);
        }
      }
    }
  }

  @Override
  public Response uninstall(String bundleKey, Set<String> nodeSet) throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "uninstall");
    log.debug("Uninstall bundle {}  on nodes {}", new Object[] {bundleKey, nodeSet});
    if(log.isDebugEnabled()) {
    }
    try{
      OperationResult result = getModuleManager().uninstall(bundleKey, nodeSet);
      return Response.ok(result).build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while uninstalling module " + bundleKey, mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage(), mmEx);
    }
  }

  @Override
  public Response start(String bundleKey, Set<String> nodeSet) throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "start");
    log.debug("Start bundle {} on nodes {}", new Object[] {bundleKey, nodeSet});
    try{
      OperationResult result = getModuleManager().start(bundleKey, nodeSet);
      return Response.ok(result).build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while starting bundle " + bundleKey, mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  @Override
  public Response stop(String bundleKey, Set<String> nodeSet) throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "stop");
    
    log.debug("Stoping bundle {} on nodes {}", new Object[] {bundleKey, nodeSet});
    try{
      OperationResult result = getModuleManager().stop(bundleKey, nodeSet);
      return Response.ok(result).build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while stoping module.", mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  @Override
  public Response getBundleState(String bundleUniqueKey, Set<String> nodeSet) throws ModuleDeploymentException {
    log.debug("Get bundle state {}",  bundleUniqueKey);
    return Response.ok(getModuleManager().getBundleState(bundleUniqueKey, null)).build();
  }

  @Override
  public Response getNodesBundleStates(Set<String> nodeSet) throws ModuleDeploymentException {
    log.debug("Get bundle states for nodes {}", nodeSet);
    return Response.ok(getModuleManager().getNodesBundleStates(nodeSet)).build();
  }

  /**
   * Spring bridge method to access to the module manager bean.
   * @return an instance of the module manager service
   */
  private ModuleManager getModuleManager() {
      if (moduleManager == null) {
          moduleManager = ModuleManagerApplicationContext.getBean("ModuleManager", ModuleManager.class);
      }
      return moduleManager;
  }

}
