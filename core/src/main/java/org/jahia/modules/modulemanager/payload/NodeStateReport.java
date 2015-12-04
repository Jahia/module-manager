package org.jahia.modules.modulemanager.payload;

import java.util.Set;

/**
 * Represents an information about bundle states of the current DX node.
 * 
 * @author achaabni
 */
public class NodeStateReport {

    private String nodeId;
    private Set<BundleStateReport> bundles;

    public NodeStateReport(String nodeId, Set<BundleStateReport> bundleStateReports) {
        this.nodeId = nodeId;
        this.bundles = bundleStateReports;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Set<BundleStateReport> getBundles() {
        return bundles;
    }

    public void setBundles(Set<BundleStateReport> bundleStateReports) {
        this.bundles = bundleStateReports;
    }
}
