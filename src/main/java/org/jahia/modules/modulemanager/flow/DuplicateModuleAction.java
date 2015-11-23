/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
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
            String newModuleStudioUrl = "/cms/studio/default/" + resource.getLocale() + "/modules/" + newModule.getId() + ".html";
            JSONObject json = new JSONObject();
            json.put("newModuleStudioUrl", newModuleStudioUrl);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        } catch (ScmUnavailableModuleIdException e) {
            String message = Messages.getWithArgs("resources.JahiaServerSettings",
                    "serverSettings.manageModules.duplicateModuleError.moduleExists", resource.getLocale(), newModuleName);
            JSONObject json = new JSONObject();
            json.put("error", message);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        } catch (ScmWrongVersionException e) {
            String message = Messages.get("resources.JahiaServerSettings",
                    "serverSettings.manageModules.downloadSourcesError.wrongVersion", resource.getLocale());
            JSONObject json = new JSONObject();
            json.put("error", message);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        } catch (SourceControlException e) {
            String message = Messages.getWithArgs("resources.JahiaServerSettings",
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
