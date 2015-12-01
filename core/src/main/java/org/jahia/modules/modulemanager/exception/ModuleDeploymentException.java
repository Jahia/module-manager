/**
 * 
 */
package org.jahia.modules.modulemanager.exception;

import javax.ws.rs.core.Response;

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
  
  private final Throwable reason;
  private final Response.Status responseStatus;

  /**
   * 
   
  public ModuleDeploymentException() {
    // TODO Auto-generated constructor stub
  }*/

  /**
   * @param message
   */
  public ModuleDeploymentException(Response.Status httpStatus, String msg, Throwable err) {
    super(msg);
    this.responseStatus = httpStatus;
    reason = err;
  }



  /**
   * @param httpStatus
   * @param message
   */
  public ModuleDeploymentException(Response.Status httpStatus, String msg) {
    this(httpStatus, msg, null);
  }

  /**
   * @return the statusCode
   */
  public Response.Status getResponseStatus() {
    return responseStatus;
  }



  /**
   * @return the reason
   */
  public Throwable getReason() {
    return reason;
  }
  
  /**
   * Gets the response status code
   * @return
   */
  public int getStatus() {
    return responseStatus.getStatusCode();
  }



  @Override
  public String toString() {
    return java.text.MessageFormat.format("ModuleDeploymentException'{'status:{0},message:''{1}'',reason:{2}'}'", responseStatus, getMessage(), reason) ;
  }
}
