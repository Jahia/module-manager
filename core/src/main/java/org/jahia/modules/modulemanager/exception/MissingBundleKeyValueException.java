/**
 * 
 */
package org.jahia.modules.modulemanager.exception;

import javax.ws.rs.core.Response.Status;

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
    super(Status.BAD_REQUEST, msg, err);
  }

  /**
   * @param httpStatus
   * @param msg
   */
  public MissingBundleKeyValueException(String msg) {
    super(Status.BAD_REQUEST, msg);
  }

}
