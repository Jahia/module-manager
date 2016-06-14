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
package org.jahia.modules.modulemanager.rest.filters;

import org.jahia.exceptions.JahiaUnauthorizedException;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;

/**
 * JAX-RS Filter that filters only users that match the required permission
 */

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ModuleManagerAuthenticationRequestFilter implements ContainerRequestFilter {

    @Context
    HttpServletRequest httpServletRequest;

    final private static String REQUIRED_PERMISSON = "adminTemplates";

    private static final Logger log = LoggerFactory.getLogger(ModuleManagerAuthenticationRequestFilter.class);

    public ModuleManagerAuthenticationRequestFilter() {
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            final JahiaUser user = getAllowedJahiaUser();
            if (user == null) {
                abort(requestContext);
                return;
            }
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return user;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return httpServletRequest.isUserInRole(role);
                }

                @Override
                public boolean isSecure() {
                    return httpServletRequest.isSecure();
                }

                @Override
                public String getAuthenticationScheme() {
                    return httpServletRequest.getScheme();
                }
            });
        } catch (RepositoryException e) {
            log.error("an error occurs while accessing the module manager API ", e);
            abort(requestContext);
        }
    }

    private void abort(ContainerRequestContext requestContext) {
        requestContext.abortWith(Response
                .status(Response.Status.UNAUTHORIZED)
                .entity("Unauthorized request")
                .build());
    }

    private JahiaUser getAllowedJahiaUser() throws RepositoryException {
        JCRSessionWrapper currentUserSession = JCRSessionFactory.getInstance().getCurrentUserSession();
        JahiaUser user =  currentUserSession.getUser();
        if (currentUserSession.getRootNode().hasPermission(REQUIRED_PERMISSON)) {
            return user;
        }
        log.warn("Unauthorized access to the API by user {}", user != null ? user.getUserKey() : "unknown");
        return null;
    }
}
