package org.jahia.modules.modulemanager.forge;

import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.modules.modulemanager.util.ConfigUtil;
import org.jahia.services.notification.HttpClientService;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;

public class ForgeConfig {
    private static final Logger logger = LoggerFactory.getLogger(ForgeConfig.class);
    private String pid = null;
    private String url = null;
    private String user = null;
    private String password = null;
    private HttpClientService httpClientService;

    public ForgeConfig() {
    }

    public static ForgeConfig build(String pid, Dictionary<String, ?> properties) {
        final ForgeConfig forgeConfig = new ForgeConfig();
        forgeConfig.setPid(pid);
        if (properties != null) {
            final Map<String, String> filteredProperties = ConfigUtil.getMap(properties);
            forgeConfig.setUrl(filteredProperties.getOrDefault("url", null));
            forgeConfig.setUser(filteredProperties.getOrDefault("user", null));
            forgeConfig.setPassword(filteredProperties.getOrDefault("password", null));
        }
        return forgeConfig;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void validate(HttpClientService httpClientService) {
        // try basic http connexion
        final HttpGet httpMethod = new HttpGet(url + "/contents/modules-repository.moduleList.json");
        if (!StringUtils.isNotEmpty(user) && !StringUtils.isNotEmpty(password)) {
            httpMethod.addHeader("Authorization", "Basic " + Base64.encode((user + ":" + password).getBytes()));
        }
        final CloseableHttpClient httpClient = httpClientService.getHttpClient(url);
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpMethod)) {
            // TODO: change from info to debug
            logger.info("Success reaching forge URL {}", url);
        } catch (Exception ex) {
            logger.error("Failure reaching forge URL {}", url, ex);
        }
    }

}
