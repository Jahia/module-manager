/**
 * 
 */
package org.jahia.modules.modulemanager.exception;

import org.springframework.http.HttpStatus;

/**
 * @author bdjiba
 *
 */
public class MissingBundleKeyValueException extends ModuleDeploymentException {

  private static final long serialVersionUID = -6817346037208187306L;

  /**
   * @param httpStatus
   * @param msg
   * @param err
   */
  public MissingBundleKeyValueException(String msg, Throwable err) {
    super(HttpStatus.BAD_REQUEST, msg, err);
  }

  /**
   * @param httpStatus
   * @param msg
   */
  public MissingBundleKeyValueException(String msg) {
    super(HttpStatus.BAD_REQUEST, msg);
  }

}
