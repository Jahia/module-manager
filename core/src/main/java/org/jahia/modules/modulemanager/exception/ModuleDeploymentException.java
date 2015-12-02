/**
 * 
 */
package org.jahia.modules.modulemanager.exception;

import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * The Exception raised when a deployment failed. 
 * Contains the underlying error and a HTTP status code to send to the client. 
 * 
 * @author bdjiba
 *
 */
@XmlType(propOrder = {"responseStatus", "message", "cause"})
public class ModuleDeploymentException extends Exception {
  private static final long serialVersionUID = -1886713186574565575L;
  
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
    super(msg, err);
    this.responseStatus = httpStatus;
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
   * Gets the response status code
   * @return
   */
  public int getStatus() {
    return responseStatus.getStatusCode();
  }
  
  @Override
  @XmlTransient
  public StackTraceElement[] getStackTrace() {
    return super.getStackTrace();
  }
  
  @XmlTransient
  @Override
  public String getLocalizedMessage() {
    return super.getLocalizedMessage();
  }

  @Override
  public String toString() {
    return java.text.MessageFormat.format("ModuleDeploymentException'{'status:{0},message:''{1}'',reason:{2}'}'", responseStatus, getMessage(), getCause()) ;
  }
}
