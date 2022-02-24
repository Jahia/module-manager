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
package org.jahia.modules.modulemanager.rest.filters;

import java.io.IOException;
import java.security.Principal;

import javax.annotation.Priority;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS Filter that filters only users that match the required permission or role.
 */
@Priority(Priorities.AUTHENTICATION)
public class ModuleManagerAuthenticationRequestFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ModuleManagerAuthenticationRequestFilter.class);

    private static final String REQUIRED_PERMISSON = "adminTemplates";

    private static final String REQUIRED_ROLE = "toolManager";

    @Context
    HttpServletRequest httpServletRequest;

    private Subject getAuthenticatedSubject() {
        try {
            return WebUtils.getAuthenticatedSubject(httpServletRequest);
        } catch (AuthenticationException e) {
            throw new NotAuthorizedException(e.getMessage(), HttpServletRequest.BASIC_AUTH);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        JahiaUser user = JCRSessionFactory.getInstance().getCurrentUser();
        String username = JahiaUserManagerService.GUEST_USERNAME;
        if (JahiaUserManagerService.isGuest(user)) {
            Subject subject = getAuthenticatedSubject();
            if (subject != null && subject.hasRole(REQUIRED_ROLE)) {
                // user has the required role: allow access
                return;
            }
        } else {
            try {
                JCRSessionWrapper currentUserSession = JCRSessionFactory.getInstance().getCurrentUserSession();
                final JahiaUser jahiaUser = currentUserSession.getUser();
                username = jahiaUser.getUserKey();
                if (currentUserSession.getRootNode().hasPermission(REQUIRED_PERMISSON)) {
                    requestContext.setSecurityContext(new SecurityContext() {
    
                        @Override
                        public String getAuthenticationScheme() {
                            return httpServletRequest.getScheme();
                        }
    
                        @Override
                        public Principal getUserPrincipal() {
                            return jahiaUser;
                        }
    
                        @Override
                        public boolean isSecure() {
                            return httpServletRequest.isSecure();
                        }
    
                        @Override
                        public boolean isUserInRole(String role) {
                            return httpServletRequest.isUserInRole(role);
                        }
                    });
                    
                    return;
                }
            } catch (RepositoryException e) {
                log.error("An error occurs while accessing the resource " + httpServletRequest.getRequestURI(), e);
                requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(String.format("an error occured %s (see server log for more detail)",
                                e.getMessage() != null ? e.getMessage() : e))
                        .build());
            }
        }

        log.warn("Unauthorized access to the resource {} by user {}", httpServletRequest.getRequestURI(), username);
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(String.format("User %s is not allowed to access resource %s", username, httpServletRequest.getRequestURI())).build());
    }
}
