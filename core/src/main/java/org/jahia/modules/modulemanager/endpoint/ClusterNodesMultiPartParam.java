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

import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * Aggregate multipart request parameters to wrap multiple type of nodes parameter usage to call the service.
 * @author bdjiba
 */
@XmlType
public class ClusterNodesMultiPartParam {
  @FormDataParam("nodes")
  private Set<String> nodesAsMultiPartParameterSet;
  
  private String nodesAsPathParameter;
  
  // inject the request preferred language if provided
  // otherwise use EN as default user language
  @DefaultValue("en")
  @HeaderParam("Accept-Language")
  private String acceptedLanguage;
  
  
  public ClusterNodesMultiPartParam( @DefaultValue("") @PathParam("nodes") String v) {
    this.nodesAsPathParameter = v;
  }

  /**
   * Gets the set of node ids from the the multipart form
   * @return the nodesAsMultiPartParameterSet
   */
  public Set<String> getNodesAsMultiPartParameterSet() {
    return nodesAsMultiPartParameterSet;
  }

  /**
   * Sets the node ids set
   * @param nodesAsMultiPartParameterSet the nodesAsMultiPartParameterSet to set
   */
  public void setNodesAsMultiPartParameterSet(Set<String> nodesAsMultiPartParameterSet) {
    this.nodesAsMultiPartParameterSet = nodesAsMultiPartParameterSet;
  }

  /**
   * Gets the node ids value with comma-separated
   * @return the nodesAsPathParameter or null
   */
  public String getNodesAsPathParameter() {
    return nodesAsPathParameter;
  }


  /**
   * Sets the node ids value with comma-separated
   * @param v the nodesAsPathParameter to set
   */
  public void setNodesAsPathParameter(String v) {
    this.nodesAsPathParameter = v;
  }


  /**
   * Gets the node set value
   * @return the set of node or null
   */
  public Set<String> getNodesSet(){
    // 1 - resolve from path
    if(StringUtils.isNotBlank(nodesAsPathParameter)) {
      return new CommaSeparatedNodeIdValue(nodesAsPathParameter);
    }
    //2 - form params
    return nodesAsMultiPartParameterSet;
  }
  
  /**
   * Get the array of node ids
   * @return the node ids or null
   */
  public String[] getNodeIds() {
    Set<String> nodeSet = getNodesSet();
    if(CollectionUtils.isEmpty(nodeSet)) {
      return null;
    }
    return nodeSet.toArray(new String[0]);
  }
  
  
  /**
   * @return the acceptedLanguage
   */
  public String getAcceptedLanguage() {
    return acceptedLanguage;
  }

  /**
   * @param acceptedLanguage the acceptedLanguage to set
   */
  public void setAcceptedLanguage(String acceptedLanguage) {
    this.acceptedLanguage = acceptedLanguage;
  }

  @Override
  public String toString() {
    // for serialization
    return CollectionUtils.isEmpty(nodesAsMultiPartParameterSet) ? "{}" : nodesAsMultiPartParameterSet.toString();
  }

}
