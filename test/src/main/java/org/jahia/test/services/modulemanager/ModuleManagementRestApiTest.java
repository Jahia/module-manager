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
package org.jahia.test.services.modulemanager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Jahia;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.modulemanager.persistence.jcr.BundleInfoJcrHelper;
import org.jahia.test.JahiaTestCase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class ModuleManagementRestApiTest extends JahiaTestCase {

    private static final String JAHIA_MODULES_GROUP = "org.jahia.modules";
    private static final String MODULE_MANAGER_GROUP = JAHIA_MODULES_GROUP;
    private static final String MODULE_MANAGER_NAME = "module-manager";
    private static final String MODULE_MANAGER_FULL_NAME = (MODULE_MANAGER_GROUP + "/" + MODULE_MANAGER_NAME);

    private static final String MODULE_MANAGER_TEST_GROUP = "org.jahia.test";
    private static final String MODULE_MANAGER_TEST_NAME = "module-manager-test";
    private static final String MODULE_MANAGER_TEST_FULL_NAME = (MODULE_MANAGER_TEST_GROUP + "/" + MODULE_MANAGER_TEST_NAME);
    private static final String LOCATION = "location";
    private static final String STATE = "state";
    private static final String SYMBOLIC_NAME = "symbolicName";
    private static final String VERSION = "version";

    private static String moduleManagerVersion;

    @BeforeClass
    public static void oneTimeSetUp() {
        moduleManagerVersion = getModuleManagerVersion();
    }

    @Before
    public void setUp() throws IOException {
        loginRoot();
    }

    @After
    public void tearDown() throws IOException {
        logout();
    }

    @Test
    public void shouldRetrieveSingleModuleInfoByKey() throws JSONException {
        verifyModuleInfoRetrieval(getModuleManagerKey(MODULE_MANAGER_NAME));
    }

    @Test
    public void shouldRetrieveSingleModuleInfoByFullKey() throws JSONException {
        verifyModuleInfoRetrieval(getModuleManagerKey(MODULE_MANAGER_FULL_NAME));
    }

    @Test
    public void shouldGetErrorNotRetrieveSingleModuleInfoByWrongKey() {
        verifyModuleInfoRetrievalError(MODULE_MANAGER_NAME, HttpServletResponse.SC_BAD_REQUEST);
        verifyModuleInfoRetrievalError("nonExistingBundle/1.0.0", HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void shouldRetrieveMultipleModuleInfosByKey() throws JSONException {
        verifyModuleInfosRetrieval(
            getModuleManagerKey(MODULE_MANAGER_NAME),
            getModuleManagerKey(MODULE_MANAGER_TEST_NAME)
        );
    }

    @Test
    public void shouldRetrieveMultipleModuleInfosByFullKey() throws JSONException {
        verifyModuleInfosRetrieval(
            getModuleManagerKey(MODULE_MANAGER_FULL_NAME),
            getModuleManagerKey(MODULE_MANAGER_TEST_FULL_NAME)
        );
    }

    @Test
    public void shouldGetErrorNotRetrieveMultipleModuleInfosByWrongKey() {
        verifyModuleInfoRetrievalError(getMultipleBundlesSelector(MODULE_MANAGER_NAME, getModuleManagerKey(MODULE_MANAGER_NAME)), HttpServletResponse.SC_BAD_REQUEST);
        verifyModuleInfoRetrievalError(getMultipleBundlesSelector("nonExistingBundle/1.0.0", getModuleManagerKey(MODULE_MANAGER_FULL_NAME)), HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void shouldRetrieveBucketModuleInfosByName() throws JSONException {
        verifyBucketModuleInfoRetrieval(MODULE_MANAGER_NAME, getModuleManagerKey(MODULE_MANAGER_FULL_NAME));
    }

    @Test
    public void shouldRetrieveAllModuleInfosByGroup() throws JSONException {
        JSONObject response = getBundleInfo(JAHIA_MODULES_GROUP + "/*/*");
        @SuppressWarnings("rawtypes")
        Iterator keyIterator = response.keys();
        Assert.assertTrue(keyIterator.hasNext());
        while (keyIterator.hasNext()) {
            String key = (String) keyIterator.next();
            Assert.assertTrue(key.startsWith(JAHIA_MODULES_GROUP + "/"));
        }
    }

    @Test
    public void shouldRetrieveBucketModuleInfosByGroupAndName() throws JSONException {
        verifyBucketModuleInfoRetrieval(MODULE_MANAGER_FULL_NAME, getModuleManagerKey(MODULE_MANAGER_FULL_NAME));
    }

    @Test
    public void shouldRetrieveNoBucketModuleInfosByWrongName() throws JSONException {
        JSONObject response = getBundleInfo("nonExistingBundle/*");
        Assert.assertEquals(0, response.length());
    }

    @Test
    public void shouldRetrieveAllModuleInfos() throws JSONException {
        JSONObject response = getBundleInfo("*");
        verifyModuleInfo(response.getJSONObject(getModuleManagerKey(MODULE_MANAGER_FULL_NAME)));
        verifyModuleInfo(response.getJSONObject(getModuleManagerKey(MODULE_MANAGER_TEST_FULL_NAME)));
    }

    @Test
    public void shouldHaveFragmentType() throws JSONException {
        JSONObject moduleInfo = getBundleInfo(getBlueprintExtenderConfigSelector());
        Assert.assertEquals("FRAGMENT", moduleInfo.getString("type"));
        Assert.assertEquals("RESOLVED", moduleInfo.getString("osgiState"));
    }

    @Test
    public void shouldStoreAllPersistentBundleStates() throws RepositoryException, JSONException, IOException {

        PostResult response = post(getBaseServerURL() + Jahia.getContextPath() + "/modules/api/bundles/_storeAllLocalPersistentStates");

        Assert.assertEquals(200, response.getStatusCode());
        JSONArray bubdleInfosApi = new JSONArray(response.getResponseBody());

        HashMap<String, JSONObject> bundleInfoByLocationApi = new HashMap<>(bubdleInfosApi.length());
        for (int i = 0; i < bubdleInfosApi.length(); i++) {
            JSONObject bundleInfoApi = bubdleInfosApi.getJSONObject(i);
            bundleInfoByLocationApi.put(bundleInfoApi.getString(LOCATION), bundleInfoApi);
        }

        Bundle[] bundlesOsgi = FrameworkService.getBundleContext().getBundles();
        Assert.assertEquals(bundleInfoByLocationApi.size(), bundlesOsgi.length);
        for (Bundle bundleOsgi : bundlesOsgi) {
            JSONObject bundleInfoApi = bundleInfoByLocationApi.get(bundleOsgi.getLocation());
            Assert.assertEquals(bundleInfoApi.getString(LOCATION), bundleOsgi.getLocation());
            Assert.assertEquals(bundleInfoApi.getInt(STATE), BundleUtils.getPersistentState(bundleOsgi));
            Assert.assertEquals(bundleInfoApi.getString(SYMBOLIC_NAME), bundleOsgi.getSymbolicName());
            Assert.assertEquals(bundleInfoApi.getString(VERSION), BundleUtils.getModuleVersion(bundleOsgi));
        }

        String bundleInfosJson = JCRTemplate.getInstance().doExecuteWithSystemSession(
            session -> session.getNode(BundleInfoJcrHelper.PATH_MODULE_MANAGEMENT).getPropertyAsString(BundleInfoJcrHelper.PROP_BUNDLES_PERSISTENT_STATE)
        );
        JSONArray bundleInfosJcr = new JSONArray(bundleInfosJson);
        Assert.assertEquals(bundleInfoByLocationApi.size(), bundleInfosJcr.length());
        for (int i = 0; i < bundleInfosJcr.length(); i++) {
            JSONObject bundleInfoJcr = bundleInfosJcr.getJSONObject(i);
            JSONObject bundleInfoApi = bundleInfoByLocationApi.get(bundleInfoJcr.getString(LOCATION));
            Assert.assertEquals(bundleInfoApi.getString(LOCATION), bundleInfoJcr.getString(LOCATION));
            Assert.assertEquals(bundleInfoApi.getInt(STATE), bundleInfoJcr.getInt(STATE));
            Assert.assertEquals(bundleInfoApi.getString(SYMBOLIC_NAME), bundleInfoJcr.getString(SYMBOLIC_NAME));
            Assert.assertEquals(bundleInfoApi.getString(VERSION), bundleInfoJcr.getString(VERSION));
        }
    }

    private void verifyModuleInfoRetrieval(String bundleKey) throws JSONException {
        JSONObject response = getBundleInfo(bundleKey);
        verifyModuleInfo(response);
    }

    private void verifyModuleInfosRetrieval(String... bundleKeys) throws JSONException {
        JSONObject response = getBundleInfo(getMultipleBundlesSelector(bundleKeys));
        Assert.assertEquals(bundleKeys.length, response.length());
        for (String bundleKey : bundleKeys) {
            verifyModuleInfo(response.getJSONObject(bundleKey));
        }
    }

    private void verifyBucketModuleInfoRetrieval(String bundleBucketKey, String... expectedBundleKeys) throws JSONException {
        JSONObject response = getBundleInfo(bundleBucketKey + "/*");
        Assert.assertEquals(expectedBundleKeys.length, response.length());
        for (String expectedBundleKey : expectedBundleKeys) {
            verifyModuleInfo(response.getJSONObject(expectedBundleKey));
        }
    }

    private void verifyModuleInfoRetrievalError(String bundleSelector, int expectedResponseCode) {
        getAsText(getUrl(bundleSelector), expectedResponseCode);
    }

    private static void verifyModuleInfo(JSONObject moduleInfo) throws JSONException {
        Assert.assertEquals("MODULE", moduleInfo.getString("type"));
        Assert.assertEquals("ACTIVE", moduleInfo.getString("osgiState"));
        Assert.assertEquals("STARTED", moduleInfo.getString("moduleState"));
    }

    private static String getModuleManagerKey(String moduleName) {
        return (moduleName + '/' + moduleManagerVersion);
    }

    private static String getMultipleBundlesSelector(String... bundleKeys) {
        return ("%5B" + StringUtils.join(bundleKeys, ',') + "%5D");
    }

    private JSONObject getBundleInfo(String bundleSelector) throws JSONException {
        return new JSONObject(getAsText(getUrl(bundleSelector)));
    }

    private static String getUrl(String bundleSelector) {
        return "/modules/api/bundles/" + bundleSelector + "/_localInfo";
    }

    private static String getModuleManagerVersion() {
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

    private static String getBlueprintExtenderConfigSelector() {
        Bundle foundBundle = null;
        String symbolicName = "org.jahia.bundles.blueprint.extender.config";
        for (Bundle bundle : FrameworkService.getBundleContext().getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                foundBundle = bundle;
                break;
            }
        }
        if (foundBundle == null) {
            throw new IllegalStateException("No " + symbolicName + " bundle installed");
        }
        return symbolicName + "/" + foundBundle.getVersion();
    }
}
