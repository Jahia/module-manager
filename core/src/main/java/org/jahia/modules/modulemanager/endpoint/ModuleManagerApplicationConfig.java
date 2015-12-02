/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import java.util.Set;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jahia.modules.modulemanager.exception.ModuleManagerExceptionMapper;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * @author bdjiba
 *
 */
public class ModuleManagerApplicationConfig extends ResourceConfig {

  /**
   * 
   */
  public ModuleManagerApplicationConfig() {
    register(MultiPartFeature.class);
    register(ModuleManagerResource.class);
    register(JacksonJaxbJsonProvider.class);
    register(ModuleManagerExceptionMapper.class);
    //packages("org.jahia.modules.modulemanager");
  }

  /**
   * @param classes
   */
  public ModuleManagerApplicationConfig(Set<Class<?>> classes) {
    super(classes);
  }

  /**
   * @param classes
   */
  public ModuleManagerApplicationConfig(Class<?>... classes) {
    super(classes);
  }

  /**
   * @param original
   */
  public ModuleManagerApplicationConfig(ResourceConfig original) {
    super(original);
  }

}
