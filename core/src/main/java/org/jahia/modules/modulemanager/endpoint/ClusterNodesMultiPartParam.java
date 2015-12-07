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
 *
 */
@XmlType
public class ClusterNodesMultiPartParam {
  //private static final Logger log = LoggerFactory.getLogger(ClusterNodesMultiPartParam.class);
  @FormDataParam("nodes")
  private Set<String> nodesAsMultiPartParameterSet;
  
  private String nodesAsPathParameter;
  
  
  public ClusterNodesMultiPartParam( @DefaultValue("") @PathParam("nodes") String v) {
    this.nodesAsPathParameter = v;
  }

  /**
   * @return the nodesAsMultiPartParameterSet
   */
  public Set<String> getNodesAsMultiPartParameterSet() {
    return nodesAsMultiPartParameterSet;
  }

  /**
   * @param nodesAsMultiPartParameterSet the nodesAsMultiPartParameterSet to set
   */
  public void setNodesAsMultiPartParameterSet(Set<String> nodesAsMultiPartParameterSet) {
    this.nodesAsMultiPartParameterSet = nodesAsMultiPartParameterSet;
  }

  /**
   * @return the nodesAsPathParameter
   */
  public String getNodesAsPathParameter() {
    return nodesAsPathParameter;
  }


  /**
   * @param v the nodesAsPathParameter to set
   */
  public void setNodesAsPathParameter(String v) {
    this.nodesAsPathParameter = v;
  }


  /**
   * Get the node set value
   * @return
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
   * Get the node ids
   * @return
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
