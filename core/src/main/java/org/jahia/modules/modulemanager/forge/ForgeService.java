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

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.bin.Jahia;
import org.jahia.commons.Version;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.notification.HttpClientService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service to manage Forges
 */

public class ForgeService {

    private static final Logger logger = LoggerFactory.getLogger(ForgeService.class);

    private HttpClientService httpClientService;
    private Set<Forge> forges = new HashSet<Forge>();
    private List<Module> modules = new ArrayList<Module>();
    private long loadModulesDelay;
    private long lastModulesLoad = new Date().getTime();
    private boolean flushModules = true;

    public ForgeService() {
        loadForges();
    }

    public Set<Forge> getForges() {
        return forges;
    }

    public List<Module> getModules() {
        return modules;
    }

    public void addForge(Forge forge) {
        for (Forge f : forges) {
            if (StringUtils.equals(forge.getId(), f.getId())) {
                f.setUser(forge.getUser());
                f.setUrl(forge.getUrl());
                f.setPassword(forge.getPassword());
                return;
            }
        }
        forges.add(forge);
    }

    public void removeForge(Forge forge) {
        for (Forge f : forges) {
            if (StringUtils.equals(forge.getId(), f.getId())) {
                forges.remove(f);
                return;
            }
        }
    }

    public void loadForges() {
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    if (session.itemExists("/settings/forgesSettings")) {
                        Node forgesRoot = session.getNode("/settings/forgesSettings");
                        if (forgesRoot != null) {
                            NodeIterator ni = forgesRoot.getNodes();
                            while (ni.hasNext()) {
                                Node n = ni.nextNode();
                                if (!n.isNodeType("jnt:forgeServerSettings")) {
                                    continue;
                                }
                                Forge f = new Forge();
                                f.setId(n.getIdentifier());
                                f.setUrl(n.getProperty("j:url").getString());
                                f.setUser(n.getProperty("j:user").getString());
                                f.setPassword(n.getProperty(JCRUserNode.J_PASSWORD).getString());
                                forges.add(f);
                            }
                        }
                    }
                    return null;
                }
            });

        } catch (RepositoryException e) {
            logger.error(e.getMessage(),e);
        }
    }

    public void saveForges() {
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {

                    Node forgesRoot;

                    // delete all previous nodes
                    try {
                        forgesRoot = session.getNode("/settings/forgesSettings");
                        forgesRoot.remove();
                    } catch (PathNotFoundException e) {
                        // do nothing
                    }

                    if (!session.getNode("/").hasNode("settings")) {
                        session.getNode("/").addNode("settings", "jnt:globalSettings");
                        session.save();
                    }
                    if (!session.getNode("/settings").hasNode("forgesSettings")) {
                        JCRNodeWrapper forgesNode = session.getNode("/settings").addNode("forgesSettings", "jnt:forgesServerSettings");
                        forgesNode.setAclInheritanceBreak(true);
                        session.save();
                    }

                    forgesRoot = session.getNode("/settings/forgesSettings");
                    // write all forges
                    for (Forge forge : forges) {
                        Node forgeNode = forgesRoot.addNode(JCRContentUtils.generateNodeName(forge.getUrl()), "jnt:forgeServerSettings");
                        forgeNode.setProperty("j:url", forge.getUrl());
                        forgeNode.setProperty("j:user", forge.getUser());
                        forgeNode.setProperty(JCRUserNode.J_PASSWORD, forge.getPassword());
                        forge.setId(forgeNode.getIdentifier());
                    }
                    session.save();
                    return null;
                }
            });
        } catch (RepositoryException e) {
            logger.error(e.getMessage(),e);
        }
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
        if(flushModules || (lastModulesLoad + loadModulesDelay) < new Date().getTime()){
            modules.clear();
            for (Forge forge : forges) {
                String url = forge.getUrl() + "/contents/modules-repository.moduleList.json";
                Map<String, String> headers = new HashMap<String, String>();
                if (!StringUtils.isEmpty(forge.getUser())) {
                    headers.put("Authorization", "Basic " + Base64.encode((forge.getUser() + ":" + forge.getPassword()).getBytes()));
                }
                headers.put("accept", "application/json");

                String jsonModuleList = httpClientService.executeGet(url, headers);
                try {
                    JSONArray modulesRoot = new JSONArray(jsonModuleList);

                    JSONArray moduleList = modulesRoot.getJSONObject(0).getJSONArray("modules");
                    for (int i = 0; i < moduleList.length(); i++) {
                        boolean add = true;

                        final JSONObject moduleObject = moduleList.getJSONObject(i);
                        for (Module m : modules) {
                            if (StringUtils.equals(m.getId(), moduleObject.getString("name")) && StringUtils.equals(m.getGroupId(), moduleObject.getString("groupId"))) {
                                add = false;
                                break;
                            }
                        }
                        if (add) {
                            final JSONArray moduleVersions = moduleObject.getJSONArray("versions");

                            SortedMap<Version, JSONObject> sortedVersions = new TreeMap<Version, JSONObject>();

                            final Version jahiaVersion = new Version(Jahia.VERSION);

                            for (int j = 0; j < moduleVersions.length(); j++) {
                                JSONObject object = moduleVersions.getJSONObject(j);
                                Version version = new Version(object.getString("version"));
                                Version requiredVersion = new Version(StringUtils.substringAfter(object.getString("requiredVersion"), "version-"));
                                if (requiredVersion.compareTo(jahiaVersion) <= 0 && requiredVersion.getMajorVersion() == jahiaVersion.getMajorVersion()) {
                                    sortedVersions.put(version, object);
                                }
                            }
                            if (!sortedVersions.isEmpty()) {
                                Module module = new Module();
                                JSONObject versionObject = sortedVersions.get(sortedVersions.lastKey());
                                module.setRemoteUrl(moduleObject.getString("remoteUrl"));
                                module.setRemotePath(moduleObject.getString("path"));
                                if (moduleObject.has("icon")) {
                                    module.setIcon(moduleObject.getString("icon"));
                                }
                                module.setVersion(versionObject.getString("version"));
                                module.setName(moduleObject.getString("title"));
                                module.setId(moduleObject.getString("name"));
                                module.setGroupId(moduleObject.getString("groupId"));
                                module.setDownloadUrl(versionObject.getString("downloadUrl"));
                                if (moduleObject.has("status")) {
                                    module.setStatus(moduleObject.getString("status"));
                                }else {
                                    module.setStatus("unknown");
                                }
                                module.setForgeId(forge.getId());
                                modules.add(module);
                            }
                        }
                    }
                } catch (JSONException e) {
                    logger.error("unable to parse JSON return string for " + url, e);
                } catch (Exception e) {
                    logger.error("unable to get store information" + e.getMessage(), e);
                }
            }
            Collections.sort(modules);
            lastModulesLoad = new Date().getTime();
            flushModules = false;
        }

        return modules;
    }

    public long getLastUpdateTime(){
        return lastModulesLoad;
    }

    public void flushModules(){
        flushModules = true;
    }

    public File downloadModuleFromForge(String forgeId, String url) {
        for (Forge forge : forges) {
            if (forgeId.equals(forge.getId())) {
                HttpGet httpMethod = new HttpGet(UriComponentsBuilder.fromHttpUrl(url).build(false).toUri());
                httpMethod.addHeader("Authorization", "Basic " + Base64.encode((forge.getUser() + ":" + forge.getPassword()).getBytes()));
                CloseableHttpClient httpClient = httpClientService.getHttpClient(url);
                try (CloseableHttpResponse httpResponse = httpClient.execute(httpMethod)) {
                    if (httpResponse.getCode() == HttpServletResponse.SC_OK) {
                        File f = File.createTempFile("module", "." + StringUtils.substringAfterLast(url, "."));
                        FileUtils.copyInputStreamToFile(httpResponse.getEntity().getContent(), f);
                        return f;
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        return null;
    }

    public void setHttpClientService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    public void setLoadModulesDelay(long loadModulesDelay) {
        this.loadModulesDelay = loadModulesDelay;
    }
}
