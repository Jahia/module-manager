package org.jahia.modules.modulemanager.rest.filters;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

/**
 * JAX-RS Filter that handle commons headers in responses
 */

@Priority(Priorities.HEADER_DECORATOR)
public class HeadersResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws
            IOException {
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        // tell the client to not cache the responses
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
        headers.add("Pragma", "no-cache"); // for HTTP 1.0
    }
}
