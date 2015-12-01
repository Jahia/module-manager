/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import java.util.Set;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * @author bdjiba
 *
 */
//@ApplicationPath("/bundles")
public class ModuleManagerApplicationConfig extends ResourceConfig {

  /**
   * 
   */
  public ModuleManagerApplicationConfig() {
    /*ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    // create JsonProvider to provide custom ObjectMapper
    JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
    provider.setMapper(mapper);*/
    register(MultiPartFeature.class);
    register(ModuleManagerResource.class);
    //register(ModuleManagerExceptionMapper.class);
    //register(JacksonFeature.class);
    register(JacksonJaxbJsonProvider.class);
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
