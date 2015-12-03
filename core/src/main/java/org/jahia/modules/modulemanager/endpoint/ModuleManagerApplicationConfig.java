/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jahia.modules.modulemanager.exception.ModuleManagerExceptionMapper;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * Application configuration component.
 * Register jersey components
 * @author bdjiba
 *
 */
public class ModuleManagerApplicationConfig extends ResourceConfig {

  /**
   * Constructor
   */
  public ModuleManagerApplicationConfig() {
    register(MultiPartFeature.class);
    register(ModuleManagerResource.class);
    register(JacksonJaxbJsonProvider.class);
    register(ModuleManagerExceptionMapper.class);
  }
}
