/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2025 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.test;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpHeaders;
import org.apache.xerces.impl.dv.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.SimpleCredentials;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Intermediate class used to offer an equivalent to {@link JahiaTestCase#post(String, String[]...)} with basic authentication.
 * {@link org.jahia.test.JahiaTestCase.PostResult} has a package visibility so this class must be located in the <code>org.jahia.test</code> package to access its constructor.
 * The new method is not added to the parent {@link JahiaTestCase} (which would have been cleaner) so that the test module remains compatible with any Jahia versions >= 8.0.3.0.
 */
public abstract class JahiaBasicAuthTestCase extends JahiaTestCase {

    private static final Logger logger = LoggerFactory.getLogger(JahiaBasicAuthTestCase.class);

    /**
     * Copy of {@link JahiaTestCase#post(String, String[]...)} but with a basic authorization header.
     *
     * @param url    the URL
     * @param params optional request parameters
     * @return a new {@link org.jahia.test.JahiaTestCase.PostResult}
     * @throws IOException if an error occurs
     */
    protected PostResult postWithBasicAuth(String url, String[]... params) throws IOException {
        PostMethod method = new PostMethod(url);

        for (String[] param : params) {
            method.addParameter(param[0], param[1]);
        }
        // add a basic auth header
        Map.Entry<String, String> basicAuthHeader = getBasicAuthHeader();
        method.addRequestHeader(basicAuthHeader.getKey(), basicAuthHeader.getValue());

        method.getParams().setParameter("http.method.retry-handler", new DefaultHttpMethodRetryHandler(3, false));
        int statusCode;
        String statusLine;
        String responseBody;

        try {
            statusCode = this.getHttpClient().executeMethod(method);
            statusLine = method.getStatusLine().toString();
            if (statusCode != 200) {
                logger.warn("Method failed: {}", statusLine);
            }

            responseBody = method.getResponseBodyAsString();
        } finally {
            method.releaseConnection();
        }

        return new PostResult(statusCode, statusLine, responseBody);
    }

    /**
     * Returns a map only containing a basic authorization header.
     * This is a shortcut when only the basic auth header is needed.
     *
     * @return a map with a basic authorization header
     */
    protected static Map<String, String> getHeadersWithBasicAuth() {
        Map<String, String> headers = new HashMap<>();
        headers.entrySet().add(getBasicAuthHeader());
        return headers;
    }

    /**
     * Returns an entry, with its name and value, of a basic authorization header.
     *
     * @return a map entry a basic auth header
     */
    protected static Map.Entry<String, String> getBasicAuthHeader() {
        SimpleCredentials credentials = getRootUserCredentials();
        return new AbstractMap.SimpleEntry<>(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encode((credentials.getUserID() + ":" + String.valueOf(credentials.getPassword())).getBytes()));
    }
}
