/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-20202 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.test.services.modulemanager;

import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Jahia;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.modulemanager.persistence.jcr.BundleInfoJcrHelper;
import org.jahia.test.JahiaTestCase;
import org.json.JSONArray;
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

    private static String moduleManagerVersion;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        moduleManagerVersion = getModuleManagerVersion();
    }

    @Before
    public void setUp() throws Exception {
        loginRoot();
    }

    @After
    public void tearDown() throws Exception {
        logout();
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
    public void shouldRetrieveAllModuleInfosByGroup() throws Exception {
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

    @Test
    public void shouldHaveFragmentType() throws Exception {
        JSONObject moduleInfo = getBundleInfo(getBlueprintExtenderConfigSelector());
        Assert.assertEquals("FRAGMENT", moduleInfo.getString("type"));
        Assert.assertEquals("RESOLVED", moduleInfo.getString("osgiState"));
    }

    @Test
    public void shouldStoreAllPersistentBundleStates() throws Exception {

        PostResult response = post(getBaseServerURL() + Jahia.getContextPath() + "/modules/api/bundles/_storeAllLocalPersistentStates");

        Assert.assertEquals(200, response.statusCode);
        JSONArray bubdleInfosApi = new JSONArray(response.responseBody);

        HashMap<String, JSONObject> bundleInfoByLocationApi = new HashMap<>(bubdleInfosApi.length());
        for (int i = 0; i < bubdleInfosApi.length(); i++) {
            JSONObject bundleInfoApi = bubdleInfosApi.getJSONObject(i);
            bundleInfoByLocationApi.put(bundleInfoApi.getString("location"), bundleInfoApi);
        }

        Bundle[] bundlesOsgi = FrameworkService.getBundleContext().getBundles();
        Assert.assertEquals(bundleInfoByLocationApi.size(), bundlesOsgi.length);
        for (Bundle bundleOsgi : bundlesOsgi) {
            JSONObject bundleInfoApi = bundleInfoByLocationApi.get(bundleOsgi.getLocation());
            Assert.assertEquals(bundleInfoApi.getString("location"), bundleOsgi.getLocation());
            Assert.assertEquals(bundleInfoApi.getInt("state"), BundleUtils.getPersistentState(bundleOsgi));
            Assert.assertEquals(bundleInfoApi.getString("symbolicName"), bundleOsgi.getSymbolicName());
            Assert.assertEquals(bundleInfoApi.getString("version"), BundleUtils.getModuleVersion(bundleOsgi));
        }

        String bundleInfosJson = JCRTemplate.getInstance().doExecuteWithSystemSession(
            session -> session.getNode(BundleInfoJcrHelper.PATH_MODULE_MANAGEMENT).getPropertyAsString(BundleInfoJcrHelper.PROP_BUNDLES_PERSISTENT_STATE)
        );
        JSONArray bundleInfosJcr = new JSONArray(bundleInfosJson);
        Assert.assertEquals(bundleInfoByLocationApi.size(), bundleInfosJcr.length());
        for (int i = 0; i < bundleInfosJcr.length(); i++) {
            JSONObject bundleInfoJcr = bundleInfosJcr.getJSONObject(i);
            JSONObject bundleInfoApi = bundleInfoByLocationApi.get(bundleInfoJcr.getString("location"));
            Assert.assertEquals(bundleInfoApi.getString("location"), bundleInfoJcr.getString("location"));
            Assert.assertEquals(bundleInfoApi.getInt("state"), bundleInfoJcr.getInt("state"));
            Assert.assertEquals(bundleInfoApi.getString("symbolicName"), bundleInfoJcr.getString("symbolicName"));
            Assert.assertEquals(bundleInfoApi.getString("version"), bundleInfoJcr.getString("version"));
        }
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
        getAsText(getUrl(bundleSelector), expectedResponseCode);
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
        return new JSONObject(getAsText(getUrl(bundleSelector)));
    }

    private static String getUrl(String bundleSelector) {
        return "/modules/api/bundles/" + bundleSelector + "/_localInfo";
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

    private static String getBlueprintExtenderConfigSelector() throws Exception {
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
