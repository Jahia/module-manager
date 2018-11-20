package org.jahia.test.services.modulemanager;

import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Jahia;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.jahia.test.JahiaTestCase;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class ModuleManagementRestApiTest extends JahiaTestCase {

    private static final String USER = "root";
    private static final String PASSWORD = "root1234";

    private static final String MODULE_MANAGER_GROUP = "org.jahia.modules";
    private static final String MODULE_MANAGER_NAME = "module-manager";
    private static final String MODULE_MANAGER_FULL_NAME = (MODULE_MANAGER_GROUP + "/" + MODULE_MANAGER_NAME);

    private static final String MODULE_MANAGER_TEST_GROUP = "org.jahia.test";
    private static final String MODULE_MANAGER_TEST_NAME = "module-manager-test";
    private static final String MODULE_MANAGER_TEST_FULL_NAME = (MODULE_MANAGER_TEST_GROUP + "/" + MODULE_MANAGER_TEST_NAME);

    private static HttpClient client;
    private static String moduleManagerVersion;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {

        URL url = new URL(getBaseServerURL() + Jahia.getContextPath());
        Credentials credentials = new UsernamePasswordCredentials(USER, PASSWORD);
        client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        client.getHostConfiguration().setHost(url.getHost(), url.getPort(), url.getProtocol());
        client.getState().setCredentials(new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM), credentials);

        moduleManagerVersion = getModuleManagerVersion();
    }

    @Test
    public void shouldRetrieveSingleModuleInfoByKey() throws Exception {
        verifyModuleInfoRetrieval(getModuleManagerKey(MODULE_MANAGER_NAME));
    }

    @Test
    public void shouldRetrieveSingleModuleInfoByFullKey() throws Exception {
        verifyModuleInfoRetrieval(getModuleManagerKey(MODULE_MANAGER_FULL_NAME));
    }

    @Test
    public void shouldGetErrorNotRetrieveSingleModuleInfoByWrongKey() throws Exception {
        verifyModuleInfoRetrievalError(MODULE_MANAGER_NAME, HttpServletResponse.SC_BAD_REQUEST);
        verifyModuleInfoRetrievalError("nonExistingBundle/1.0.0", HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void shouldRetrieveMultipleModuleInfosByKey() throws Exception {
        verifyModuleInfosRetrieval(
            getModuleManagerKey(MODULE_MANAGER_NAME),
            getModuleManagerKey(MODULE_MANAGER_TEST_NAME)
        );
    }

    @Test
    public void shouldRetrieveMultipleModuleInfosByFullKey() throws Exception {
        verifyModuleInfosRetrieval(
            getModuleManagerKey(MODULE_MANAGER_FULL_NAME),
            getModuleManagerKey(MODULE_MANAGER_TEST_FULL_NAME)
        );
    }

    @Test
    public void shouldGetErrorNotRetrieveMultipleModuleInfosByWrongKey() throws Exception {
        verifyModuleInfoRetrievalError(getMultipleBundlesSelector(MODULE_MANAGER_NAME, getModuleManagerKey(MODULE_MANAGER_NAME)), HttpServletResponse.SC_BAD_REQUEST);
        verifyModuleInfoRetrievalError(getMultipleBundlesSelector("nonExistingBundle/1.0.0", getModuleManagerKey(MODULE_MANAGER_FULL_NAME)), HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void shouldRetrieveBucketModuleInfosByName() throws Exception {
        verifyBucketModuleInfoRetrieval(MODULE_MANAGER_NAME, getModuleManagerKey(MODULE_MANAGER_FULL_NAME));
    }

    @Test
    public void shouldRetrieveBucketModuleInfosByGroupAndName() throws Exception {
        verifyBucketModuleInfoRetrieval(MODULE_MANAGER_FULL_NAME, getModuleManagerKey(MODULE_MANAGER_FULL_NAME));
    }

    @Test
    public void shouldRetrieveNoBucketModuleInfosByWrongName() throws Exception {
        JSONObject response = getBundleInfo("nonExistingBundle/*");
        Assert.assertEquals(0, response.length());
    }

    @Test
    public void shouldRetrieveAllModuleInfos() throws Exception {
        JSONObject response = getBundleInfo("*");
        verifyModuleInfo(response.getJSONObject(getModuleManagerKey(MODULE_MANAGER_FULL_NAME)));
        verifyModuleInfo(response.getJSONObject(getModuleManagerKey(MODULE_MANAGER_TEST_FULL_NAME)));
    }

    private void verifyModuleInfoRetrieval(String bundleKey) throws Exception {
        JSONObject response = getBundleInfo(bundleKey);
        verifyModuleInfo(response);
    }

    private void verifyModuleInfosRetrieval(String... bundleKeys) throws Exception {
        JSONObject response = getBundleInfo(getMultipleBundlesSelector(bundleKeys));
        Assert.assertEquals(bundleKeys.length, response.length());
        for (String bundleKey : bundleKeys) {
            verifyModuleInfo(response.getJSONObject(bundleKey));
        }
    }

    private void verifyBucketModuleInfoRetrieval(String bundleBucketKey, String... expectedBundleKeys) throws Exception {
        JSONObject response = getBundleInfo(bundleBucketKey + "/*");
        Assert.assertEquals(expectedBundleKeys.length, response.length());
        for (String expectedBundleKey : expectedBundleKeys) {
            verifyModuleInfo(response.getJSONObject(expectedBundleKey));
        }
    }

    private void verifyModuleInfoRetrievalError(String bundleSelector, int expectedResponseCode) throws Exception {
        int responseCode = client.executeMethod(newGetMethod(bundleSelector));
        Assert.assertEquals(expectedResponseCode, responseCode);
    }

    private static void verifyModuleInfo(JSONObject moduleInfo) throws Exception {
        Assert.assertEquals("MODULE", moduleInfo.getString("type"));
        Assert.assertEquals("ACTIVE", moduleInfo.getString("osgiState"));
        Assert.assertEquals("STARTED", moduleInfo.getString("moduleState"));
    }

    private static String getModuleManagerKey(String moduleName) throws Exception {
        return (moduleName + '/' + moduleManagerVersion);
    }

    private static String getMultipleBundlesSelector(String... bundleKeys) {
        return ("%5B" + StringUtils.join(bundleKeys, ',') + "%5D");
    }

    private JSONObject getBundleInfo(String bundleSelector) throws Exception {
        GetMethod get = newGetMethod(bundleSelector);
        int responseCode = client.executeMethod(get);
        Assert.assertEquals(HttpServletResponse.SC_OK, responseCode);
        String response = get.getResponseBodyAsString();
        return new JSONObject(response);
    }

    private static GetMethod newGetMethod(String bundleSelector) {
        return new GetMethod(getBaseServerURL() + Jahia.getContextPath() + "/modules/api/bundles/" + bundleSelector + "/_localInfo");
    }

    private static String getModuleManagerVersion() throws Exception {
        Bundle moduleManager = null;
        for (Bundle bundle : FrameworkService.getBundleContext().getBundles()) {
            if (MODULE_MANAGER_NAME.equals(bundle.getSymbolicName()) && MODULE_MANAGER_GROUP.equals(BundleUtils.getModuleGroupId(bundle))) {
                if (moduleManager != null) {
                    throw new IllegalStateException("Multiple Module Manager module versions installed");
                }
                moduleManager = bundle;
            }
        }
        if (moduleManager == null) {
            throw new IllegalStateException("No Module Manager module installed");
        }
        return moduleManager.getVersion().toString();
    }
}
