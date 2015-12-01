/**
 * 
 */
package org.jahia.modules.modulemanager.controllers;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * The Controller that handle that manage /bundles requests
 * @author bdjiba
 *
 */
//@Component
public class ModuleManagerController  {
  private static final Logger log = LoggerFactory.getLogger(ModuleManagerController.class);

  //@javax.annotation.Resource
  private ModuleManager moduleManager;
  
  //@PostConstruct
  public void init() {
    // FIXME: change to debug
    log.info("Module manager Endpoint is ready.");
  }
  
  /*public ResponseEntity<OperationResult> install(MultipartFile file, String[] nodes)
      throws ModuleDeploymentException {
    // TODO: language translation for messages
    if(file == null) {
      throw new ModuleDeploymentException(HttpStatus.NOT_ACCEPTABLE, "The bundle file could not be null");
    }
    if(!StringUtils.equalsIgnoreCase("application/java-archive", file.getContentType())) {
      throw new ModuleDeploymentException(HttpStatus.NOT_ACCEPTABLE, "Expected bundle file should be java archive");
    }
    log.debug("Install bundle " + file.getName() + " filename: " + file.getOriginalFilename() + " content-type: " + file.getContentType());
    try {
      Resource bundleResource = getUploadedFileAsResource(file);
      moduleManager.install(bundleResource, nodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return new ResponseEntity<OperationResult>(result, HttpStatus.OK);
    } catch (IOException ioe) {
      log.error("Error occured while installing a bundle file.", ioe);
      throw new ModuleDeploymentException(HttpStatus.INTERNAL_SERVER_ERROR, ioe.getMessage());
    }
  }*/

  private Resource getUploadedFileAsResource(MultipartFile uploaded) throws IOException {
    File bundleFile = new File(uploaded.getOriginalFilename());
    uploaded.transferTo(bundleFile);
    Resource bundleResource = new FileSystemResource(bundleFile);
    return bundleResource;
  }

  /*public ResponseEntity<OperationResult> uninstall(String bundleKey, String[] nodes)
      throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "uninstall");
    log.debug("Uninstall bundle " + bundleKey + " on nodes " + StringUtils.defaultIfBlank(StringUtils.join(nodes, ","), "all"));
    try{
      moduleManager.uninstall(bundleKey, nodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return new ResponseEntity<OperationResult>(result, HttpStatus.OK);      
    } catch(ModuleManagementException mmEx){
      log.error("Error while uninstalling module.", mmEx);
      throw new ModuleDeploymentException(HttpStatus.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  public ResponseEntity<OperationResult> start(String bundleKey, String[] nodes)
      throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "start");
    log.debug("Start bundle " + bundleKey + " on nodes " + StringUtils.defaultIfBlank(StringUtils.join(nodes, ","), "all"));
    try{
      moduleManager.start(bundleKey, nodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return new ResponseEntity<OperationResult>(result, HttpStatus.OK);      
    } catch(ModuleManagementException mmEx){
      log.error("Error while starting module.", mmEx);
      throw new ModuleDeploymentException(HttpStatus.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  public ResponseEntity<OperationResult> stop(String bundleKey, String[] nodes)
      throws ModuleDeploymentException {
    validateBundleOperation(bundleKey, "stop");
    log.debug("Stop bundle " + bundleKey + " on nodes " + StringUtils.defaultIfBlank(StringUtils.join(nodes, ","), "all"));
    try{
      moduleManager.stop(bundleKey, nodes);
      OperationResult result = OperationResultImpl.SUCCESS;
      return new ResponseEntity<OperationResult>(result, HttpStatus.OK);      
    } catch(ModuleManagementException mmEx){
      log.error("Error while stoping module.", mmEx);
      throw new ModuleDeploymentException(HttpStatus.INTERNAL_SERVER_ERROR, mmEx.getMessage());
    }
  }

  public ResponseEntity<OperationResult> check() throws ModuleDeploymentException {
    log.info("Test done !");
    OperationResult result = OperationResultImpl.SUCCESS;
    return new ResponseEntity<OperationResult>(result, HttpStatus.OK);
  }*/
  
  private void validateBundleOperation(String bundleKey, String operationName) throws MissingBundleKeyValueException {
    if(StringUtils.isBlank(bundleKey)) {
      throw new MissingBundleKeyValueException("Bundle key is mandatory for " + operationName + " action.");
    }
  }

}
