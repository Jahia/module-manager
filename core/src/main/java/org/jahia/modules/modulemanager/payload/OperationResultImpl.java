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
package org.jahia.modules.modulemanager.payload;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.jahia.modules.modulemanager.OperationResult;

/**
 * Represents the result of a module operation.
 * 
 * @author Sergiy Shyrkov
 */
@XmlRootElement
public class OperationResultImpl implements OperationResult {

    /**
     * Indicates the fact that the same bundle is already installed.
     */
    public static final OperationResult ALREADY_INSTALLED = new OperationResultImpl(false,
            "The bundle is already installed");

    /**
     * Represents a successfully fulfilled or submitted operation.
     */
    public static final OperationResult NOT_VALID_BUNDLE = new OperationResultImpl(false,
            "Submitted bundle is either not a valid OSGi bundle or has no required manifest headers"
                    + " Bundle-SymbolicName and Implementation-Version/Bundle-Version");

    private static final long serialVersionUID = -6027106110628985206L;

    /**
     * Represents a successfully fulfilled or submitted operation.
     */
    public static final OperationResult SUCCESS = new OperationResultImpl(true, "Operation successfully performed");
    
    /**
     * Represents a default failed operation.
     */
    public static final OperationResult FAIL = new OperationResultImpl(false, "Operation failed");


    private String[] messages;

    private boolean success;

    private String operationId;
    
    @XmlAttribute(name="bundleInfos", required = false)
    private List<BundleInfo> bundleInfoList;

    /**
     * Initializes an instance of this class.
     * 
     * @param success
     *            <code>true</code> if an operation was successful
     * @param message
     *            description of the operation result
     */
    public OperationResultImpl(boolean success, String message) {
        this.success = success;
        this.messages = new String[] { message };
    }

    /**
     * Initializes an instance of this class.
     *
     * @param success
     *            <code>true</code> if an operation was successful
     * @param message
     *            description of the operation result
     * @param operationId Operation Identifier
     */
    public OperationResultImpl(boolean success, String message,String operationId) {
        this(success, message);
        this.operationId = operationId;
    }


    public String[] getMessages() {
        return messages;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setMessages(String message) {
        this.messages = new String[] { message };
    }
    
    public void setMessages(String[] v) {
      this.messages = v;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    /**
     * Get the bundle info list
     * @return the bundleInfoList the list of info
     */
    public List<BundleInfo> getBundleInfoList() {
      if(bundleInfoList == null) {
        bundleInfoList = new ArrayList<BundleInfo>();
      }
      return bundleInfoList;
    }

    /**
     * Set the bundle info list
     * @param bundleInfoList the bundleInfoList to set
     */
    public void setBundleInfoList(List<BundleInfo> bundleInfoList) {
      this.bundleInfoList = bundleInfoList;
    }

    @Override
    public void addMessage(String v) {
      messages = (String[])ArrayUtils.add(messages, v);
    }
    
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }


}
