/**
 * 
 */
package org.jahia.modules.modulemanager.exception;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Exception raised when a deployment failed. 
 * Contains the underlying error and a HTTP status code to send to the client. 
 * 
 * @author bdjiba
 *
 */
@JsonIgnoreProperties(value={ "cause", "stackTrace", "localizedMessage", "suppressed" }, ignoreUnknown=true)
public class ModuleDeploymentException extends Exception {
  private static final long serialVersionUID = -1886713186574565575L;
  
  private final HttpStatus statusCode;
  private final Throwable reason;

  /**
   * 
   
  public ModuleDeploymentException() {
    // TODO Auto-generated constructor stub
  }*/

  /**
   * @param message
   */
  public ModuleDeploymentException(HttpStatus httpStatus, String msg, Throwable err) {
    super(msg);
    this.statusCode = httpStatus;
    reason = err;
  }



  /**
   * @param httpStatus
   * @param message
   */
  public ModuleDeploymentException(HttpStatus httpStatus, String msg) {
    this(httpStatus, msg, null);
  }

  /**
   * @return the statusCode
   */
  public HttpStatus getStatusCode() {
    return statusCode;
  }



  /**
   * @return the reason
   */
  public Throwable getReason() {
    return reason;
  }



  @Override
  public String toString() {
    return java.text.MessageFormat.format("ModuleDeploymentException'{'status:{0},message:''{1}'',reason:{2}'}'", statusCode, getMessage(), reason) ;
  }
}
