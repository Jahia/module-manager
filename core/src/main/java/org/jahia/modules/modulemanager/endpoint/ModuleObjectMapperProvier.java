/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import javax.ws.rs.ext.ContextResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author bdjiba
 *
 */
public class ModuleObjectMapperProvier implements ContextResolver<ObjectMapper> {

  /**
   * 
   */
  public ModuleObjectMapperProvier() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    // TODO Auto-generated method stub
    return null;
  }

}
