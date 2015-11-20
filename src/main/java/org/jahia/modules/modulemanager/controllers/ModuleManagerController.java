/**
 * 
 */
package org.jahia.modules.modulemanager.controllers;

import javax.annotation.PostConstruct;

import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;
import org.jahia.modules.modulemanager.payload.OperationResult;
import org.jahia.modules.modulemanager.payload.OperationResultImpl;
import org.jahia.modules.modulemanager.spi.ModuleManagerSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.AbstractApplicationEventMulticaster;
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

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#install(org.springframework.web.multipart.MultipartFile, java.lang.String[])
   */
  @Override
  public ResponseEntity<OperationResult> install(MultipartFile bundleFile, String[] targetNodes)
      throws ModuleDeploymentException {
    log.info("Install bundle " + bundleFile.getName());
    // TODO:
    OperationResult result = OperationResultImpl.SUCCESS;
    return new ResponseEntity<OperationResult>(result, HttpStatus.OK);
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#uninstall(java.lang.String, java.lang.String[])
   */
  @Override
  public ResponseEntity<OperationResult> uninstall(String bundleKey, String[] targetNodes)
      throws ModuleDeploymentException {
    log.info("Uninstall bundle " + bundleKey);
    // TODO: correct implementation
    
    OperationResult result = OperationResultImpl.SUCCESS;
    return new ResponseEntity<OperationResult>(result, HttpStatus.OK);
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#start(java.lang.String, java.lang.String[])
   */
  @Override
  public ResponseEntity<OperationResult> start(String bundleKey, String[] targetNodes)
      throws ModuleDeploymentException {
    log.info("Start bundle " + bundleKey);
    // TODO: correct implementation
    OperationResult result = OperationResultImpl.SUCCESS;
    return new ResponseEntity<OperationResult>(result, HttpStatus.OK);
  }

  /* (non-Javadoc)
   * @see org.jahia.modules.modulemanager.spi.ModuleManagerSpi#stop(java.lang.String, java.lang.String[])
   */
  @Override
  public ResponseEntity<OperationResult> stop(String bundleKey, String[] targetNodes)
      throws ModuleDeploymentException {
    log.info("Stop bundle " + bundleKey);
    // TODO: correct implementation
    
    OperationResult result = OperationResultImpl.SUCCESS;
    return new ResponseEntity<OperationResult>(result, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<OperationResult> check() throws ModuleDeploymentException {
    log.info("Test done !");
    OperationResult result = OperationResultImpl.SUCCESS;
    return new ResponseEntity<OperationResult>(result, HttpStatus.OK);
  }

}
