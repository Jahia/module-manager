package org.jahia.modules.modulemanager.endpoint;

import java.util.Set;

import javax.ws.rs.DefaultValue;
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
  
  
  @Override
  public String toString() {
    // for serialization
    return CollectionUtils.isEmpty(nodesAsMultiPartParameterSet) ? "{}" : nodesAsMultiPartParameterSet.toString();
  }

}
