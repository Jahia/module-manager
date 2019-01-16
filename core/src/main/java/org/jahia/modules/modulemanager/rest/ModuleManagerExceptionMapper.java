/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Provide a mapping of the module management related exception into a displayable client response.
 *
 * @author bdjiba
 */
public class ModuleManagerExceptionMapper implements ExceptionMapper<Exception> {

    /**
     * A (part of) REST response representing an error handling the REST call.
     */
    @XmlRootElement
    @XmlType(propOrder = { "status", "reasonPhrase", "message", "cause" })
    private static class ErrorInfo {

        private final String cause;
        private final String message;
        private final Status status;

        /**
         * Create an error info instance.
         *
         * @param status HTTP response status
         * @param message Error message
         * @param cause Error cause if any
         */
        public ErrorInfo(Status status, String message, String cause) {
            this.status = status;
            this.message = message;
            this.cause = cause;
        }

        /**
         * @return Error cause if any
         */
        @XmlElement
        public String getCause() {
            return cause;
        }

        /**
         * @return Error message
         */
        @XmlElement
        public String getMessage() {
            return message;
        }

        /**
         * @return HTTP response status description
         */
        @XmlElement
        public String getReasonPhrase() {
            return status.getReasonPhrase();
        }

        /**
         * @return HTTP response status
         */
        @XmlElement
        public int getStatus() {
            return status.getStatusCode();
        }
    }

    /**
     * Convert an exception to an error info
     *
     * @param ex Exception
     * @return Error info
     */
    private static ErrorInfo getErrorInfo(Exception ex) {
        int statusCode = ex instanceof WebApplicationException
                ? ((WebApplicationException) ex).getResponse().getStatus()
                : Status.INTERNAL_SERVER_ERROR.getStatusCode();
        Throwable cause = Response.Status.Family.familyOf(statusCode) == Response.Status.Family.SERVER_ERROR
                ? ExceptionUtils.getRootCause(ex)
                : null;
        return new ErrorInfo(Status.fromStatusCode(statusCode), ex.getMessage(), (cause == null ? null : cause.toString()));
    }

    @Override
    public Response toResponse(Exception exception) {
        ErrorInfo errorInfo = getErrorInfo(exception);
        return Response.status(errorInfo.status).entity(errorInfo).build();
    }
}
