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
package org.jahia.modules.modulemanager.rest;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.jahia.modules.modulemanager.rest.filters.HeadersResponseFilter;
import org.jahia.modules.modulemanager.rest.filters.ModuleManagerAuthenticationRequestFilter;

/**
 * Application configuration component.
 *
 * @author bdjiba
 */
public class ModuleManagerApplicationConfig extends ResourceConfig {

    /**
     * Initializes an instance of this class providing a list of classes to be registered.
     */
    public ModuleManagerApplicationConfig() {
        super(
                MultiPartFeature.class,
                ModuleManagerResource.class,
                JacksonJaxbJsonProvider.class,
                ModuleManagerExceptionMapper.class,
                ModuleManagerAuthenticationRequestFilter.class,
                HeadersResponseFilter.class
        );
    }
}
