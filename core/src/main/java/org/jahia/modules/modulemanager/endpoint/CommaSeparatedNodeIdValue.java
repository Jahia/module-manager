/**
 * 
 */
package org.jahia.modules.modulemanager.endpoint;

import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

/**
 * @author bdjiba
 *
 */
public class CommaSeparatedNodeIdValue extends HashSet<String> {

  private static final long serialVersionUID = 1L;

  public CommaSeparatedNodeIdValue(String commaSeparatedValues) {
    if(StringUtils.isNotBlank(commaSeparatedValues)) {
      String nodes = commaSeparatedValues;
      if(commaSeparatedValues.indexOf("/") == 0) {
        nodes = commaSeparatedValues.substring(1);
      }
      for(String v : StringUtils.split(nodes,",")) {
        if(StringUtils.containsNone(v, "/.")) {
          add(v.trim()); 
        }
      }
    }
  }

}
