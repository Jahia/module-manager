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
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.jahia.osgi.BundleUtils;
import org.jahia.params.valves.AuthValveContext;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.securityfilter.PermissionService;
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

    @Context
    HttpServletRequest httpServletRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String username = "";
        try {
            JCRSessionWrapper currentUserSession = JCRSessionFactory.getInstance().getCurrentUserSession();
            final JahiaUser jahiaUser = currentUserSession.getUser();
            AuthValveContext ctx = (AuthValveContext) httpServletRequest.getAttribute(AuthValveContext.class.getName());
            username = jahiaUser.getUserKey();
            if (hasPermission(getAction(requestContext)) && ctx != null && !ctx.isAuthRetrievedFromSession()) {
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

        log.warn("Unauthorized access to the resource {} by user {}", httpServletRequest.getRequestURI(), username);
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(String.format("User %s is not allowed to access resource %s", username, httpServletRequest.getRequestURI())).build());
    }

    private String getAction(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        return path.contains("/_") ? path.substring(path.lastIndexOf("/_") + 2) : path;
    }
    private boolean hasPermission(String endpoint) {
        boolean hasPermission;
        try {
            PermissionService permissionService = BundleUtils.getOsgiService(PermissionService.class, null);
            hasPermission = permissionService.hasPermission("module_manager." + endpoint);
            if (!hasPermission) {
                log.warn("No permission to execute bundle operation");
                log.debug("Endpoint: {}", endpoint);
            }
            return hasPermission;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
