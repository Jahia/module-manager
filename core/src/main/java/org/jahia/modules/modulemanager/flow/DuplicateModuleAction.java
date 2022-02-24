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
package org.jahia.modules.modulemanager.flow;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.templates.ScmUnavailableModuleIdException;
import org.jahia.services.templates.ScmWrongVersionException;
import org.jahia.services.templates.SourceControlException;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;
import org.osgi.framework.BundleException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public class DuplicateModuleAction extends Action {

    private JahiaTemplateManagerService jahiaTemplateManagerService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        String newModuleName = getParameter(parameters, "newModuleName");
        String newModuleId = getParameter(parameters, "newModuleId");
        String newGroupId = getParameter(parameters, "newGroupId");
        String newDstPath = getParameter(parameters, "newDstPath");

        String srcPath = getParameter(parameters, "srcPath");
        String newScmUri = getParameter(parameters, "newScmUri");
        String branchOrTag = getParameter(parameters, "branchOrTag");
        String moduleId = getParameter(parameters, "moduleId");
        String version = getParameter(parameters, "version");
        boolean containsTypeDefinitions = Boolean.valueOf(getParameter(parameters, "containsTypeDefinitions", "false"));
        boolean areSourcesTemporary = Boolean.valueOf(getParameter(parameters, "areSourcesTemporary", "false"));

        try {
            JahiaTemplatesPackage newModule = jahiaTemplateManagerService.duplicateModule(newModuleName, newModuleId, newGroupId, srcPath, newScmUri, branchOrTag, moduleId, version, containsTypeDefinitions, newDstPath, areSourcesTemporary, session);
            String contextPath = renderContext.getRequest().getContextPath();
            String newModuleStudioUrl = (StringUtils.equals(contextPath, "/") ? "" : contextPath) + "/cms/studio/default/" + resource.getLocale() +
                    "/modules/" + newModule.getId() + ".html";
            JSONObject json = new JSONObject();
            json.put("newModuleStudioUrl", newModuleStudioUrl);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        } catch (ScmUnavailableModuleIdException e) {
            String message = Messages.getWithArgs("resources.ModuleManager",
                    "serverSettings.manageModules.duplicateModuleError.moduleExists", resource.getLocale(), newModuleName);
            JSONObject json = new JSONObject();
            json.put("error", message);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        } catch (ScmWrongVersionException e) {
            String message = Messages.get("resources.ModuleManager",
                    "serverSettings.manageModules.downloadSourcesError.wrongVersion", resource.getLocale());
            JSONObject json = new JSONObject();
            json.put("error", message);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        } catch (SourceControlException e) {
            String message = Messages.getWithArgs("resources.ModuleManager",
                    "serverSettings.manageModules.downloadSourcesError", resource.getLocale(), version);
            JSONObject json = new JSONObject();
            json.put("error", message);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        } catch (Exception e) {
            String message = e.getLocalizedMessage();
            if (StringUtils.isBlank(message)) {
                message = e.toString();
            }
            JSONObject json = new JSONObject();
            json.put(e instanceof BundleException ? "bundleError" : "error", message);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        }
    }

    public void setJahiaTemplateManagerService(JahiaTemplateManagerService jahiaTemplateManagerService) {
        this.jahiaTemplateManagerService = jahiaTemplateManagerService;
    }
}
