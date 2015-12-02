package org.jahia.modules.modulemanager.payload;

import org.jahia.modules.modulemanager.model.Bundle;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class BundleStateReport {

    String clusterBundle;
    Map<String, String> nodeStates;

    public BundleStateReport() {
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
