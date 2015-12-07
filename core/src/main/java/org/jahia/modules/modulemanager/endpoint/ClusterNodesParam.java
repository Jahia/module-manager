package org.jahia.modules.modulemanager.endpoint;

import java.util.HashSet;
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
   * @return the nodesAsPathParameter
   */
  public String getNodesAsPathParameter() {
    return nodesAsPathParameter;
  }

  /**
   * @param nodesAsPathParameter the nodesAsPathParameter to set
   */
  public void setNodesAsPathParameter(String nodesAsPathParameter) {
    this.nodesAsPathParameter = nodesAsPathParameter;
  }


  /**
   * @return the nodesAsQueryParameter
   */
  public Set<String> getNodesAsQueryParameter() {
    return nodesAsQueryParameter;
  }



  /**
   * @param nodesAsQueryParameter the nodesAsQueryParameter to set
   */
  public void setNodesAsQueryParameter(Set<String> nodesAsQueryParameter) {
    this.nodesAsQueryParameter = nodesAsQueryParameter;
  }



  /**
   * @return the nodesAsFormParameter
   */
  public Set<String> getNodesAsFormParameter() {
    return nodesAsFormParameter;
  }



  /**
   * @param nodesAsFormParameter the nodesAsFormParameter to set
   */
  public void setNodesAsFormParameter(Set<String> nodesAsFormParameter) {
    this.nodesAsFormParameter = nodesAsFormParameter;
  }
  
  // FIXME: priority path, query and form
  public Set<String> getNodesSet(){
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
