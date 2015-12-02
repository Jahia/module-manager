/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.2 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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
package org.jahia.modules.modulemanager.model;

import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

/**
 * This object represents a cluster node. 
 * 
 * @author Sergiy Shyrkov
 */
@Node(jcrType = "jmm:node", discriminator = false)
public class ClusterNode extends BasePersistentObject {

    private static final long serialVersionUID = 4606202580861227782L;

    @Collection(jcrName = "bundles", proxy = true)
    private TreeMap<String, NodeBundle> bundles = new TreeMap<>();

    @Collection(jcrName = "operations", proxy = true)
    private LinkedHashMap<String, NodeOperation> operations = new LinkedHashMap<>();

    @Field(jcrName = "j:processingServer")
    private boolean processingServer = false;

    @Field(jcrName = "j:started")
    private boolean started = true;

    @Field(jcrName = "j:type")
    private String type;

    /**
     * Initializes an instance of this class.
     */
    public ClusterNode() {
        super();
    }

    /**
     * Initializes an instance of this class.
     * 
     * @param name the cluster node identifier
     * @param processingServer <code>true</code> for processing node; <code>false</code> otherwise
     */
    public ClusterNode(String name, boolean processingServer) {
        super(name);
        this.processingServer = processingServer;
    }

    public TreeMap<String, NodeBundle> getBundles() {
        return bundles;
    }

    public LinkedHashMap<String, NodeOperation> getOperations() {
        return operations;
    }

    public String getType() {
        return type;
    }

    public boolean isProcessingServer() {
        return processingServer;
    }

    public boolean isStarted() {
        return started;
    }

    public void setBundles(TreeMap<String, NodeBundle> bundles) {
        this.bundles = bundles;
    }

    public void setOperations(LinkedHashMap<String, NodeOperation> operations) {
        this.operations = operations;
    }

    public void setProcessingServer(boolean processingServer) {
        this.processingServer = processingServer;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public void setType(String type) {
        this.type = type;
    }
}
