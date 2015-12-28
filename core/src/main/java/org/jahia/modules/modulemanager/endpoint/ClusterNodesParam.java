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
import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Aggregate some request parameters to wrap multiple type of nodes parameter usage to call the service.
 * @author bdjiba
 *
 */
@XmlType
public class ClusterNodesParam {
  
  private String nodesAsPathParameter;
  
  @QueryParam("nodes")
  private Set<String> nodesAsQueryParameter;
  
  @FormParam("nodes")
  private Set<String> nodesAsFormParameter;
  
  public ClusterNodesParam( @DefaultValue("") @PathParam("nodes") String nodesAsPathParam) {
    this.nodesAsPathParameter = nodesAsPathParam;
  }


  /**
   * Gets the node ids value from the URL path
   * @return the nodesAsPathParameter
   */
  public String getNodesAsPathParameter() {
    return nodesAsPathParameter;
  }

  /**
   * Sets the node ids specified in the path URL
   * @param nodesAsPathParameter the nodesAsPathParameter to set
   */
  public void setNodesAsPathParameter(String nodesAsPathParameter) {
    this.nodesAsPathParameter = nodesAsPathParameter;
  }


  /**
   * Gets the set of node ids from the query parameter
   * @return the nodesAsQueryParameter
   */
  public Set<String> getNodesAsQueryParameter() {
    return nodesAsQueryParameter;
  }



  /**
   * Sets the set of node id from the query parameter.
   * @param nodesAsQueryParameter the nodesAsQueryParameter to set
   */
  public void setNodesAsQueryParameter(Set<String> nodesAsQueryParameter) {
    this.nodesAsQueryParameter = nodesAsQueryParameter;
  }



  /**
   * Get the node id set
   * @return the nodesAsFormParameter
   */
  public Set<String> getNodesAsFormParameter() {
    return nodesAsFormParameter;
  }



  /**
   * Set the set of the node ids from the form
   * @param nodesAsFormParameter the nodesAsFormParameter to set
   */
  public void setNodesAsFormParameter(Set<String> nodesAsFormParameter) {
    this.nodesAsFormParameter = nodesAsFormParameter;
  }
  
  /**
   * Get the set of the given node identifier.
   * @return the node set otherwise returns null
   */
  public Set<String> getNodesSet(){
    // FIXME: priority path, query and form
    // 1 - resolve from path
    if(StringUtils.isNotBlank(nodesAsPathParameter)) {
      return new CommaSeparatedNodeIdValue(nodesAsPathParameter);
    }
    // 2 - resolve from query
    if(CollectionUtils.isNotEmpty(nodesAsQueryParameter)){
      return nodesAsQueryParameter;
    }
    // 3 - resolve from form but is should be the same as query
    if(CollectionUtils.isNotEmpty(nodesAsFormParameter)) {
      return nodesAsFormParameter;
    }
    
    return null;
  }
  
  /**
   * Gets the node identifier array.
   * @return the node ids or null if empty
   */
  public String[] getNodeIds() {
    Set<String> nodeSet = getNodesSet();
    if(CollectionUtils.isEmpty(nodeSet)) {
      return null;
    }
    return nodeSet.toArray(new String[0]);
  }
  
  @Override
  public String toString() {
    // for serialization
    Set<String> nodeSet =  getNodesSet();
    return CollectionUtils.isEmpty(nodeSet) ? "{}" : nodeSet.toString();
  }
}
