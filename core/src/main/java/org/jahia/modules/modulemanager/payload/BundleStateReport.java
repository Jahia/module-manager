package org.jahia.modules.modulemanager.payload;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * Represents a bundle state.
 */
@XmlRootElement
public class BundleStateReport {

    private String bundle;
    
    private Map<String, String> nodeStates;

    public BundleStateReport() {
        super();
    }

    public BundleStateReport(String clusterBundle, Map<String, String> nodeStates) {
        this.bundle = clusterBundle;
        this.nodeStates = nodeStates;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String clusterBundle) {
        this.bundle = clusterBundle;
    }

    public Map<String, String> getNodeStates() {
        return nodeStates;
    }

    public void setNodeStates(Map<String, String> nodeStates) {
        this.nodeStates = nodeStates;
    }

}
