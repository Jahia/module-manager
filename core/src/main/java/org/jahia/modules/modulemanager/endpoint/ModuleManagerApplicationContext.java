/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author bdjiba
 *
 */
public class ModuleManagerApplicationContext implements ApplicationContextAware {

  private static ApplicationContext context;
  
  private ModuleManagerApplicationContext() {}

  /* (non-Javadoc)
   * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    context = applicationContext;

  }
  
  public static Object getBean(String beanName) {
    return context.getBean(beanName);
  }
  
  public static <T> T getBean(String beanName, Class<T> beanType) throws BeansException {
    return context.getBean(beanName, beanType);
  }
  
  public static String[] getBeanNames() {
    return context.getBeanDefinitionNames();
  }

}
