/**
 * 
 */
package org.jahia.modules.modulemanager.spi;

import org.jahia.modules.modulemanager.exception.ModuleDeploymentException;
import org.jahia.modules.modulemanager.payload.OperationResult;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * The Module manager Service Provider Interface
 * Accepted headers: all or only application/json ??
 * Produce json string result
 * 
 * @author bdjiba
 *
 */
@Controller
@RequestMapping(/*headers="content-type=application/*", */produces="application/json", value={"/bundles"})
public interface ModuleManagerSpi {
  
  /**
   * Install the given bundle on the specified nodes.
   * If nodes parameter is empty then deploy on all available nodes
   * @param bundleFile the bundle file to deploy
   * @param targetNodes the target node
   * @return the operation result
   * @throws ModuleDeploymentException
   */
  @RequestMapping(method=RequestMethod.POST, params={"bundleFile", "nodes"})
  @ResponseBody
  ResponseEntity<OperationResult> install(@RequestParam(value = "bundleFile", required = true) MultipartFile bundleFile, @RequestParam(value="nodes", required = false) String[] targetNodes) throws ModuleDeploymentException;
  
  /**
   * Uninstall the bundle on the target nodes or all the nodes if nodes param is missing
   * @param bundleKey the target bundle
   * @param targetNodes the target nodes
   * @return the operation result
   * @throws ModuleDeploymentException
   */
  @RequestMapping(method=RequestMethod.POST, value="/{bundleKey}/uninstall/{nodes}")
  @ResponseBody
  ResponseEntity<OperationResult> uninstall(@PathVariable("bundleKey")String bundleKey, @PathVariable("nodes")String[] targetNodes) throws ModuleDeploymentException;
  
  /**
   * Starts the bundle which key is specified in the URL.
   * If the nodes part is missing, start it on all nodes
   * @param bundleKey the bundle key
   * @param targetNodes the target nodes
   * @return the operation status
   * @throws ModuleDeploymentException
   */
  @RequestMapping(method=RequestMethod.POST, value="/{bundleKey}/start/{nodes}")
  @ResponseBody
  ResponseEntity<OperationResult> start(@PathVariable("bundleKey")String bundleKey, @PathVariable("nodes") String[] targetNodes) throws ModuleDeploymentException;
  
  /**
   * Stops the bundle that key is given in parameters on the specified nodes.
   * If the node's ids are missing then will stop it on all existing nodes 
   * @param bundleKey the bundle key
   * @param targetNodes the target nodes
   * @return the operation result or an error in case when the bundle is missing
   * @throws ModuleDeploymentException
   */
  @RequestMapping(method=RequestMethod.POST, value="/{bundleKey}/stop/{nodes}")
  @ResponseBody
  ResponseEntity<OperationResult> stop(@PathVariable("bundleKey") String bundleKey, @PathVariable("nodes") String[] targetNodes) throws ModuleDeploymentException;
  
  @RequestMapping(method=RequestMethod.GET, value="/test")
  @ResponseBody
  ResponseEntity<OperationResult> check() throws ModuleDeploymentException;
}
