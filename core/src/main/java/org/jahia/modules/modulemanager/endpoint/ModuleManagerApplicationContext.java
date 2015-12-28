/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.modulemanager.endpoint;

import org.jahia.services.SpringContextSingleton;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The module bridge to access to module declared Spring bean from Jersey components.
 * @author bdjiba
 *
 */
public class ModuleManagerApplicationContext implements ApplicationContextAware  {

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
    return lookupBean(beanName, null);
  }
  
  /**
   * Get an instance of the specified bean by its name from the bundle Spring context.
   * @param beanName the bean name
   * @param beanType the bean type
   * @return a bean instance
   * @throws BeansException the exception thrown when the bean is not found in the context
   */
  public static <T> T getBean(String beanName, Class<T> beanType) throws BeansException {
    return lookupBean(beanName, beanType);
  }

  // Reprensentes a bridge that access to Servlet container spring beans
  private static <T> T lookupBeanFromServletContext(String beanName, Class<T> beanType) throws BeansException {
    return (T) SpringContextSingleton.getBean(beanName);
  }
  
  // lookup a bean in the bundle spring context and if not found, get from servlet container
  private static <T> T lookupBean(String beanName, Class<T> beanType) throws BeansException {
    try {
      T targetBean = context.getBean(beanName, beanType);
      return targetBean;
    } catch(BeansException bex) {
      return lookupBeanFromServletContext(beanName, beanType);
    }
  }
  
}
