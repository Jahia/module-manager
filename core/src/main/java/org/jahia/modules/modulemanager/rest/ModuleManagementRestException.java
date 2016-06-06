/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
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
package org.jahia.modules.modulemanager.rest;

import java.text.MessageFormat;

import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * The Exception raised when a module management operation fails. Contains the underlying error and a HTTP status code to send to the
 * client.
 * 
 * @author bdjiba
 */
@XmlType(propOrder = { "responseStatus", "message", "cause" })
public class ModuleManagementRestException extends Exception {
    private static final long serialVersionUID = -1886713186574565575L;

    private final Response.Status responseStatus;

    /**
     * Initializes an instance of this class.
     * 
     * @param httpStatus the response status
     * @param message the detail error message
     */
    public ModuleManagementRestException(Response.Status httpStatus, String msg) {
        this(httpStatus, msg, null);
    }

    /**
     * Initializes an instance of this class.
     * 
     * @param httpStatus the response status
     * @param message the detail error message
     * @param cause the cause of the error
     */
    public ModuleManagementRestException(Response.Status httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.responseStatus = httpStatus;
    }

    @XmlTransient
    @Override
    public String getLocalizedMessage() {
        return super.getLocalizedMessage();
    }

    /**
     * Obtains the response status object.
     * 
     * @return the response status object
     */
    public Response.Status getResponseStatus() {
        return responseStatus;
    }

    @Override
    @XmlTransient
    public StackTraceElement[] getStackTrace() {
        return super.getStackTrace();
    }
    
    /**
     * Gets the response status code.
     * 
     * @return the response status code
     */
    public int getStatus() {
        return responseStatus.getStatusCode();
    }

    @Override
    public String toString() {
        return MessageFormat.format("Error '{'status:{0},message:''{1}'',reason:{2}'}'", responseStatus, getMessage(),
                getCause());
    }
}
