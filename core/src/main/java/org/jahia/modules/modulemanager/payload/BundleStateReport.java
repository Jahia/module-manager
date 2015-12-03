package org.jahia.modules.modulemanager.payload;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class BundleStateReport {

    private String clusterBundle;
    
    private Map<String, String> nodeStates;

    public BundleStateReport() {
        super();
    }

    public BundleStateReport(String clusterBundle, Map<String, String> nodeStates) {
        this.clusterBundle = clusterBundle;
        this.nodeStates = nodeStates;
    }

    public String getClusterBundle() {
        return clusterBundle;
    }

    public void setClusterBundle(String clusterBundle) {
        this.clusterBundle = clusterBundle;
    }

    public Map<String, String> getNodeStates() {
        return nodeStates;
    }

    public void setNodeStates(Map<String, String> nodeStates) {
        this.nodeStates = nodeStates;
    }

}
