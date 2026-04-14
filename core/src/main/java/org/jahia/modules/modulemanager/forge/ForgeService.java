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
package org.jahia.modules.modulemanager.forge;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.bin.Jahia;
import org.jahia.commons.Version;
import org.jahia.services.notification.HttpClientService;
import org.jahia.settings.SettingsBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service to manage Forges
 */

@Component(service = {ForgeService.class}, immediate = true)
public class ForgeService {

    private static final Logger logger = LoggerFactory.getLogger(ForgeService.class);
    private final List<Module> modules = new ArrayList<Module>();
    private final long loadModulesDelay;
    private HttpClientService httpClientService;
    private long lastModulesLoad = new Date().getTime();
    private boolean flushModules = true;
    private ForgeConfigFactory forgeConfigFactory;

    public ForgeService() {
        loadModulesDelay = SettingsBean.getInstance().getLong("jahia.settings.forgeModulesUpdateDelay", 86400000L);
    }

    public Collection<ForgeConfig> getForgeConfigs() {
        return forgeConfigFactory.getConfigs();
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module findModule(String name, String groupId) {
        for (Module m : modules) {
            if (StringUtils.equals(name, m.getId()) && m.getGroupId().equals(groupId)) {
                return m;
            }
        }
        return null;
    }

    public List<Module> loadModules() {
        boolean expired = (lastModulesLoad + loadModulesDelay) < new Date().getTime();
        logger.debug("Start to load modules, flushModules: {}, expired: {}", flushModules, expired);
        if (flushModules || expired) {
            modules.clear();
            for (ForgeConfig forgeConfig : getForgeConfigs()) {
                final String url = forgeConfig.getUrl() + ForgeConstants.MODULE_LIST_JSON_PATH;
                logger.debug("Start retrieving module list from {}", url);
                final Map<String, String> headers = new HashMap<String, String>();
                final String user = forgeConfig.getUser();
                final String password = forgeConfig.getPassword();
                if (!StringUtils.isNotEmpty(user) && !StringUtils.isNotEmpty(password)) {
                    headers.put(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encode((user + ":" + password).getBytes(StandardCharsets.UTF_8)));
                }
                headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

                final String jsonModuleList = httpClientService.executeGet(url, headers);
                try {
                    final JSONArray modulesRoot = new JSONArray(jsonModuleList);
                    final JSONArray moduleList = modulesRoot.getJSONObject(0).getJSONArray(ForgeConstants.JSON_ARRAY_MODULES);
                    for (int i = 0; i < moduleList.length(); i++) {
                        boolean add = true;

                        final JSONObject moduleObject = moduleList.getJSONObject(i);
                        for (Module m : modules) {
                            if (StringUtils.equals(m.getId(), moduleObject.getString(ForgeConstants.JSON_ATT_NAME)) && StringUtils.equals(m.getGroupId(), moduleObject.getString(ForgeConstants.JSON_ATT_GROUP_ID))) {
                                add = false;
                                break;
                            }
                        }
                        if (add) {
                            final JSONArray moduleVersions = moduleObject.getJSONArray(ForgeConstants.JSON_ARRAY_VERSIONS);
                            final SortedMap<Version, JSONObject> sortedVersions = new TreeMap<Version, JSONObject>();
                            final Version jahiaVersion = new Version(Jahia.VERSION);

                            for (int j = 0; j < moduleVersions.length(); j++) {
                                final JSONObject object = moduleVersions.getJSONObject(j);
                                final Version version = new Version(object.getString(ForgeConstants.JSON_ATT_VERSION));
                                final Version requiredVersion = new Version(StringUtils.substringAfter(object.getString(ForgeConstants.JSON_ATT_REQUIRED_VERSION), "version-"));
                                if (requiredVersion.compareTo(jahiaVersion) <= 0 && requiredVersion.getMajorVersion() == jahiaVersion.getMajorVersion()) {
                                    sortedVersions.put(version, object);
                                }
                            }
                            if (!sortedVersions.isEmpty()) {
                                final Module module = new Module();
                                final JSONObject versionObject = sortedVersions.get(sortedVersions.lastKey());
                                module.setRemoteUrl(moduleObject.getString(ForgeConstants.JSON_ATT_REMOTE_URL));
                                module.setRemotePath(moduleObject.getString(ForgeConstants.JSON_ATT_PATH));
                                if (moduleObject.has(ForgeConstants.JSON_ATT_ICON)) {
                                    module.setIcon(moduleObject.getString(ForgeConstants.JSON_ATT_ICON));
                                }
                                module.setVersion(versionObject.getString(ForgeConstants.JSON_ATT_VERSION));
                                module.setName(moduleObject.getString(ForgeConstants.JSON_ATT_TITLE));
                                module.setId(moduleObject.getString(ForgeConstants.JSON_ATT_NAME));
                                module.setGroupId(moduleObject.getString(ForgeConstants.JSON_ATT_GROUP_ID));
                                module.setDownloadUrl(versionObject.getString(ForgeConstants.JSON_ATT_DOWNLOAD_URL));
                                module.setForgeId(forgeConfig.getPid());
                                modules.add(module);
                            }
                        }
                    }
                } catch (JSONException ex) {
                    logger.error("Unable to parse JSON return string for {}", url, ex);
                } catch (Exception ex) {
                    logger.error("Unable to get store information", ex);
                }
                logger.debug("End retrieving module list from {}", url);
            }
            Collections.sort(modules);
            lastModulesLoad = new Date().getTime();
            flushModules = false;
        }
        logger.debug("End to load modules");
        return modules;
    }

    public long getLastUpdateTime() {
        return lastModulesLoad;
    }

    public void flushModules() {
        flushModules = true;
    }

    public File downloadModuleFromForge(String forgeId, String url) {
        for (ForgeConfig forgeConfig : getForgeConfigs()) {
            if (forgeId.equals(forgeConfig.getPid())) {
                final HttpGet httpMethod = new HttpGet(UriComponentsBuilder.fromHttpUrl(url).build(false).toUri());
                final String user = forgeConfig.getUser();
                final String password = forgeConfig.getPassword();
                if (!StringUtils.isNotEmpty(user) && !StringUtils.isNotEmpty(password)) {
                    httpMethod.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encode((user + ":" + password).getBytes(StandardCharsets.UTF_8)));
                }
                final CloseableHttpClient httpClient = httpClientService.getHttpClient(url);
                try (CloseableHttpResponse httpResponse = httpClient.execute(httpMethod)) {
                    if (httpResponse.getCode() == HttpServletResponse.SC_OK) {
                        final File f = File.createTempFile("module", "." + StringUtils.substringAfterLast(url, "."));
                        FileUtils.copyInputStreamToFile(httpResponse.getEntity().getContent(), f);
                        return f;
                    }
                } catch (IOException ex) {
                    logger.error("Impossible to download module {}", url, ex);
                }
            }
        }
        return null;
    }

    @Reference
    public void setHttpClientService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    @Reference(service = ForgeConfigFactory.class)
    public void setForgeConfigFactory(ForgeConfigFactory forgeConfigFactory) {
        this.forgeConfigFactory = forgeConfigFactory;
    }
}
