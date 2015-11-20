/**
 * 
 */
package org.jahia.modules.modulemanager.controllers;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.atmosphere.config.service.Post;
import org.jahia.modules.modulemanager.ModuleManagementException;
import org.jahia.modules.modulemanager.ModuleManager;
import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;
import org.jahia.modules.modulemanager.payload.OperationResult;
import org.jahia.modules.modulemanager.payload.OperationResultImpl;
import org.jahia.modules.modulemanager.spi.ModuleManagerSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * The Controller that handle that manage /bundles requests
 * @author bdjiba
 *
 */
@Component
public class ModuleManagerController implements ModuleManagerSpi {
  private static final Logger log = LoggerFactory.getLogger(ModuleManagerController.class);

  @javax.annotation.Resource
  private ModuleManager moduleManager;
  
  @PostConstruct
  public void init() {
    // FIXME: change to debug
    log.info("Module manager Endpoint is ready.");
  }
  
  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#install(org.springframework.web.multipart.MultipartFile, java.lang.String[])
   */
  @Override
  public ResponseEntity<OperationResult> install(MultipartFile uploaded, String[] targetNodes)
      throws ModuleDeploymentException {
    log.info("Install bundle " + uploaded.getName());
    try {
      Resource bundleResource = getUploadedFileAsResource(uploaded);
      moduleManager.install(bundleResource, targetNodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return new ResponseEntity<OperationResult>(result, HttpStatus.OK);
    } catch (IOException ioe) {
      log.error("Error occured while installing a bundle file.", ioe);
      throw new ModuleDeploymentException(HttpStatus.INTERNAL_SERVER_ERROR, ioe.getMessage());
    }
  }

  private Resource getUploadedFileAsResource(MultipartFile uploaded) throws IOException {
    File bundleFile = new File(uploaded.getOriginalFilename());
    uploaded.transferTo(bundleFile);
    Resource bundleResource = new FileSystemResource(bundleFile);
    return bundleResource;
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#uninstall(java.lang.String, java.lang.String[])
   */
  @Override
  public ResponseEntity<OperationResult> uninstall(String bundleKey, String[] targetNodes)
      throws ModuleDeploymentException {
    log.info("Uninstall bundle " + bundleKey);
    try{
      moduleManager.uninstall(bundleKey, targetNodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return new ResponseEntity<OperationResult>(result, HttpStatus.OK);      
    } catch(ModuleManagementException mmEx){
      log.error("Error while uninstalling module.", mmEx);
      throw new ModuleDeploymentException(HttpStatus.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#start(java.lang.String, java.lang.String[])
   */
  @Override
  public ResponseEntity<OperationResult> start(String bundleKey, String[] targetNodes)
      throws ModuleDeploymentException {
    log.info("Start bundle " + bundleKey);
    try{
      moduleManager.start(bundleKey, targetNodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return new ResponseEntity<OperationResult>(result, HttpStatus.OK);      
    } catch(ModuleManagementException mmEx){
      log.error("Error while starting module.", mmEx);
      throw new ModuleDeploymentException(HttpStatus.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#stop(java.lang.String, java.lang.String[])
   */
  @Override
  public ResponseEntity<OperationResult> stop(String bundleKey, String[] targetNodes)
      throws ModuleDeploymentException {
    log.info("Stop bundle " + bundleKey);
    try{
      moduleManager.stop(bundleKey, targetNodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return new ResponseEntity<OperationResult>(result, HttpStatus.OK);      
    } catch(ModuleManagementException mmEx){
      log.error("Error while stoping module.", mmEx);
      throw new ModuleDeploymentException(HttpStatus.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  @Override
  public ResponseEntity<OperationResult> check() throws ModuleDeploymentException {
    log.info("Test done !");
    OperationResult result = OperationResultImpl.SUCCESS;
    return new ResponseEntity<OperationResult>(result, HttpStatus.OK);
  }

}
