/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import javax.ws.rs.ext.ContextResolver;

import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.AnnotationIntrospector.Pair;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;

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
