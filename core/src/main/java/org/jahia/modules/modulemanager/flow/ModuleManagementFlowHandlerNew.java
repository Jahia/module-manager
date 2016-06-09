/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.modulemanager.flow;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeIterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.dom4j.DocumentException;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.data.templates.ModuleState;
import org.jahia.exceptions.JahiaException;
import org.jahia.modules.modulemanager.forge.ForgeService;
import org.jahia.modules.modulemanager.forge.Module;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.modulemanager.BundleInfo;
import org.jahia.services.modulemanager.ModuleManager;
import org.jahia.services.modulemanager.ModuleNotFoundException;
import org.jahia.services.modulemanager.OperationResult;
import org.jahia.services.render.RenderContext;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.templates.ModuleVersion;
import org.jahia.services.templates.ScmUnavailableModuleIdException;
import org.jahia.services.templates.ScmWrongVersionException;
import org.jahia.services.templates.SourceControlException;
import org.jahia.services.templates.TemplatePackageRegistry;
import org.jahia.utils.i18n.Messages;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.RequestContext;

/**
 * WebFlow handler for managing modules.
 *
 * @author rincevent
 */
public class ModuleManagementFlowHandlerNew implements Serializable {

    private static final long serialVersionUID = -4195379181264451784L;
    private static Logger logger = LoggerFactory.getLogger(ModuleManagementFlowHandlerNew.class);

    @Autowired
    private transient JahiaTemplateManagerService templateManagerService;

    @Autowired
    private transient JahiaSitesService sitesService;

    @Autowired
    private transient ForgeService forgeService;

    @Autowired
    private transient ModuleManager moduleManager;

    @Autowired
    private transient TemplatePackageRegistry templatePackageRegistry;

    private String moduleName;

    public boolean isInModule(RenderContext renderContext) {
        try {
            if (renderContext.getMainResource().getNode().isNodeType("jnt:module")) {
                moduleName = renderContext.getMainResource().getNode().getName();
                return true;
            }
        } catch (RepositoryException e) {
        }
        return false;
    }

    public boolean isStudio(RenderContext renderContext) {
        return renderContext.getEditModeConfigName().equals("studiomode") || renderContext.getEditModeConfigName().equals("studiovisualmode");
    }

    public ModuleFile initModuleFile() {
        return new ModuleFile();
    }

    public boolean installModule(String forgeId, String url, MessageContext context) {
        File file = null;
        try {
            file = forgeService.downloadModuleFromForge(forgeId, url);
            moduleManager.install(new FileSystemResource(file), null);
            return true;

        } catch (Exception e) {
            context.addMessage(new MessageBuilder().source("moduleFile")
                    .code("serverSettings.manageModules.install.failed")
                    .arg(e.getMessage())
                    .error()
                    .build());
            logger.error(e.getMessage(), e);
        } finally {
            FileUtils.deleteQuietly(file);
        }
        return false;
    }

    public boolean uploadModule(ModuleFile moduleFile, MessageContext context, boolean forceUpdate) {

        String originalFilename = moduleFile.getModuleFile().getOriginalFilename();
        if (!FilenameUtils.isExtension(StringUtils.lowerCase(originalFilename), "jar")) {
            context.addMessage(new MessageBuilder().error().source("moduleFile")
                    .code("serverSettings.manageModules.install.wrongFormat").build());
            return false;
        }
        File file = null;
        try {
            file = File.createTempFile("module-", "." + StringUtils.substringAfterLast(originalFilename, "."));
            moduleFile.getModuleFile().transferTo(file);
            JarFile jarFile = new JarFile(file);
            Manifest manifest = jarFile.getManifest();
            String symbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
            if (symbolicName == null) {
                symbolicName = manifest.getMainAttributes().getValue("root-folder");
            }
            String version = manifest.getMainAttributes().getValue("Implementation-Version");
            String groupId = manifest.getMainAttributes().getValue("Jahia-GroupId");
            if (templateManagerService.differentModuleWithSameIdExists(symbolicName, groupId)) {
                context.addMessage(new MessageBuilder().source("moduleFile")
                        .code("serverSettings.manageModules.install.moduleWithSameIdExists")
                        .arg(symbolicName)
                        .error()
                        .build());
                return false;
            }
            if (!forceUpdate) {
                Set<ModuleVersion> moduleVerions = templatePackageRegistry.getAvailableVersionsForModule(symbolicName);
                ModuleVersion moduleVersion = new ModuleVersion(version);
                if (!moduleVersion.isSnapshot() && moduleVerions.contains(moduleVersion)) {
                    context.addMessage(new MessageBuilder().source("moduleExists")
                            .code("serverSettings.manageModules.install.moduleExists")
                            .args(new String[]{symbolicName, version})
                            .build());
                    return false;
                }
            }
            OperationResult result = moduleManager.install(new FileSystemResource(file), null);
            if (result!= null && result.getBundleInfos()!= null && result.getBundleInfos().iterator().hasNext()) {
                BundleInfo info = result.getBundleInfos().iterator().next();
                context.addMessage(new MessageBuilder().source("moduleFile")
                        .code("serverSettings.manageModules.install.uploaded")
                        .args(new String[]{info.getSymbolicName(), info.getVersion().toString()})
                        .build());
            }
            return true;

        } catch (Exception e) {
            context.addMessage(new MessageBuilder().source("moduleFile")
                    .code("serverSettings.manageModules.install.failed")
                    .arg(e.getMessage())
                    .error()
                    .build());
            logger.error(e.getMessage(), e);
        } finally {
            FileUtils.deleteQuietly(file);
        }
        return false;
    }

    private void createMessageForMissingDependencies(MessageContext context, List<String> missingDeps) {
        logger.warn("Missing dependencies : " + missingDeps);
        context.addMessage(new MessageBuilder().source("moduleFile")
                .code("serverSettings.manageModules.install.missingDependencies")
                .arg(StringUtils.join(missingDeps, ","))
                .error()
                .build());
    }

    private List<String> getMissingDependenciesFrom(List<String> deps) {
        List<String> missingDeps = new ArrayList<String>(deps.size());
        for (String dep : deps) {
            if (templateManagerService.getTemplatePackageById(dep) == null && templateManagerService.getTemplatePackage(dep) == null) {
                missingDeps.add(dep);
            }
        }
        return missingDeps;
    }

    public void loadModuleInformation(RequestContext context) {
        String selectedModuleName = moduleName != null ? moduleName : (String) context.getFlowScope().get("selectedModule");
        Map<ModuleVersion, JahiaTemplatesPackage> selectedModule = templateManagerService.getTemplatePackageRegistry().getAllModuleVersions().get(selectedModuleName);
        if (selectedModule != null) {
            if (selectedModule.size() > 1) {
                boolean foundActiveVersion = false;
                for (Map.Entry<ModuleVersion, JahiaTemplatesPackage> entry : selectedModule.entrySet()) {
                    JahiaTemplatesPackage value = entry.getValue();
                    if (value.isActiveVersion()) {
                        foundActiveVersion = true;
                        populateActiveVersion(context, value);
                    }
                }
                if (!foundActiveVersion) {
                    // there is no active version take information from most recent installed version
                    LinkedList<ModuleVersion> sortedVersions = new LinkedList<ModuleVersion>(selectedModule.keySet());
                    Collections.sort(sortedVersions);
                    populateActiveVersion(context, selectedModule.get(sortedVersions.getFirst()));
                }
            } else {
                populateActiveVersion(context, selectedModule.values().iterator().next());
            }
            context.getRequestScope().put("otherVersions", selectedModule);
        } else {
            // module is not yet parsed probably because it depends on unavailable modules so look for it in module states
            final Map<Bundle, ModuleState> moduleStates = templateManagerService.getModuleStates();
            for (Bundle bundle : moduleStates.keySet()) {
                JahiaTemplatesPackage module = BundleUtils.getModule(bundle);
                if (module.getId().equals(selectedModuleName)) {
                    populateActiveVersion(context, module);
                    final List<String> missing = getMissingDependenciesFrom(module.getDepends());
                    if(!missing.isEmpty()) {
                        createMessageForMissingDependencies(context.getMessageContext(), missing);
                    }
                    break;
                }

            }
        }

        populateSitesInformation(context);
        Set<String> systemSiteRequiredModules = getSystemSiteRequiredModules();
        context.getRequestScope().put("systemSiteRequiredModules", systemSiteRequiredModules);
        // Get list of definitions
        NodeTypeIterator nodeTypes = NodeTypeRegistry.getInstance().getNodeTypes(selectedModuleName);
        Map<String, Boolean> booleanMap = new TreeMap<String, Boolean>();
        while (nodeTypes.hasNext()) {
            ExtendedNodeType nodeType = (ExtendedNodeType) nodeTypes.next();
            booleanMap.put(nodeType.getLabel(LocaleContextHolder.getLocale()), nodeType.isNodeType(
                    "jmix:droppableContent"));
        }
        context.getRequestScope().put("nodeTypes", booleanMap);
    }

    public void populateSitesInformation(RequestContext context) {
        //populate information about sites
        List<String> siteKeys = new ArrayList<String>();
        Map<String, List<String>> directSiteDep = new HashMap<String, List<String>>();
        Map<String, List<String>> templateSiteDep = new HashMap<String, List<String>>();
        Map<String, List<String>> transitiveSiteDep = new HashMap<String, List<String>>();
        try {
            List<JCRSiteNode> sites = sitesService.getSitesNodeList();
            for (JCRSiteNode site : sites) {
                siteKeys.add(site.getSiteKey());
                List<JahiaTemplatesPackage> directDependencies = templateManagerService.getInstalledModulesForSite(
                        site.getSiteKey(), false, true, false);
                for (JahiaTemplatesPackage directDependency : directDependencies) {
                    if (!directSiteDep.containsKey(directDependency.getId())) {
                        directSiteDep.put(directDependency.getId(), new ArrayList<String>());
                    }
                    directSiteDep.get(directDependency.getId()).add(site.getSiteKey());
                }
                if (site.getTemplatePackage() != null) {
                    if (!templateSiteDep.containsKey(site.getTemplatePackage().getId())) {
                        templateSiteDep.put(site.getTemplatePackage().getId(), new ArrayList<String>());
                    }
                    templateSiteDep.get(site.getTemplatePackage().getId()).add(site.getSiteKey());
                }
                List<JahiaTemplatesPackage> transitiveDependencies = templateManagerService.getInstalledModulesForSite(
                        site.getSiteKey(), true, false, true);
                for (JahiaTemplatesPackage transitiveDependency : transitiveDependencies) {
                    if (!transitiveSiteDep.containsKey(transitiveDependency.getId())) {
                        transitiveSiteDep.put(transitiveDependency.getId(), new ArrayList<String>());
                    }
                    transitiveSiteDep.get(transitiveDependency.getId()).add(site.getSiteKey());
                }
            }
        } catch (JahiaException e) {
            logger.error(e.getMessage(), e);
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        context.getRequestScope().put("sites", siteKeys);
        context.getRequestScope().put("sitesDirect", directSiteDep);
        context.getRequestScope().put("sitesTemplates", templateSiteDep);
        context.getRequestScope().put("sitesTransitive", transitiveSiteDep);

        populateModuleVersionStateInfo(context, directSiteDep, templateSiteDep, transitiveSiteDep);
        populateModulesWithNodetypesInfo(context);
    }

    /**
     * Returns a map, keyed by the module name, with the sorted map (by version ascending) of {@link JahiaTemplatesPackage} objects.
     *
     * @return a map, keyed by the module name, with the sorted map (by version ascending) of {@link JahiaTemplatesPackage} objects
     */
    public Map<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>> getAllModuleVersions() {
        Map<Bundle, ModuleState> moduleStates = templateManagerService.getModuleStates();
        Map<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>> allModuleVersions = templateManagerService.getTemplatePackageRegistry().getAllModuleVersions();
        Map<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>> result = new TreeMap<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>>(allModuleVersions);
        for (Bundle bundle : moduleStates.keySet()) {
            JahiaTemplatesPackage module = BundleUtils.getModule(bundle);
            if (!allModuleVersions.containsKey(module.getId())) {
                TreeMap<ModuleVersion, JahiaTemplatesPackage> map = new TreeMap<ModuleVersion, JahiaTemplatesPackage>();
                map.put(module.getVersion(), module);
                result.put(module.getId(), map);
            }
        }
        return result;
    }

    /**
     * Returns a map, keyed by the module name, with all available module updates.
     *
     * @return a map, keyed by the module name, with all available module updates
     */
    public Map<String, Module> getAvailableUpdates() {
        Map<String, Module> availableUpdate = new HashMap<String, Module>();
        Map<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>> moduleStates = templateManagerService.getTemplatePackageRegistry().getAllModuleVersions();
        for (String key : moduleStates.keySet()) {
            Module forgeModule = forgeService.findModule(key, moduleStates.get(key).get(moduleStates.get(key).firstKey()).getGroupId());
            if (forgeModule != null) {
                ModuleVersion forgeVersion = new ModuleVersion(forgeModule.getVersion());
                if (!moduleStates.get(key).containsKey(forgeVersion) && forgeVersion.compareTo(moduleStates.get(key).lastKey()) > 0) {
                    availableUpdate.put(key, forgeModule);
                }
            }
        }
        return availableUpdate;
    }

    private void populateModuleVersionStateInfo(RequestContext context, Map<String, List<String>> directSiteDep,
                                                Map<String, List<String>> templateSiteDep, Map<String, List<String>> transitiveSiteDep) {
        Map<String, Map<ModuleVersion, ModuleVersionState>> states = new TreeMap<String, Map<ModuleVersion, ModuleVersionState>>();
        Map<String, String> errors = new TreeMap<String, String>();

        Set<String> systemSiteRequiredModules = getSystemSiteRequiredModules();
        context.getRequestScope().put("systemSiteRequiredModules", systemSiteRequiredModules);
        for (Map.Entry<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>> entry : getAllModuleVersions().entrySet()) {
            Map<ModuleVersion, ModuleVersionState> moduleVersions = states.get(entry.getKey());
            if (moduleVersions == null) {
                moduleVersions = new TreeMap<ModuleVersion, ModuleVersionState>();
                states.put(entry.getKey(), moduleVersions);
            }

            if (BundleUtils.getContextStartException(entry.getKey()) != null) {
                errors.put(entry.getKey(), BundleUtils.getContextStartException(entry.getKey()).getLocalizedMessage());
            }

            for (Map.Entry<ModuleVersion, JahiaTemplatesPackage> moduleVersionEntry : entry.getValue().entrySet()) {
                ModuleVersionState state = getModuleVersionState(context, moduleVersionEntry.getKey(),
                        moduleVersionEntry.getValue(), entry.getValue().size() > 1, directSiteDep, templateSiteDep, transitiveSiteDep, systemSiteRequiredModules, errors);
                moduleVersions.put(moduleVersionEntry.getKey(), state);
            }
        }
        context.getRequestScope().put("moduleStates", states);
        context.getRequestScope().put("errors", errors);
    }

    private void populateModulesWithNodetypesInfo(RequestContext context) {
        Set<String> modulesWithNodetypes = new HashSet<String>();
        NodeTypeRegistry nodeTypeRegistry = NodeTypeRegistry.getInstance();
        for (String moduleId : getAllModuleVersions().keySet()) {
            if (nodeTypeRegistry.getNodeTypes(moduleId).hasNext()) {
                modulesWithNodetypes.add(moduleId);
            }
        }
        context.getRequestScope().put("modulesWithNodetypes", modulesWithNodetypes);
    }

    private ModuleVersionState getModuleVersionState(RequestContext context, ModuleVersion moduleVersion, JahiaTemplatesPackage pkg,
                                                     boolean multipleVersionsOfModuleInstalled, Map<String, List<String>> directSiteDep,
                                                     Map<String, List<String>> templateSiteDep, Map<String, List<String>> transitiveSiteDep, Set<String> systemSiteRequiredModules, Map<String, String> errors) {
        ModuleVersionState state = new ModuleVersionState();
        Map<String, JahiaTemplatesPackage> registeredModules = templateManagerService.getTemplatePackageRegistry()
                .getRegisteredModules();
        String moduleId = pkg.getId();

        // check for unresolved dependencies
        if (!pkg.getDepends().isEmpty()) {
            for (String dependency : pkg.getDepends()) {
                if (templateManagerService.getTemplatePackageRegistry().getAvailableVersionsForModule(dependency).isEmpty()) {
                    state.getUnresolvedDependencies().add(dependency);
                }
            }
        }
        List<JahiaTemplatesPackage> dependantModules = templateManagerService.getTemplatePackageRegistry()
                .getDependantModules(pkg);
        for (JahiaTemplatesPackage dependant : dependantModules) {
            state.getDependencies().add(dependant.getId());
        }

        // check site usage and system dependency
        if (templateSiteDep.containsKey(moduleId)) {
            state.getUsedInSites().addAll(templateSiteDep.get(moduleId));
        }
        if (directSiteDep.containsKey(moduleId)) {
            state.getUsedInSites().addAll(directSiteDep.get(moduleId));
        }
        if (transitiveSiteDep.containsKey(moduleId)) {
            state.getUsedInSites().addAll(transitiveSiteDep.get(moduleId));
        }
        state.setSystemDependency(systemSiteRequiredModules.contains(moduleId));

        if (registeredModules.containsKey(moduleId)
                && registeredModules.get(moduleId).getVersion().equals(moduleVersion)) {
            // this is the currently active version of a module
            state.setCanBeStopped(!state.isSystemDependency());
        } else {
            // not currently active version of a module
            if (pkg.getState().getState() == ModuleState.State.INCOMPATIBLE_VERSION) {
                state.setCanBeStarted(false);
                state.setInstalled(false);
                state.setCanBeUninstalled(state.getUsedInSites().isEmpty() || multipleVersionsOfModuleInstalled);
                if (pkg.getState().getDetails() != null) {
                    String dspMsg = Messages.getWithArgs("resources.JahiaServerSettings", "serverSettings.manageModules.incompatibleVersion", LocaleContextHolder.getLocale(), pkg.getState().getDetails().toString());
                    errors.put(moduleId, dspMsg);
                }
            } else if (state.getUnresolvedDependencies().isEmpty()) {
                // no unresolved dependencies -> can start module version
                state.setCanBeStarted(true);

                // if the module is not used in sites or this is not the only version of a module installed -> allow to uninstall it
                state.setCanBeUninstalled(state.getUsedInSites().isEmpty() || multipleVersionsOfModuleInstalled);
            } else {
                state.setCanBeUninstalled(!state.isSystemDependency());
            }
        }

        return state;
    }

    private void populateActiveVersion(RequestContext context, JahiaTemplatesPackage value) {
        context.getRequestScope().put("activeVersion", value);
        Map<String, String> bundleInfo = new HashMap<String, String>();
        Dictionary<String, String> dictionary = value.getBundle().getHeaders();
        Enumeration<String> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String s = keys.nextElement();
            bundleInfo.put(s, dictionary.get(s));
        }
        context.getRequestScope().put("bundleInfo", bundleInfo);
        context.getRequestScope().put("activeVersionDate", new Date(value.getBundle().getLastModified()));

        context.getRequestScope().put("dependantModules", templateManagerService.getTemplatePackageRegistry().getDependantModules(value));
    }

    private Set<String> getSystemSiteRequiredModules() {
        Set<String> modules = new TreeSet<String>();
        for (String module : templateManagerService.getNonManageableModules()) {
            JahiaTemplatesPackage pkg = templateManagerService.getTemplatePackageById(module);
            if (pkg != null) {
                modules.add(pkg.getId());
                for (JahiaTemplatesPackage dep : pkg.getDependencies()) {
                    modules.add(dep.getId());
                }
            }
        }

        return modules;
    }

    public Date getLastModulesUpdateTime() {
        return new Date(forgeService.getLastUpdateTime());
    }

    public void initModules(RequestContext requestContext, RenderContext renderContext) {
        // generate tables ids, used by datatable jquery plugin to store the state of a table in the user localestorage.
        // new flow = new ids
        reloadTablesUUIDFromSession(requestContext);
        if (!requestContext.getFlowScope().contains("adminModuleTableUUID")) {
            requestContext.getFlowScope().put("adminModuleTableUUID", UUID.randomUUID().toString());
            requestContext.getFlowScope().put("forgeModuleTableUUID", UUID.randomUUID().toString());
        }

        if (!isStudio(renderContext)) {
            forgeService.loadModules();
            final Object moduleHasBeenStarted = requestContext.getExternalContext().getSessionMap().get(
                    "moduleHasBeenStarted");
            if (moduleHasBeenStarted != null) {
                requestContext.getMessageContext().addMessage(new MessageBuilder().info().source(moduleHasBeenStarted).code("serverSettings.manageModules.module.started").arg(moduleHasBeenStarted).build());
                requestContext.getExternalContext().getSessionMap().remove("moduleHasBeenStarted");
            }
            final Object moduleHasBeenStopped = requestContext.getExternalContext().getSessionMap().get(
                    "moduleHasBeenStopped");
            if (moduleHasBeenStopped != null) {
                requestContext.getMessageContext().addMessage(new MessageBuilder().info().source(moduleHasBeenStopped).code("serverSettings.manageModules.module.stopped").arg(moduleHasBeenStopped).build());
                requestContext.getExternalContext().getSessionMap().remove("moduleHasBeenStopped");
            }
            final List<String> missingDependencies = (List<String>) requestContext.getExternalContext().getSessionMap().get("missingDependencies");
            if (missingDependencies != null) {
                createMessageForMissingDependencies(requestContext.getMessageContext(), missingDependencies);
                requestContext.getExternalContext().getSessionMap().remove("missingDependencies");

            }
        }
    }

    public void reloadModules() {
        forgeService.flushModules();
        forgeService.loadModules();
    }

    public List<Module> getForgeModules() {
        List<Module> installedModule = new ArrayList<Module>();
        List<Module> newModules = new ArrayList<Module>();
        for (Module module : forgeService.getModules()) {
            module.setInstallable(!templateManagerService.differentModuleWithSameIdExists(module.getId(), module.getGroupId()));
            JahiaTemplatesPackage pkg = templateManagerService.getTemplatePackageRegistry().lookupById(module.getId());
            if (pkg != null && pkg.getGroupId().equals(module.getGroupId())) {
                installedModule.add(module);
            } else {
                newModules.add(module);
            }
        }

        newModules.addAll(installedModule);
        return newModules;
    }


    /**
     * Logs the specified exception details.
     *
     * @param e the occurred exception to be logged
     */
    public void logError(Exception e) {
        logger.error(e.getMessage(), e);
    }

    public void handleError(Exception exception, MutableAttributeMap flowScope, MessageContext messageContext) {
        if (exception instanceof ScmUnavailableModuleIdException) {
            messageContext.addMessage(new MessageBuilder().error().code(
                    "serverSettings.manageModules.duplicateModuleError.moduleExists").arg(flowScope.get("newModuleName")).build());
        } else if (exception instanceof ScmWrongVersionException) {
            messageContext.addMessage(new MessageBuilder().error().code(
                    "serverSettings.manageModules.downloadSourcesError.wrongVersion").build());
        } else if (exception instanceof SourceControlException) {
            messageContext.addMessage(new MessageBuilder().error().code(
                    "serverSettings.manageModules.downloadSourcesError").arg(flowScope.get("version")).build());
        } else {
            String message = exception.getLocalizedMessage();
            if (StringUtils.isBlank(message)) {
                message = exception.toString();
            }
            messageContext.addMessage(new MessageBuilder().error().defaultText(message).build());
        }
    }

    public void startModule(String moduleId, String version, RequestContext requestContext) throws RepositoryException, BundleException {
        List<String> missingDependencies = new ArrayList<String>();
        JahiaTemplatesPackage module = templateManagerService.getTemplatePackageRegistry().lookupByIdAndVersion(moduleId,
                new ModuleVersion(version));
        for (JahiaTemplatesPackage dep : module.getDependencies()) {
            if (!dep.isActiveVersion()) {
                missingDependencies.add(dep.getName());
            }
        }
        if (missingDependencies.isEmpty()) {
            moduleManager.start(module.getBundleKey(), null);
            requestContext.getExternalContext().getSessionMap().put("moduleHasBeenStarted", moduleId);
        } else {
            requestContext.getExternalContext().getSessionMap().put("missingDependencies", missingDependencies);
        }
        storeTablesUUID(requestContext);
    }

    public void uninstallModule(String moduleId, String moduleVersion, RequestContext requestContext) throws RepositoryException, BundleException {
        moduleManager.uninstall(BundleInfo.fromModuleInfo(moduleId, moduleVersion).getKey(), null);
    }

    public void stopModule(String moduleId, RequestContext requestContext) throws RepositoryException, BundleException {
        // templateManagerService.stopModule(moduleId);
        JahiaTemplatesPackage module = templatePackageRegistry.lookupById(moduleId);
        try {
            moduleManager.stop(module.getBundleKey(), null);
            requestContext.getExternalContext().getSessionMap().put("moduleHasBeenStopped", moduleId);
            storeTablesUUID(requestContext);
        } catch (ModuleNotFoundException e) {
            requestContext.getMessageContext()
            .addMessage(new MessageBuilder().error()
                    .code("serverSettings.manageModules.createOperation.failure.notFound")
                    .args("stop", "moduleId").build());
        }
    }

    public void storeTablesUUID(RequestContext requestContext) {
        requestContext.getExternalContext().getSessionMap().put("adminModuleTableUUID", requestContext.getFlowScope().get("adminModuleTableUUID"));
        requestContext.getExternalContext().getSessionMap().put("forgeModuleTableUUID", requestContext.getFlowScope().get("forgeModuleTableUUID"));
    }

    private void reloadTablesUUIDFromSession(RequestContext requestContext) {
        if (requestContext.getExternalContext().getSessionMap().contains("adminModuleTableUUID") && !requestContext.getFlowScope().contains("adminModuleTableUUID")) {
            requestContext.getFlowScope().put("adminModuleTableUUID", requestContext.getExternalContext().getSessionMap().get("adminModuleTableUUID"));
            requestContext.getFlowScope().put("forgeModuleTableUUID", requestContext.getExternalContext().getSessionMap().get("forgeModuleTableUUID"));

            requestContext.getExternalContext().getSessionMap().remove("adminModuleTableUUID");
            requestContext.getExternalContext().getSessionMap().remove("forgeModuleTableUUID");
        }
    }

    public String[] getModuleNodetypes(String moduleId) {
        ArrayList<String> typeNames = new ArrayList<String>();
        NodeTypeRegistry.JahiaNodeTypeIterator it = NodeTypeRegistry.getInstance().getNodeTypes(moduleId);
        while (it.hasNext()) {
            typeNames.add(it.nextNodeType().getName());
        }
        return typeNames.toArray(new String[typeNames.size()]);
    }

    public Map<String, String> listBranchOrTags(String moduleVersion, String scmURI) throws IOException {
        if (moduleVersion.endsWith("-SNAPSHOT")) {
            return templateManagerService.listBranches(scmURI);
        } else {
            return templateManagerService.listTags(scmURI);
        }
    }

    public String guessBranchOrTag(String moduleVersion, String scmURI, Map<String, String> branchOrTags, String defaultBranchOrTag) {
        String branchOrTag = templateManagerService.guessBranchOrTag(moduleVersion, StringUtils.substringBefore(StringUtils.removeStart(scmURI, "scm:"), ":"), branchOrTags.keySet());
        return branchOrTag != null ? branchOrTag : defaultBranchOrTag;
    }

    public void validateScmInfo(String scmUri, String branchOrTag, String moduleVersion, MutableAttributeMap flashScope) throws IOException {
        if ((StringUtils.startsWith(scmUri, "scm:git:") && StringUtils.isBlank(branchOrTag))
                || (StringUtils.startsWith(scmUri, "scm:svn:") && StringUtils.contains(scmUri, "/trunk/"))) {
            Map<String, String> branchTagInfos = listBranchOrTags(moduleVersion, scmUri);
            flashScope.put("branchTagInfos", branchTagInfos);
            branchOrTag = guessBranchOrTag(moduleVersion, scmUri, branchTagInfos, null);
            flashScope.put("branchOrTag", branchOrTag);
        }
    }

    public JCRNodeWrapper checkoutModule(MutableAttributeMap flowScope, JCRSessionWrapper session) throws RepositoryException, XmlPullParserException, DocumentException, IOException, BundleException {
        String scmUri = (String) flowScope.get("scmUri");
        String branchOrTag = (String) flowScope.get("branchOrTag");
        String module = (String) flowScope.get("module");
        String version = (String) flowScope.get("version");
        try {
            return templateManagerService.checkoutModule(null, scmUri, branchOrTag, module, version, session);
        } catch (SourceControlException e) {
            Map<String, String> branchTagInfos = listBranchOrTags(version, scmUri);
            String newBranchOrTag = guessBranchOrTag(version, scmUri, branchTagInfos, branchOrTag);
            String newScmUri = branchTagInfos.get(newBranchOrTag);
            if (newScmUri != null && newBranchOrTag != null && (!newBranchOrTag.equals(branchOrTag) || newScmUri.equals(scmUri))) {
                flowScope.put("scmUri", newScmUri);
                flowScope.put("branchTagInfos", branchTagInfos);
                flowScope.put("branchOrTag", newBranchOrTag);
                return templateManagerService.checkoutModule(null, newScmUri, newBranchOrTag, module, version, session);
            }
            throw e;
        }
    }

    public File checkoutTempModule(MutableAttributeMap flowScope) throws RepositoryException, XmlPullParserException, DocumentException, IOException {
        String scmUri = (String) flowScope.get("scmUri");
        String branchOrTag = (String) flowScope.get("branchOrTag");
        String module = (String) flowScope.get("module");
        String version = (String) flowScope.get("version");
        try {
            return templateManagerService.checkoutTempModule(scmUri, branchOrTag, module, version);
        } catch (SourceControlException e) {
            Map<String, String> branchTagInfos = listBranchOrTags(version, scmUri);
            String newBranchOrTag = guessBranchOrTag(version, scmUri, branchTagInfos, branchOrTag);
            String newScmUri = branchTagInfos.get(newBranchOrTag);
            if (newScmUri != null && newBranchOrTag != null && (!newBranchOrTag.equals(branchOrTag) || newScmUri.equals(scmUri))) {
                flowScope.put("newScmUri", newScmUri);
                flowScope.put("branchTagInfos", branchTagInfos);
                flowScope.put("branchOrTag", newBranchOrTag);
                return templateManagerService.checkoutTempModule(newScmUri, newBranchOrTag, module, version);
            }
            throw e;
        }
    }

    public void deleteTempSources(File tempSources) {
        if (tempSources != null && tempSources.exists()) {
            FileUtils.deleteQuietly(tempSources);
        }
    }
}
