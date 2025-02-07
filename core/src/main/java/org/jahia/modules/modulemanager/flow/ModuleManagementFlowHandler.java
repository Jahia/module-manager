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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.dom4j.DocumentException;
import org.jahia.bin.Jahia;
import org.jahia.commons.Version;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.data.templates.ModuleState;
import org.jahia.data.templates.ModuleState.State;
import org.jahia.data.templates.ModulesPackage;
import org.jahia.exceptions.JahiaException;
import org.jahia.modules.modulemanager.configuration.OperationConstraints;
import org.jahia.modules.modulemanager.configuration.OperationConstraintsService;
import org.jahia.modules.modulemanager.configuration.OperationConstraintsUtil;
import org.jahia.modules.modulemanager.forge.ForgeService;
import org.jahia.modules.modulemanager.forge.Module;
import org.jahia.modules.modulemanager.message.CustomMessage;
import org.jahia.modules.modulemanager.message.MessageService;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.jahia.security.spi.LicenseCheckUtil;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.modulemanager.*;
import org.jahia.services.modulemanager.models.JahiaDepends;
import org.jahia.services.render.RenderContext;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.templates.*;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.i18n.Messages;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.binding.message.MessageResolver;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.RequestContext;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeIterator;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * WebFlow handler for managing modules.
 *
 * @author rincevent
 */
public class ModuleManagementFlowHandler implements Serializable {

    private static final long serialVersionUID = -4195379181264451784L;
    private static final Logger logger = LoggerFactory.getLogger(ModuleManagementFlowHandler.class);
    public static final Version jahiaVersion = new Version(Jahia.VERSION);

    @Autowired
    private transient JahiaTemplateManagerService templateManagerService;

    @Autowired
    private transient DefinitionsManagerService definitionsManagerService;

    @Autowired
    private transient JCRStoreService jcrStoreService;

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

    public boolean installModule(String forgeId, String url, boolean autoStart, boolean ignoreChecks, MessageContext context) {
        File file = null;
        try {
            file = forgeService.downloadModuleFromForge(forgeId, url);
            handleModule(file, context, file.getName(), ignoreChecks, autoStart, ignoreChecks);
            return true;
        } catch (Exception e) {
            handleInstallationFailure(context, e);
        } finally {
            FileUtils.deleteQuietly(file);
        }
        return false;
    }

    public boolean uploadModule(MultipartFile moduleFile, MessageContext context, boolean forceUpdate, boolean autoStart, boolean ignoreChecks) {
        if (moduleFile == null) {
            context.addMessage(new MessageBuilder().error().source("moduleFile")
                    .code("serverSettings.manageModules.install.moduleFileRequired").build());
            return false;
        }

        String originalFilename = moduleFile.getOriginalFilename();

        File file = null;
        try {
            file = File.createTempFile("module-", "." + StringUtils.substringAfterLast(originalFilename, "."));
            moduleFile.transferTo(file);
            return handleModule(file, context, originalFilename, forceUpdate, autoStart, ignoreChecks);
        } catch (Exception e) {
            handleInstallationFailure(context, e);
        } finally {
            FileUtils.deleteQuietly(file);
        }
        return false;
    }
    

    private boolean handleModule(File file, MessageContext context, String originalFilename, boolean forceUpdate, boolean autoStart, boolean ignoreChecks) {
        try {
            String fileNameLoweredCase = StringUtils.lowerCase(originalFilename);
            boolean canHandleExtension = FilenameUtils.isExtension(fileNameLoweredCase, "jar");
            //If it cannot be handled as Jar, look for impl that can handle this extension
            if (!canHandleExtension) {
                BundleContext bundleContext = FrameworkService.getBundleContext();
                Collection<ServiceReference<ArtifactUrlTransformer>> serviceReferences = bundleContext.getServiceReferences(ArtifactUrlTransformer.class, null);
                for (ServiceReference<ArtifactUrlTransformer> serviceReference : serviceReferences) {
                    ArtifactUrlTransformer transformer = bundleContext.getService(serviceReference);
                    if (transformer.canHandle(file)) {
                        File fileTemp = File.createTempFile("module-", "." + StringUtils.substringAfterLast(originalFilename, "."));
                        try {
                            URL transformedURL = transformer.transform(file.toURI().toURL());
                            FileUtils.copyInputStreamToFile(transformedURL.openConnection().getInputStream(), fileTemp);
                            installBundles(fileTemp, context, originalFilename, forceUpdate, autoStart, ignoreChecks);
                            return true;
                        } finally {
                            FileUtils.deleteQuietly(fileTemp);
                        }
                    }
                }
            }

            //If it is not a jar and none impl can handle this extension
            if (!canHandleExtension) {
                MessageBuilder moduleFileMsgBuilder = new MessageBuilder().error().source("moduleFile");
                // Use a custom error message for tgz files when no Javascript Modules Engine is installed
                if (FilenameUtils.isExtension(fileNameLoweredCase, "tgz")) {
                    context.addMessage(moduleFileMsgBuilder
                            .code("serverSettings.manageModules.install.missingJavascriptEngine").build());
                } else {
                    context.addMessage(moduleFileMsgBuilder
                            .code("serverSettings.manageModules.install.wrongFormat").build());
                }

                return false;
            }

            installBundles(file, context, originalFilename, forceUpdate, autoStart, ignoreChecks);
            return true;
        } catch (Exception e) {
            handleInstallationFailure(context, e);
        } finally {
            FileUtils.deleteQuietly(file);
        }
        return false;
    }

    private static void handleInstallationFailure(MessageContext context, Exception e) {
        context.addMessage(new MessageBuilder().source("moduleFile")
                .code("serverSettings.manageModules.install.failed")
                .arg(e.getMessage())
                .error()
                .build());
        logger.error(e.getMessage(), e);
    }

    private void installBundles(File file, MessageContext context, String originalFilename, boolean forceUpdate, boolean autoStart, boolean ignoreChecks) throws IOException, BundleException {

        JarFile jarFile = new JarFile(file);
        try {

            Attributes manifestAttributes = jarFile.getManifest().getMainAttributes();
            String jahiaRequiredVersion = manifestAttributes.getValue(Constants.ATTR_NAME_JAHIA_REQUIRED_VERSION);
            if (StringUtils.isEmpty(jahiaRequiredVersion)) {
                context.addMessage(new MessageBuilder().source("moduleFile")
                        .code("serverSettings.manageModules.install.required.version.missing.error").error().build());
                return;
            }
            Version requiredVersion = new Version(jahiaRequiredVersion);
            if (requiredVersion.compareTo(jahiaVersion) > 0 || requiredVersion.getMajorVersion() != jahiaVersion.getMajorVersion()) {
                context.addMessage(new MessageBuilder().source("moduleFile")
                        .code("serverSettings.manageModules.install.required.version.error")
                        .args(jahiaRequiredVersion, Jahia.VERSION).error().build());
                return;
            }
            //Check license
            String licenseFeature = manifestAttributes.getValue("Jahia-Key");
            if (licenseFeature != null && !LicenseCheckUtil.isAllowed(licenseFeature)) {
                context.addMessage(new MessageBuilder().source("moduleFile")
                        .code("serverSettings.manageModules.install.module.missing.license")
                        .args(originalFilename, licenseFeature)
                        .error().build());
                return;
            }

            if (manifestAttributes.getValue(Constants.ATTR_NAME_JAHIA_PACKAGE_NAME) != null) {
                handlePackage(jarFile, manifestAttributes, originalFilename, forceUpdate, autoStart, ignoreChecks, context);
            } else {
                ModuleInstallationResult installationResult = installModule(file, context, null, null, forceUpdate, autoStart, ignoreChecks);
                if (installationResult != null) {
                    addModuleInstallationMessage(installationResult, context);
                }
            }
        } finally {
            jarFile.close();
        }
    }

    private void handlePackage(JarFile jarFile, Attributes manifestAttributes, String originalFilename,
                               boolean forceUpdate, boolean autoStart, boolean ignoreChecks, MessageContext context) throws IOException, BundleException {

        // check package name validity
        String jahiaPackageName = manifestAttributes.getValue(Constants.ATTR_NAME_JAHIA_PACKAGE_NAME);
        if (jahiaPackageName != null && jahiaPackageName.trim().isEmpty()) {
            context.addMessage(new MessageBuilder().source("moduleFile")
                    .code("serverSettings.manageModules.install.package.name.error").error().build());
            return;
        }

        //Check license
        String licenseFeature = manifestAttributes.getValue(Constants.ATTR_NAME_JAHIA_PACKAGE_LICENSE);
        if (licenseFeature != null && !LicenseCheckUtil.isAllowed(licenseFeature)) {
            context.addMessage(new MessageBuilder().source("moduleFile")
                    .code("serverSettings.manageModules.install.package.missing.license")
                    .args(originalFilename, licenseFeature)
                    .error().build());
            return;
        }

        ModulesPackage pack = ModulesPackage.create(jarFile);

        try {

            List<String> providedBundles = new ArrayList<String>(pack.getModules().keySet());
            Map<Bundle, MessageResolver> collectedResolutionErrors = new LinkedHashMap<>();
            List<ModuleInstallationResult> installationResults = new LinkedList<>();
            for (ModulesPackage.PackagedModule entry : pack.getModules().values()) {
                ModuleInstallationResult installationResult = installModule(entry.getModuleFile(), context, providedBundles, collectedResolutionErrors, forceUpdate, autoStart, ignoreChecks);
                if (installationResult != null) {
                    installationResults.add(installationResult);
                }
            }
            if (!collectedResolutionErrors.isEmpty()) {
                // double-check the resolution issues after all bundles were installed
                for (Iterator<Map.Entry<Bundle, MessageResolver>> resolutionErrorIterator = collectedResolutionErrors.entrySet().iterator(); resolutionErrorIterator.hasNext(); ) {
                    Entry<Bundle, MessageResolver> resolutionErrorEntry = resolutionErrorIterator.next();
                    if (resolutionErrorEntry.getKey().getState() >= Bundle.RESOLVED) {
                        // the bundle is successfully resolved now
                        resolutionErrorIterator.remove();
                    } else {
                        // the bundle still has resolution issue -> add error message
                        context.addMessage(resolutionErrorEntry.getValue());
                    }
                }
            }

            // add info about installed bundles
            for (ModuleInstallationResult installationResult : installationResults) {
                if (!collectedResolutionErrors.containsKey(installationResult.getBundle())) {
                    addModuleInstallationMessage(installationResult, context);
                }
            }
        } finally {
            // delete temporary created files of the package
            for (ModulesPackage.PackagedModule entry : pack.getModules().values()) {
                FileUtils.deleteQuietly(entry.getModuleFile());
            }
        }
    }

    /**
     * Add module installation result to the message context
     * @param installationResult module installation result
     * @param context message context
     * @throws BundleException thrown exception
     */
    private void addModuleInstallationMessage(ModuleInstallationResult installationResult, MessageContext context) throws BundleException {
        Bundle bundle = installationResult.getBundle();
        context.addMessage(new MessageBuilder().source("moduleFile")
                .code(installationResult.getMessageCode())
                .args(bundle.getSymbolicName(), bundle.getVersion().toString())
                .build());
    }

    private ModuleInstallationResult installModule(File file, MessageContext context, List<String> providedBundles,
            Map<Bundle, MessageResolver> collectedResolutionErrors,
            boolean forceUpdate, boolean autoStart, boolean ignoreChecks) throws IOException, BundleException {

        JarFile jarFile = new JarFile(file);
        try {
            Manifest manifest = jarFile.getManifest();
            String symbolicName = manifest.getMainAttributes().getValue(Constants.ATTR_NAME_BUNDLE_SYMBOLIC_NAME);
            if (symbolicName == null) {
                symbolicName = manifest.getMainAttributes().getValue(Constants.ATTR_NAME_ROOT_FOLDER);
            }
            String version = StringUtils.defaultIfBlank(manifest.getMainAttributes().getValue(Constants.ATTR_NAME_IMPL_VERSION), manifest.getMainAttributes().getValue(Constants.ATTR_NAME_BUNDLE_VERSION));
            String groupId = manifest.getMainAttributes().getValue(Constants.ATTR_NAME_GROUP_ID);

            if (!OperationConstraintsUtil.checkDeployConstraint(symbolicName, version, context)) {
                return null;
            }

            String successMessage = (autoStart
                    ? "serverSettings.manageModules.install.uploadedAndStarted"
                    : "serverSettings.manageModules.install.uploaded");
            boolean shouldAutoStart = autoStart;
            if (groupId != null) {
                // GroupId set only for jahia modules
                if (templateManagerService.differentModuleWithSameIdExists(symbolicName, groupId)) {
                    context.addMessage(new MessageBuilder().source("moduleFile")
                            .code("serverSettings.manageModules.install.moduleWithSameIdExists")
                            .arg(symbolicName)
                            .error()
                            .build());
                    return null;
                }
                ModuleVersion moduleVersion = new ModuleVersion(version);
                Set<ModuleVersion> allVersions = templatePackageRegistry.getAvailableVersionsForModule(symbolicName);
                if (!forceUpdate) {
                    if (!moduleVersion.isSnapshot()) {
                        if (allVersions.contains(moduleVersion)) {
                            context.addMessage(new MessageBuilder().source("moduleExists")
                                    .code("serverSettings.manageModules.install.moduleExists")
                                    .args(symbolicName, version)
                                    .build());
                            return null;
                        }
                    }
                }

                if (autoStart &&
                        !Boolean.parseBoolean(SettingsBean.getInstance().getPropertiesFile().getProperty("org.jahia.modules.autoStartOlderVersions"))) {
                    // verify that a newer version is not active already
                    JahiaTemplatesPackage currentActivePackage = templateManagerService.getTemplatePackageRegistry()
                            .lookupById(symbolicName);
                    ModuleVersion currentVersion = currentActivePackage != null ? currentActivePackage.getVersion() : null;
                    if (currentActivePackage != null && moduleVersion.compareTo(currentVersion) < 0) {
                        // we do not start the uploaded older version automatically
                        shouldAutoStart = false;
                        successMessage = "serverSettings.manageModules.install.uploadedNotStartedDueToNewerVersionActive";
                    }
                }
            }

            String resolutionError = null;

            try {
                moduleManager.install(Collections.singleton(new FileSystemResource(file)), null, shouldAutoStart, ignoreChecks);
            } catch (ModuleManagementException e) {
                Throwable cause = e.getCause();
                if (cause instanceof BundleException && ((BundleException) cause).getType() == BundleException.RESOLVE_ERROR) {
                    // we are dealing with unresolved dependencies here
                    resolutionError = cause.getMessage();
                } else {
                    // re-throw the exception
                    throw e;
                }
            }

            Bundle bundle = BundleUtils.getBundle(symbolicName, version);

            if (BundleUtils.isJahiaBundle(bundle)) {

                JahiaTemplatesPackage module = BundleUtils.getModule(bundle);

                if (module.getState().getState() == ModuleState.State.WAITING_TO_BE_IMPORTED) {
                    // This only can happen in a cluster.
                    successMessage = "serverSettings.manageModules.install.waitingToBeImported";
                }

                if (resolutionError != null) {
                    List<String> missingDeps = getMissingDependenciesFrom(module.getDepends(), providedBundles);
                    if (!missingDeps.isEmpty()) {
                        createMessageForMissingDependencies(context, missingDeps);
                    } else {
                        MessageResolver errorMessage = new MessageBuilder().source("moduleFile")
                                .code("serverSettings.manageModules.resolutionError").arg(resolutionError).error().build();
                        if (collectedResolutionErrors != null) {
                            // we just collect the resolution errors for multiple module to double-check them after all modules are installed
                            collectedResolutionErrors.put(bundle, errorMessage);
                            return new ModuleInstallationResult(bundle, successMessage);
                        } else {
                            // we directly add error message
                            context.addMessage(errorMessage);
                        }
                    }
                } else if (module.getState().getState() == ModuleState.State.ERROR_WITH_DEFINITIONS) {
                    context.addMessage(new MessageBuilder().source("moduleFile")
                            .code("serverSettings.manageModules.errorWithDefinitions")
                            .arg(((Exception) module.getState().getDetails()).getCause().getMessage())
                            .error()
                            .build());
                } else {
                    return new ModuleInstallationResult(bundle, successMessage);
                }
            } else {
                successMessage = (autoStart
                        ? "serverSettings.manageModules.install.uploadedAndStarted.bundle"
                        : "serverSettings.manageModules.install.uploaded.bundle");

                if (resolutionError != null) {
                    MessageResolver errorMessage = new MessageBuilder().source("moduleFile")
                            .code("serverSettings.manageModules.resolutionError.bundle").arg(resolutionError).error().build();
                    if (collectedResolutionErrors != null) {
                        // we just collect the resolution errors for multiple module to double-check them after all modules are installed
                        collectedResolutionErrors.put(bundle, errorMessage);
                    }
                }
                return new ModuleInstallationResult(bundle, successMessage);
            }
        } finally {
            IOUtils.closeQuietly(jarFile);
        }

        return null;
    }

    private void createMessageForMissingDependencies(MessageContext context, List<String> missingDeps) {
        logger.warn("Missing dependencies : " + missingDeps);
        context.addMessage(new MessageBuilder().source("moduleFile")
                .code("serverSettings.manageModules.install.missingDependencies")
                .arg(StringUtils.join(missingDeps, ","))
                .error()
                .build());
    }

    private List<String> getMissingDependenciesFrom(List<String> deps, List<String> providedDependencies) {
        List<String> missingDeps = new ArrayList<String>(deps.size());
        for (String dep : deps) {
            if (providedDependencies != null && providedDependencies.contains(dep)) {
                // we have the dependency
                continue;
            }
            if (templateManagerService.getTemplatePackageById(dep) == null && templateManagerService.getTemplatePackage(dep) == null) {
                missingDeps.add(dep);
            }
        }
        return missingDeps;
    }

    public void loadModuleInformation(RequestContext context) {

        String selectedModuleName = moduleName != null ? moduleName : (String) context.getFlowScope().get("selectedModule");
        Map<ModuleVersion, JahiaTemplatesPackage> selectedModule = getAllModuleVersions().get(selectedModuleName);
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
                    final List<String> missing = getMissingDependenciesFrom(module.getDepends(), null);
                    if (!missing.isEmpty()) {
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
        } catch (JahiaException | RepositoryException e) {
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
        Map<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>> result = new TreeMap<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>>();
        Map<Bundle, ModuleState> moduleStatesByBundle = templateManagerService.getModuleStates();
        for (Bundle bundle : moduleStatesByBundle.keySet()) {
            JahiaTemplatesPackage module = BundleUtils.getModule(bundle);
            SortedMap<ModuleVersion, JahiaTemplatesPackage> modulesByVersion = result.computeIfAbsent(module.getId(), k -> new TreeMap<>());
            modulesByVersion.put(module.getVersion(), module);
        }
        return result;
    }

    /**
     * Collect optional dependencies for all available modules
     *
     * @return a map of module ids and OptionDependency objects
     */
    public Map<String, List<OptionalDependency>> getOptionalDependenciesForAvailableModules() {
        Map<String, List<OptionalDependency>> result = new HashMap<>();
        Map<Bundle, ModuleState> moduleStatesByBundle = templateManagerService.getModuleStates();
        for (Bundle bundle : moduleStatesByBundle.keySet()) {
            JahiaTemplatesPackage module = BundleUtils.getModule(bundle);
            result.put(module.getId(), getOptionalDependenciesForModule(module));
        }
        return result;
    }

    private List<OptionalDependency> getOptionalDependenciesForModule(JahiaTemplatesPackage module) {
        List<OptionalDependency> result = new ArrayList<>();
        for (JahiaDepends jd : module.getVersionDepends()) {
            if (jd.isOptional()) {
                result.add(new OptionalDependency(jd.getModuleName(), templateManagerService.getTemplatePackageById(jd.getModuleName())));
            }
        }
        return result;
    }

    public void validateDefinitions(MessageContext context) {
        if (definitionsManagerService.skipDefinitionValidation()) {
            logger.debug("Skipping CND definition validation...");
            return;
        }

        Map<String, JahiaTemplatesPackage> modules = templateManagerService
                .getTemplatePackageRegistry().getRegisteredModules();
        for (String moduleId : modules.keySet()) {
            try {
                if (definitionsManagerService.isLatest(moduleId)) {
                    continue;
                }
                DefinitionsManagerService.CND_STATUS status = definitionsManagerService.checkDefinition(moduleId);
                if (status != DefinitionsManagerService.CND_STATUS.OK) {
                    JahiaTemplatesPackage pkg = modules.get(moduleId);
                    String latestVersion = jcrStoreService.getDefinitionVersion(pkg.getBundle().getSymbolicName());
                    String currentVersion = pkg.getVersion().toString();
                    logger.debug("'{}' module violates definition check with current started version {} against latest registered {}",
                            moduleId, currentVersion, latestVersion);
                    String messageCode = currentVersion.equals(latestVersion) ?
                            "serverSettings.manageModules.module.state.definitionConflict.sameVersion" :
                            "serverSettings.manageModules.module.state.definitionConflict";
                    context.addMessage(new MessageBuilder().source("moduleDefinitions")
                            .code(messageCode)
                            .arg(moduleId).arg(latestVersion).arg(currentVersion)
                            .warning()
                            .build());
                }
            } catch (IOException | RepositoryException e) {
                logger.error("Unable to validate definition for module {}", moduleId);
            }
        }
    }

    public void checkForMultipleVersions(MessageContext context, Map<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>> allModulesVersions) {
        if (allModulesVersions.values().stream().anyMatch(moduleVersionJahiaTemplatesPackageSortedMap -> moduleVersionJahiaTemplatesPackageSortedMap.size() > 1)) {
            String messageCode = SettingsBean.getInstance().isDevelopmentMode() ?
                    "serverSettings.manageModules.module.state.multipleVersions.development" :
                    "serverSettings.manageModules.module.state.multipleVersions.production";
            MessageBuilder customMessage = new MessageBuilder().source("customMessage")
                    .code(messageCode).arg("https://academy.jahia.com/documentation/developer/jahia/8/introducing-jahia-technical-concepts/about-osgi/jahia-osgi-implementation#recommendation");
            context.addMessage(SettingsBean.getInstance().isDevelopmentMode() ? customMessage.info().build() : customMessage.warning().build());
        }
    }

    /**
     * Returns a map, keyed by the module name, with all available module updates.
     *
     * @return a map, keyed by the module name, with all available module updates
     */
    public Map<String, Module> getAvailableUpdates() {
        OperationConstraintsService opConstraintsService = BundleUtils.getOsgiService(OperationConstraintsService.class, null);
        Map<String, Module> availableUpdate = new HashMap<String, Module>();
        Map<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>> moduleStates = getAllModuleVersions();
        for (String key : moduleStates.keySet()) {
            SortedMap<ModuleVersion, JahiaTemplatesPackage> moduleVersions = moduleStates.get(key);
            Module forgeModule = forgeService.findModule(key, moduleVersions.get(moduleVersions.firstKey()).getGroupId());
            if (forgeModule != null) {
                ModuleVersion forgeVersion = new ModuleVersion(forgeModule.getVersion());
                org.osgi.framework.Version osgiVersion = new org.osgi.framework.Version(JahiaDepends.toOsgiVersion(forgeVersion.toString()));
                OperationConstraints ops = opConstraintsService.getConstraintForBundle(key, osgiVersion);
                if (!isSameOrNewerVersionPresent(key, forgeVersion) && (ops == null || ops.canDeploy(osgiVersion))) {
                    availableUpdate.put(key, forgeModule);
                }
            }
        }
        return availableUpdate;
    }

    private boolean isSameOrNewerVersionPresent(String symbolicName, ModuleVersion forgeVersion) {
        for (Bundle bundle : FrameworkService.getBundleContext().getBundles()) {
            String n = bundle.getSymbolicName();
            if (StringUtils.equals(n, symbolicName)
                    && forgeVersion.compareTo(new ModuleVersion(BundleUtils.getModuleVersion(bundle))) <= 0) {
                // we've found either same or a new version present
                return true;
            }
        }
        return false;
    }

    private void populateModuleVersionStateInfo(RequestContext context, Map<String, List<String>> directSiteDep,
                                                Map<String, List<String>> templateSiteDep, Map<String, List<String>> transitiveSiteDep) {

        Map<String, Map<ModuleVersion, ModuleVersionState>> states = new TreeMap<String, Map<ModuleVersion, ModuleVersionState>>();
        Map<String, String> errors = new TreeMap<String, String>();

        Set<String> systemSiteRequiredModules = getSystemSiteRequiredModules();
        context.getRequestScope().put("systemSiteRequiredModules", systemSiteRequiredModules);

        OperationConstraintsService opConstraintsService = BundleUtils.getOsgiService(OperationConstraintsService.class, null);
        for (Map.Entry<String, SortedMap<ModuleVersion, JahiaTemplatesPackage>> entry : getAllModuleVersions().entrySet()) {

            Map<ModuleVersion, ModuleVersionState> moduleVersions = states.computeIfAbsent(entry.getKey(), k -> new TreeMap<>());

            if (BundleUtils.getContextStartException(entry.getKey()) != null) {
                errors.put(entry.getKey(), BundleUtils.getContextStartException(entry.getKey()).getLocalizedMessage());
            }

            for (Map.Entry<ModuleVersion, JahiaTemplatesPackage> moduleVersionEntry : entry.getValue().entrySet()) {
                ModuleVersionState state = getModuleVersionState(context, moduleVersionEntry.getKey(),
                        moduleVersionEntry.getValue(), entry.getValue().size() > 1, directSiteDep, templateSiteDep, transitiveSiteDep, systemSiteRequiredModules, errors);
                moduleVersions.put(moduleVersionEntry.getKey(), state);

                Bundle b = moduleVersionEntry.getValue().getBundle();
                if (opConstraintsService != null) {
                    OperationConstraints ops = opConstraintsService.getConstraintForBundle(b);
                    if (ops != null) {
                        org.osgi.framework.Version v = b.getVersion();
                        state.setCanBeStarted(state.isCanBeStarted() && ops.canStart(v));
                        state.setCanBeStopped(state.isCanBeStopped() && ops.canStop(v));
                        state.setCanBeUninstalled(state.isCanBeUninstalled() && ops.canUndeploy(v));
                        state.setCanBeReinstalled(state.isCanBeReinstalled() && ops.canDeploy(v));
                    }
                }
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

        TemplatePackageRegistry registry = templateManagerService.getTemplatePackageRegistry();
        ModuleVersionState state = new ModuleVersionState();
        Map<String, JahiaTemplatesPackage> registeredModules = registry.getRegisteredModules();
        String moduleId = pkg.getId();

        // check for unresolved dependencies
        if (!pkg.getDepends().isEmpty()) {
            for (String dependency : pkg.getDepends()) {
                if (registry.getAvailableVersionsForModule(dependency).isEmpty()) {
                    state.getUnresolvedDependencies().add(dependency);
                }
            }
        }

        for (JahiaDepends d : pkg.getVersionDepends()) {
            if (!d.isOptional()) {
                JahiaTemplatesPackage p = templateManagerService.getAnyDeployedTemplatePackage(d.toString());
                if (p == null) state.getUnresolvedDependencies().add(d.toString());
            }
        }

        List<JahiaTemplatesPackage> dependantModules = registry.getDependantModules(pkg);
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

        ModuleState moduleState = pkg.getState();
        State stateFlag = moduleState.getState();
        Object details = moduleState.getDetails();
        if (registeredModules.containsKey(moduleId) && registeredModules.get(moduleId).getVersion().equals(moduleVersion)
                && (stateFlag == ModuleState.State.STARTED ||
                stateFlag == ModuleState.State.SPRING_STARTING)) {
            // this is the currently active version of a module
            state.setCanBeStopped(!state.isSystemDependency());
            if (details != null) {
                String dspMsg = Messages.getWithArgs("resources.ModuleManager", "serverSettings.manageModules.startError", LocaleContextHolder.getLocale(),
                        details.toString());
                addError(moduleVersion, errors, moduleId, dspMsg);
            }
        } else {
            // not currently active version of a module
            if (stateFlag == ModuleState.State.INCOMPATIBLE_VERSION) {
                state.setCanBeStarted(false);
                state.setInstalled(false);
                state.setCanBeUninstalled(state.getUsedInSites().isEmpty() || multipleVersionsOfModuleInstalled);
                if (details != null) {
                    String dspMsg = getI18nMessage("serverSettings.manageModules.incompatibleVersion", details.toString());
                    addError(moduleVersion, errors, moduleId, dspMsg);
                }
            } else if (stateFlag == ModuleState.State.ERROR_WITH_DEFINITIONS) {
                state.setCanBeStarted(false);
                state.setInstalled(false);
                state.setCanBeUninstalled(state.getUsedInSites().isEmpty() || multipleVersionsOfModuleInstalled);
                state.setCanBeReinstalled(true);
                if (details != null) {
                    String dspMsg = getI18nMessage("serverSettings.manageModules.errorWithDefinitions", details.toString());
                    addError(moduleVersion, errors, moduleId, dspMsg);
                }
            } else if (stateFlag == ModuleState.State.ERROR_WITH_RULES) {
                state.setCanBeStarted(false);
                state.setCanBeStopped(pkg.getBundle() != null && pkg.getBundle().getState() == Bundle.ACTIVE);
                state.setCanBeUninstalled(state.getUsedInSites().isEmpty() || multipleVersionsOfModuleInstalled);
                if (details != null) {
                    String dspMsg = getI18nMessage("serverSettings.manageModules.errorWithRules", details.toString());
                    addError(moduleVersion, errors, moduleId, dspMsg);
                }
            } else if (stateFlag == ModuleState.State.WAITING_TO_BE_IMPORTED) {
                state.setCanBeStarted(false);
                state.setCanBeUninstalled(state.getUsedInSites().isEmpty() || multipleVersionsOfModuleInstalled);
                state.setCanBeReinstalled(true);
                String dspMsg = getI18nMessage("serverSettings.manageModules.waitingToBeImported");
                addError(moduleVersion, errors, moduleId, dspMsg);
            } else if (stateFlag == ModuleState.State.SPRING_NOT_STARTED) {
                state.setCanBeStarted(false);
                state.setCanBeStopped(pkg.getBundle() != null && pkg.getBundle().getState() == Bundle.ACTIVE);
                state.setCanBeUninstalled(state.getUsedInSites().isEmpty() || multipleVersionsOfModuleInstalled);
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

    private String getI18nMessage(String key, Object... arguments) {
        return arguments != null
                ? Messages.getWithArgs("resources.ModuleManager", key, LocaleContextHolder.getLocale(), arguments)
                : Messages.get("resources.ModuleManager", key, LocaleContextHolder.getLocale());
    }

    private void addError(ModuleVersion moduleVersion, Map<String, String> errors, String moduleId, String dspMsg) {
        if (errors.containsKey(moduleId)) {
            errors.put(moduleId, errors.get(moduleId) + "\n\n" + moduleVersion + " : " + dspMsg);
        } else {
            errors.put(moduleId, moduleVersion + " : " + dspMsg);
        }
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

    private void addCustomMessages(MessageContext messageContext) {
        MessageService ms = BundleUtils.getOsgiService(MessageService.class, null);

        if (ms != null) {
            for (CustomMessage m : ms.getAllMessages()) {
                m.addMessageToContext(messageContext);
            }
        }
    }

    public void initModules(RequestContext requestContext, RenderContext renderContext) {
        // generate tables ids, used by datatable jquery plugin to store the state of a table in the user localestorage.
        // new flow = new ids
        addCustomMessages(requestContext.getMessageContext());
        reloadTablesUUIDFromSession(requestContext);
        if (!requestContext.getFlowScope().contains("adminModuleTableUUID")) {
            requestContext.getFlowScope().put("adminModuleTableUUID", UUID.randomUUID().toString());
            requestContext.getFlowScope().put("forgeModuleTableUUID", UUID.randomUUID().toString());
        }

        if (!isStudio(renderContext)) {
            forgeService.loadModules();
            final Long startedBundleId = (Long) requestContext.getExternalContext().getSessionMap().get(
                    "moduleHasBeenStarted");
            if (startedBundleId != null) {
                Bundle b = BundleUtils.getBundle(startedBundleId);
                JahiaTemplatesPackage module = BundleUtils.getModule(b);
                String msgKey = "serverSettings.manageModules.module.started";
                if (module != null && module.getState().getState() == ModuleState.State.WAITING_TO_BE_IMPORTED) {
                    msgKey = "serverSettings.manageModules.start.waitingToBeImported";
                }
                requestContext.getMessageContext().addMessage(new MessageBuilder().info().source(Long.toString(startedBundleId))
                        .code(msgKey).arg(b.getSymbolicName()).build());
                requestContext.getExternalContext().getSessionMap().remove("moduleHasBeenStarted");
            }
            final Object stoppedBundleId = requestContext.getExternalContext().getSessionMap().get(
                    "moduleHasBeenStopped");
            if (stoppedBundleId != null) {
                requestContext.getMessageContext().addMessage(new MessageBuilder().info().source(stoppedBundleId).code("serverSettings.manageModules.module.stopped").arg(stoppedBundleId).build());
                requestContext.getExternalContext().getSessionMap().remove("moduleHasBeenStopped");
            }
            @SuppressWarnings("unchecked") final List<String> missingDependencies = (List<String>) requestContext.getExternalContext().getSessionMap().get("missingDependencies");
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
        OperationConstraintsService opConstraintsService = BundleUtils.getOsgiService(OperationConstraintsService.class, null);
        List<Module> installedModule = new ArrayList<>();
        List<Module> newModules = new ArrayList<>();
        for (Module module : forgeService.getModules()) {
            org.osgi.framework.Version osgiVersion = new org.osgi.framework.Version(JahiaDepends.toOsgiVersion(module.getVersion()));
            OperationConstraints ops = opConstraintsService.getConstraintForBundle(module.getId(), osgiVersion);
            module.setInstallable(!templateManagerService.differentModuleWithSameIdExists(module.getId(), module.getGroupId()) && (ops == null || ops.canDeploy(osgiVersion)));

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

    public void handleError(Exception exception, MutableAttributeMap<?> flowScope, MessageContext messageContext) {
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

    public void startModule(String moduleId, String version, RequestContext requestContext)
            throws RepositoryException, BundleException {
        Bundle bundle = BundleUtils.getBundle(moduleId, version);
        moduleManager.start(new BundleInfo(BundleUtils.getModuleGroupId(bundle), bundle.getSymbolicName(),
                bundle.getVersion().toString()).getKey(), null);

        if (BundleUtils.getBundle(moduleId, version) == null) {
            // Bundle somehow disappear ...
            throw new ModuleManagementException("Module '" + moduleId + " (" + version + ")' could not be started. " +
                    "Something went wrong during the startup.");
        }

        if (bundle.getState() == Bundle.ACTIVE) {
            requestContext.getExternalContext().getSessionMap().put("moduleHasBeenStarted", bundle.getBundleId());
        }
        storeTablesUUID(requestContext);
    }

    public void stopModule(String moduleId, RequestContext requestContext) throws RepositoryException, BundleException {
        JahiaTemplatesPackage module = templatePackageRegistry.lookupById(moduleId);
        if (module != null) {
            moduleManager.stop(module.getBundleKey(), null);
        } else {
            Bundle bundle = BundleUtils.getBundleBySymbolicName(moduleId, null);
            throw new ModuleManagementException(bundle == null || bundle.getState() == Bundle.ACTIVE
                    ? "Module '" + moduleId + "' could not stopped as it was not found in the registry."
                    : "Module '" + moduleId + "' was already stopped");
        }
        requestContext.getExternalContext().getSessionMap().put("moduleHasBeenStopped", moduleId);
        storeTablesUUID(requestContext);
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
        return typeNames.toArray(new String[0]);
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

    public void validateScmInfo(String scmUri, String branchOrTag, String moduleVersion, MutableAttributeMap<Object> flowScope) throws IOException {
        if ((StringUtils.startsWith(scmUri, "scm:git:") && StringUtils.isBlank(branchOrTag))
                || (StringUtils.startsWith(scmUri, "scm:svn:") && StringUtils.contains(scmUri, "/trunk/"))) {
            Map<String, String> branchTagInfos = listBranchOrTags(moduleVersion, scmUri);
            flowScope.put("branchTagInfos", branchTagInfos);
            branchOrTag = guessBranchOrTag(moduleVersion, scmUri, branchTagInfos, null);
            flowScope.put("branchOrTag", branchOrTag);
        }
    }

    public JCRNodeWrapper checkoutModule(MutableAttributeMap<Object> flowScope, JCRSessionWrapper session) throws RepositoryException, XmlPullParserException, DocumentException, IOException, BundleException {
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

    public File checkoutTempModule(MutableAttributeMap<Object> flowScope) throws RepositoryException, XmlPullParserException, DocumentException, IOException {
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

    public void updateModule(String id, String version) throws RepositoryException {
        Bundle bundle = BundleUtils.getBundle(id, version);
        if (bundle != null) {
            try {
                bundle.update();
            } catch (BundleException e) {
                logger.error("Cannot update module", e);
            }
        }
    }

    public void refreshModule(String id, String version) throws RepositoryException {
        Bundle bundle = BundleUtils.getBundle(id, version);
        Map<String, ModuleWiring> oldWirings = ModuleWiring.getWirings(bundle);
        moduleManager.refresh(BundleInfo.fromBundle(bundle).getKey(), null);
        logWiringDiff(bundle, oldWirings);
    }

    private void logWiringDiff(Bundle bundle, final Map<String, ModuleWiring> oldWirings) {
        Collection<ModuleWiring> wiringDiff = null;
        Map<String, ModuleWiring> newWirings = null;

        if (oldWirings != null) {
            newWirings = ModuleWiring.getWirings(bundle);
            if (newWirings != null) {
                wiringDiff = CollectionUtils.disjunction(oldWirings.values(), newWirings.values());
            }
        }

        if (wiringDiff != null && !wiringDiff.isEmpty()) {
            logger.error("Bundle wiring for bundle {} has been refreshed with wiring differences:", bundle);
            // Collect and log latest wiring states
            Collection<String> changedWirings = wiringDiff.stream()
                    .map(ModuleWiring::getCapabilityName)
                    .collect(Collectors.toSet());
            for (String c: changedWirings) {
                logger.error("Changed package wiring - BEFORE: {}, AFTER: {}", oldWirings.get(c), newWirings.get(c));
            }
        } else if (oldWirings != null && newWirings != null && logger.isDebugEnabled()) {
            logger.debug("No wiring differences for bundle {}", bundle);
        }
    }

    public void uninstallModule(String moduleId, String moduleVersion, RequestContext requestContext) throws RepositoryException, BundleException {
        moduleManager.uninstall(BundleInfo.fromModuleInfo(moduleId, moduleVersion).getKey(), null);
    }

    private static class ModuleInstallationResult {

        private final Bundle bundle;
        private final String messageCode;

        public ModuleInstallationResult(Bundle bundle, String messageCode) {
            this.bundle = bundle;
            this.messageCode = messageCode;
        }

        public Bundle getBundle() {
            return bundle;
        }

        public String getMessageCode() {
            return messageCode;
        }
    }
}
