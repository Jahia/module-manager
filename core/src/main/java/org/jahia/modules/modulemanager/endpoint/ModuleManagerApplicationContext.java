/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The module bridge to access to module declared Spring bean from Jersey components.
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
  
  /**
   * Get an instance of the specified bean by its name from the bundle Spring context.
   * @param beanName the bean name
   * @return the bean instance if exists otherwise throws a bean exception
   * @throws BeansException the exception thrown when the bean is not found in the context
   */
  public static Object getBean(String beanName) throws BeansException {
    return context.getBean(beanName);
  }
  
  /**
   * Get an instance of the specified bean by its name from the bundle Spring context.
   * @param beanName the bean name
   * @param beanType the bean type
   * @return a bean instance
   * @throws BeansException the exception thrown when the bean is not found in the context
   */
  public static <T> T getBean(String beanName, Class<T> beanType) throws BeansException {
    return context.getBean(beanName, beanType);
  }
  
}
