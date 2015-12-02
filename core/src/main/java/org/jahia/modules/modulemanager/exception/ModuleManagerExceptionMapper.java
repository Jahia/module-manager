/**
 * 
 */
package org.jahia.modules.modulemanager.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Provide a mapping of the module exception to display as the response to the client. 
 * @author bdjiba
 *
 */
public class ModuleManagerExceptionMapper implements ExceptionMapper<ModuleDeploymentException> {

  /**
   * 
   */
  public ModuleManagerExceptionMapper() {
    /* EMPTY */
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
   */
  @Override
  public Response toResponse(ModuleDeploymentException exception) {
    return Response.status(exception.getStatus()).entity(exception).build();
  }

}
