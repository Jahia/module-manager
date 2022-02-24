/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
