package org.jahia.modules.modulemanager.payload;

import java.util.Set;

/**
 * @author achaabni
 */
public class NodeStateReport {

    private String nodeId;
    private Set<BundleStateReport> bundleStateReports;

    public NodeStateReport(String nodeId, Set<BundleStateReport> bundleStateReports) {
        this.nodeId = nodeId;
        this.bundleStateReports = bundleStateReports;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Set<BundleStateReport> getBundleStateReports() {
        return bundleStateReports;
    }

    public void setBundleStateReports(Set<BundleStateReport> bundleStateReports) {
        this.bundleStateReports = bundleStateReports;
    }
}
