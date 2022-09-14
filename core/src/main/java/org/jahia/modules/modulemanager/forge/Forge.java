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

import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.notification.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.validation.ValidationContext;

import java.io.Serializable;

public class Forge implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Forge.class);

    private static final long serialVersionUID = 2031426003900898977L;
    String url;
    String user;
    String password;
    String id;
    String actionType;

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void validateView(ValidationContext context) {
        if (!StringUtils.equals((String) context.getUserValue("actionType"),"delete")) {
            // try basic http connexion
            HttpGet httpMethod = new HttpGet(url + "/contents/modules-repository.moduleList.json");
            httpMethod.addHeader("Authorization", "Basic " + Base64.encode((user + ":" + password).getBytes()));
            CloseableHttpClient httpClient = ((HttpClientService) SpringContextSingleton.getBean("HttpClientService")).getHttpClient(url);
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpMethod)) {
                if (httpResponse.getCode() != 200) {
                    context.getMessageContext().addMessage(new MessageBuilder()
                            .error()
                            .source("testUrl")
                            .code("serverSettings.manageForges.error.cannotVerify").arg(httpResponse.getCode())
                            .build());
                }
            } catch (Exception e) {
                context.getMessageContext().addMessage(new MessageBuilder()
                        .error()
                        .source("testUrl")
                        .code("serverSettings.manageForges.error.httpError").arg(e.getMessage())
                        .build());
                logger.error(e.getMessage(), e);
            }
        }
    }
}
