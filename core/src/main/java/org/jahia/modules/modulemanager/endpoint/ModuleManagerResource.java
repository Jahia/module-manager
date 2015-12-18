package org.jahia.modules.modulemanager.endpoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
import org.jahia.modules.modulemanager.ModuleManagerHelper;
import org.jahia.modules.modulemanager.OperationResult;
import org.jahia.modules.modulemanager.exception.MissingBundleKeyValueException;
import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.utils.i18n.ModuleMessageSource;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.DefaultMessageContext;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.MessageSource;
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
  //private TemplatePackageRegistry templatePackageRegistry;
  private JahiaTemplateManagerService templateManagerService;
  private MessageSource messageSource;
  
  private Resource getUploadedFileAsResource(InputStream uploadedFileIs, String filename) throws ModuleDeploymentException {
    // create internal temp file
    FileOutputStream fileOutputStream = null;
    File tempFile = null;
    try {
        tempFile = File.createTempFile(FilenameUtils.getBaseName(filename) + "-", "." + FilenameUtils.getExtension(filename), FileUtils.getTempDirectory());
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
  public Response install(InputStream bundleFileInputStream, FormDataContentDisposition fileDisposition, FormDataBodyPart fileBodyPart, ClusterNodesMultiPartParam nodes) throws ModuleDeploymentException {
    if(bundleFileInputStream == null || fileDisposition == null || StringUtils.isEmpty(fileDisposition.getFileName())) {
      throw new ModuleDeploymentException(Response.Status.BAD_REQUEST, "The bundle file could not be null");
    }
    
    Resource bundleResource = null;
    if(log.isDebugEnabled()) {
      log.debug("Installing bundle {} on nodes {}", fileDisposition.getFileName(), nodes);
    }
    try{
      bundleResource = getUploadedFileAsResource(bundleFileInputStream, fileDisposition.getFileName());
      MessageContext context = new DefaultMessageContext(getMessageSource());
      
      try {
        // FIXME: from rest, always force the update then we will not check force update
        OperationResult result = ModuleManagerHelper.installBundles(getModuleManager(), bundleResource.getFile(), context, fileDisposition.getFileName(), true, getJahiaTemplateManagerService(), getJahiaTemplateManagerService().getTemplatePackageRegistry());
        
        return Response.ok(result).build();
    } catch (IOException ex) {
      log.error("IOE error when installing module.", ex);
      throw new ModuleDeploymentException(Response.Status.EXPECTATION_FAILED, ex.getMessage(), ex);
    } catch (BundleException bex) {
      log.error("Bundle Exception occured during module installation.", bex.getMessage(), bex);
      throw new ModuleDeploymentException(Response.Status.EXPECTATION_FAILED, bex.getMessage(), bex);
    }
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
  public Response uninstall(String bundleKey, ClusterNodesParam nodes) throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "uninstall");
    if(log.isDebugEnabled()) {
      log.debug("Uninstall bundle {}  on nodes {}", bundleKey, nodes);
    }
    try{
      OperationResult result = getModuleManager().uninstall(bundleKey, nodes.getNodeIds());
      return Response.ok(result).build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while uninstalling module " + bundleKey, mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage(), mmEx);
    }
  }

  @Override
  public Response start(String bundleKey, ClusterNodesParam nodes) throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "start");
    if(log.isDebugEnabled()) {
      log.debug("Start bundle {} on nodes {}", bundleKey, nodes);
    }
    
    try{
      OperationResult result = getModuleManager().start(bundleKey, nodes.getNodeIds());
      return Response.ok(result).build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while starting bundle " + bundleKey, mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  @Override
  public Response stop(String bundleKey, ClusterNodesParam nodes) throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "stop");
    if(log.isDebugEnabled()) {
      log.debug("Stoping bundle {} on nodes {}", bundleKey, nodes);
    }
    try{
      OperationResult result = getModuleManager().stop(bundleKey, nodes.getNodeIds());
      return Response.ok(result).build();      
    } catch(ModuleManagementException mmEx){
      log.error("Error while stoping module.", mmEx);
      throw new ModuleDeploymentException(Status.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  @Override
  public Response getBundleState(String bundleUniqueKey, ClusterNodesParam nodes) throws ModuleDeploymentException {
    if(log.isDebugEnabled()) {
      log.debug("Get bundle state {}", bundleUniqueKey);
    }
    return Response.ok(getModuleManager().getBundleState(bundleUniqueKey, nodes.getNodeIds())).build();
  }

  @Override
  public Response getNodesBundleStates(ClusterNodesParam nodes) throws ModuleDeploymentException {
    if(log.isDebugEnabled()) {
      log.debug("Get bundle states for nodes {}", nodes);
    }
    return Response.ok(getModuleManager().getNodesBundleStates(nodes.getNodeIds())).build();
  }

  @Override
  public Response getOperationState(String operationUuid) throws ModuleDeploymentException {
    if(log.isDebugEnabled()) {
      log.debug("Get operation state for Operation", operationUuid);
    }
    return Response.ok(getModuleManager().getOperationState(operationUuid)).build();
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
  
  private JahiaTemplateManagerService getJahiaTemplateManagerService(){
    if(templateManagerService == null) {
      templateManagerService = ModuleManagerApplicationContext.getBean("JahiaTemplateManagerService", JahiaTemplateManagerService.class);
    }
    return templateManagerService;
  }
  
  private MessageSource getMessageSource() {
    if(messageSource == null) {
      messageSource = ModuleManagerApplicationContext.getBean("messageSource", ModuleMessageSource.class);
    }
    return messageSource;
  }

}
