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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.data.templates.ModuleState;
import org.jahia.data.templates.ModuleState.State;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.osgi.BundleState;
import org.jahia.osgi.FrameworkService;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.modulemanager.*;
import org.jahia.services.modulemanager.spi.BundleService;
import org.jahia.services.modulemanager.spi.BundleService.FragmentInformation;
import org.jahia.settings.readonlymode.ReadOnlyModeException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The REST service implementation for module manager API.
 *
 * @author bdjiba
 */
@Path("/api/bundles")
@Produces({MediaType.APPLICATION_JSON})
public class ModuleManagerResource {

    private static final String PATH_GET_INFO = "/{bundleKey:[^\\[\\]\\*]+}/";
    private static final String PATH_GET_INFOS = "/[{bundleKeys:[^\\[\\]\\*]*}]/";
    private static final String PATH_GET_BUCKET_INFOS = "/{bundleBucketKey:[^\\[\\]]+}/*/";
    private static final String PATH_GET_ALL_INFOS = "/*/";

    /**
     * OSGi bundle type.
     */
    public enum BundleType {

        /**
         * Regular OSGi bundle.
         */
        BUNDLE,

        /**
         * OSGi bundle fragment.
         */
        FRAGMENT,

        /**
         * OSGi bundle, which is a DX module in addition.
         */
        MODULE
    }

    /**
     * A (part of) REST response representing info about a bundle.
     */
    public static class BundleInfoDto {

        private BundleType type;
        private BundleState osgiState;
        private ModuleState.State moduleState;

        /**
         * Create an instance representing info about a standalone OSGi bundle.
         */
        public BundleInfoDto(BundleState osgiState) {
            this(osgiState, false);
        }

        /**
         * Create an instance representing info about an OSGi bundle (standard or fragment).
         */
        public BundleInfoDto(BundleState osgiState, boolean isFragment) {
            this(isFragment ? BundleType.FRAGMENT : BundleType.BUNDLE, osgiState, null);
        }

        /**
         * Create an instance representing info about an OSGi bundle which is a DX module.
         */
        public BundleInfoDto(BundleState osgiState, State moduleState) {
            this(BundleType.MODULE, osgiState, moduleState);
        }

        private BundleInfoDto(BundleType type, BundleState osgiState, State moduleState) {
            this.type = type;
            this.osgiState = osgiState;
            this.moduleState = moduleState;
        }

        /**
         * @return bundle type
         */
        public BundleType getType() {
            return type;
        }

        /**
         * @return state of the bundle in OSGi terms
         */
        public BundleState getOsgiState() {
            return osgiState;
        }

        /**
         * @return state of the module bundle in DX terms; null in case the bundle is not a DX module
         */
        public ModuleState.State getModuleState() {
            return moduleState;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ModuleManagerResource.class);

    private ModuleManager getModuleManager() {
        return (ModuleManager) SpringContextSingleton.getBean("ModuleManager");
    }

    private UploadedBundle getUploadedFileAsResource(InputStream inputStream, String filename)
            throws InternalServerErrorException {

        File tempFile;
        try {
            tempFile = File.createTempFile(FilenameUtils.getBaseName(filename) + "-",
                                           "." + FilenameUtils.getExtension(filename), FileUtils.getTempDirectory());
            FileUtils.copyInputStreamToFile(inputStream, tempFile);
        } catch (IOException e) {
            log.error("Error copy uploaded stream to local temp file for " + filename, e);
            throw new InternalServerErrorException("Error while deploying bundle " + filename, e);
        }
        BundleContext bundleContext = FrameworkService.getBundleContext();
        Resource bundleResource = new FileSystemResource(tempFile);
        try {
            Collection<ServiceReference<ArtifactUrlTransformer>> serviceReferences = bundleContext.getServiceReferences(ArtifactUrlTransformer.class, null);
            for (ServiceReference<ArtifactUrlTransformer> serviceReference : serviceReferences) {
                ArtifactUrlTransformer transformer = bundleContext.getService(serviceReference);
                if (transformer.canHandle(bundleResource.getFile())) {
                    URL transformedURL = transformer.transform(new URL("file:"+bundleResource.getFile().getPath()));
                    bundleResource = new UrlResource(transformedURL);
                }

            }
        } catch (Exception e) {
            log.warn("Could not transform the uploaded file {}", e.getMessage());
        }
        return new UploadedBundle(bundleResource, tempFile);
    }

    /**
     * Install the given bundle on the specified group of nodes. If nodes parameter is empty then deploy to default group.
     *
     * @param bundleParts parts of the request body containing binaries of modules to install
     * @param target      the group of cluster nodes targeted by the install operation
     * @param start       whether the installed bundle should be started right away
     * @return the operation result
     * @throws WebApplicationException when the operation fails
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response install(@FormDataParam("bundle") List<FormDataBodyPart> bundleParts, @FormDataParam("target") String target, @FormDataParam("start") boolean start) throws WebApplicationException {

        if (bundleParts == null || bundleParts.isEmpty()) {
            throw new ClientErrorException("At least one bundle file is required", Response.Status.BAD_REQUEST);
        }

        long startTime = System.currentTimeMillis();
        log.info("Received request to install {} bundles on target {}. Should start the bundles after: {}",
                 bundleParts.size(), target, start);

        try {

            ArrayList<UploadedBundle> bundleResources = new ArrayList<>(bundleParts.size());

            try {

                for (FormDataBodyPart bundlePart : bundleParts) {
                    FormDataContentDisposition fileDisposition = bundlePart.getFormDataContentDisposition();
                    InputStream bundleInputStream = bundlePart.getValueAs(InputStream.class);
                    try {
                        UploadedBundle bundleResource = getUploadedFileAsResource(bundleInputStream, fileDisposition.getFileName());
                        bundleResources.add(bundleResource);
                    } finally {
                        IOUtils.closeQuietly(bundleInputStream);
                    }
                }

                OperationResult result = getModuleManager().install(bundleResources.stream().map(UploadedBundle::getBundleResource).collect(Collectors.toList()),
                                                                    target, start);
                return Response.ok(result).build();
            } finally {
                for (UploadedBundle bundleResource : bundleResources) {
                        File bundleFile = bundleResource.getTempFile();
                        FileUtils.deleteQuietly(bundleFile);
                }
            }
        } catch (ModuleManagementInvalidArgumentException e) {
            log.error("Unable to install module. Cause: {}", e.getMessage());
            throw new ClientErrorException("Unable to install module. Cause: " + e.getMessage(), Response.Status.BAD_REQUEST, e);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof ReadOnlyModeException) {
                throw new ServerErrorException(cause.getMessage(), Response.Status.SERVICE_UNAVAILABLE, e);
            } else {
                log.error("Module management exception when installing module", e);
                throw new InternalServerErrorException("Error while installing bundle", e);
            }
        } finally {
            log.info("Operation completed in {} ms", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Updates the specified bundle.
     *
     * @param bundleKey the bundle key
     * @param target    the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws WebApplicationException in case of an error during update operation
     */
    @POST
    @Path("/{bundleKey:.*}/_update")
    public Response update(@PathParam("bundleKey") String bundleKey, @FormParam("target") String target)
            throws WebApplicationException {

        validateBundleKey(bundleKey, "update");
        log.info("Received request to update bundle {} on target {}", bundleKey, target);

        try {
            OperationResult result = getModuleManager().update(bundleKey, target);
            return Response.ok(result).build();
        } catch (ModuleNotFoundException e) {
            throw new ClientErrorException(e.getMessage(), Status.NOT_FOUND, e);
        } catch (ModuleManagementInvalidArgumentException e) {
            throw new ClientErrorException(e.getMessage(), Status.BAD_REQUEST, e);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof ReadOnlyModeException) {
                throw new ServerErrorException(cause.getMessage(), Response.Status.SERVICE_UNAVAILABLE, e);
            } else {
                log.error("Error while updating bundle {}", bundleKey, e);
                throw new InternalServerErrorException("Error while updating bundle " + bundleKey, e);
            }
        }
    }

    /**
     * Starts the specified bundle.
     *
     * @param bundleKey the bundle key
     * @param target    the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws WebApplicationException in case of an error during start operation
     */
    @POST
    @Path("/{bundleKey:.*}/_start")
    public Response start(@PathParam("bundleKey") String bundleKey, @FormParam("target") String target)
            throws WebApplicationException {

        validateBundleKey(bundleKey, "start");
        log.info("Received request to start bundle {} on target {}", bundleKey, target);

        try {
            OperationResult result = getModuleManager().start(bundleKey, target);
            return Response.ok(result).build();
        } catch (ModuleNotFoundException e) {
            throw new ClientErrorException(e.getMessage(), Status.NOT_FOUND, e);
        } catch (ModuleManagementInvalidArgumentException e) {
            throw new ClientErrorException(e.getMessage(), Status.BAD_REQUEST, e);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof ReadOnlyModeException) {
                throw new ServerErrorException(cause.getMessage(), Response.Status.SERVICE_UNAVAILABLE, e);
            } else {
                log.error("Error while starting bundle {}", bundleKey, e);
                throw new InternalServerErrorException("Error while starting bundle " + bundleKey, e);
            }
        }
    }

    /**
     * Stops the specified bundle.
     *
     * @param bundleKey the bundle key
     * @param target    the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws WebApplicationException in case of an error during stop operation
     */
    @POST
    @Path("/{bundleKey:.*}/_stop")
    public Response stop(@PathParam("bundleKey") String bundleKey, @FormParam("target") String target)
            throws WebApplicationException {

        validateBundleKey(bundleKey, "stop");
        log.info("Received request to stop bundle {} on target {}", bundleKey, target);

        try {
            OperationResult result = getModuleManager().stop(bundleKey, target);
            return Response.ok(result).build();
        } catch (ModuleNotFoundException e) {
            throw new ClientErrorException(e.getMessage(), Status.NOT_FOUND, e);
        } catch (ModuleManagementInvalidArgumentException e) {
            throw new ClientErrorException(e.getMessage(), Status.BAD_REQUEST, e);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof ReadOnlyModeException) {
                throw new ServerErrorException(cause.getMessage(), Response.Status.SERVICE_UNAVAILABLE, e);
            } else {
                log.error("Error while stopping bundle {}", bundleKey, e);
                throw new InternalServerErrorException("Error while stopping bundle " + bundleKey, e);

            }
        }
    }

    /**
     * Uninstalls the specified bundle.
     *
     * @param bundleKey the bundle key
     * @param target    the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws WebApplicationException in case of an error during uninstall operation
     */
    @POST
    @Path("/{bundleKey:.*}/_uninstall")
    public Response uninstall(@PathParam("bundleKey") String bundleKey, @FormParam("target") String target)
            throws WebApplicationException {

        validateBundleKey(bundleKey, "stop");
        log.info("Received request to uninstall bundle {} on target {}", bundleKey, target);

        try {
            OperationResult result = getModuleManager().uninstall(bundleKey, target);
            return Response.ok(result).build();
        } catch (ModuleNotFoundException e) {
            throw new ClientErrorException(e.getMessage(), Status.NOT_FOUND, e);
        } catch (ModuleManagementInvalidArgumentException e) {
            throw new ClientErrorException(e.getMessage(), Status.BAD_REQUEST, e);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof ReadOnlyModeException) {
                throw new ServerErrorException(cause.getMessage(), Response.Status.SERVICE_UNAVAILABLE, e);
            } else {
                log.error("Error while uninstalling bundle {}", bundleKey, e);
                throw new InternalServerErrorException("Error while uninstalling bundle " + bundleKey, e);
            }
        }
    }

    /**
     * Refreshes the specified bundle.
     *
     * @param bundleKey the bundle key
     * @param target    the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws WebApplicationException in case of an error during refresh operation
     */
    @POST
    @Path("/{bundleKey:.*}/_refresh")
    public Response refresh(@PathParam("bundleKey") String bundleKey, @FormParam("target") String target)
            throws WebApplicationException {

        validateBundleKey(bundleKey, "refresh");
        log.info("Received request to refresh bundle {} on target {}", bundleKey, target);

        try {
            OperationResult result = getModuleManager().refresh(bundleKey, target);
            return Response.ok(result).build();
        } catch (ModuleNotFoundException e) {
            throw new ClientErrorException(e.getMessage(), Status.NOT_FOUND, e);
        } catch (ModuleManagementInvalidArgumentException e) {
            throw new ClientErrorException(e.getMessage(), Status.BAD_REQUEST, e);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof ReadOnlyModeException) {
                throw new ServerErrorException(cause.getMessage(), Response.Status.SERVICE_UNAVAILABLE, e);
            } else {
                log.error("Error while refreshing bundle " + bundleKey, e);
                throw new InternalServerErrorException("Error while refreshing bundle " + bundleKey, e);
            }
        }
    }

    /**
     * Get info about a single bundle.
     *
     * @param bundleKey the bundle key
     * @param target    the group of cluster nodes targeted by this operation
     * @return a map of bundle info by cluster node name; each map value is either a BundleInfoDto object or a ModuleManagerExceptionMapper.ErrorInfo in case there was an error communicating with corresponding cluster node
     */
    @GET
    @Path(PATH_GET_INFO + "_info")
    public Map<String, Object> getInfo(@PathParam("bundleKey") String bundleKey, @QueryParam("target") String target) {

        validateBundleKey(bundleKey, "getInfo");

        return getBundleInfo(bundleKey, target, new BundleInfoRetrievalHandler<String, BundleService.BundleInformation, BundleInfoDto>() {

            @Override
            public Map<String, BundleService.BundleInformation> getBundleInfo(String bundleKey, String target) {
                return getModuleManager().getInfo(bundleKey, target);
            }

            @Override
            public BundleInfoDto getBundleInfoDto(BundleService.BundleInformation bundleInfo) {
                return bundleInfoToDto(bundleInfo);
            }
        });
    }

    /**
     * Get info about multiple bundles.
     *
     * @param bundleKeys comma separated list of bundle keys
     * @param target     the group of cluster nodes targeted by this operation
     * @return a map of bundle info by cluster node name; each map value is either a nested map of BundleInfoDto by bundle key or a ModuleManagerExceptionMapper.ErrorInfo in case there was an error communicating with corresponding cluster node
     */
    @GET
    @Path(PATH_GET_INFOS + "_info")
    public Map<String, Object> getInfos(@PathParam("bundleKeys") String bundleKeys, @QueryParam("target") String target) {

        final LinkedHashSet<String> keys = new LinkedHashSet<>();
        processBundleKeys(bundleKeys, keys::add);

        return getBundleInfo(keys, target, new BundleInfoRetrievalHandler<Collection<String>, Map<String, BundleService.BundleInformation>, Map<String, BundleInfoDto>>() {

            @Override
            public Map<String, Map<String, BundleService.BundleInformation>> getBundleInfo(Collection<String> bundleKeys, String target) {
                return getModuleManager().getInfos(bundleKeys, target);
            }

            @Override
            public Map<String, BundleInfoDto> getBundleInfoDto(Map<String, BundleService.BundleInformation> bundleInfoByBundleKey) {
                return bundleInfosToDtos(bundleInfoByBundleKey);
            }
        });
    }

    /**
     * Get info about multiple bundles sharing a single bundle group/name.
     *
     * @param bundleBucketKey the bundle group/name
     * @param target          the group of cluster nodes targeted by this operation
     * @return a map of bundle info by cluster node name; each map value is either a nested map of BundleInfoDto by bundle key or a ModuleManagerExceptionMapper.ErrorInfo in case there was an error communicating with corresponding cluster node
     */
    @GET
    @Path(PATH_GET_BUCKET_INFOS + "_info")
    public Map<String, Object> getBucketInfos(@PathParam("bundleBucketKey") String bundleBucketKey, @QueryParam("target") String target) {

        validateBundleBucketKey(bundleBucketKey, "getBucketInfos");

        return getBundleInfo(bundleBucketKey, target, new BundleInfoRetrievalHandler<String, Map<String, BundleService.BundleInformation>, Map<String, BundleInfoDto>>() {

            @Override
            public Map<String, Map<String, BundleService.BundleInformation>> getBundleInfo(String bundleBucketKey, String target) {
                return getModuleManager().getBucketInfos(bundleBucketKey, target);
            }

            @Override
            public Map<String, BundleInfoDto> getBundleInfoDto(Map<String, BundleService.BundleInformation> bundleInfoByBundleKey) {
                return bundleInfosToDtos(bundleInfoByBundleKey);
            }
        });
    }

    /**
     * Get info about all installed bundles.
     *
     * @param target the group of cluster nodes targeted by this operation
     * @return a map of bundle info by cluster node name; each map value is either a nested map of BundleInfoDto by bundle key or a ModuleManagerExceptionMapper.ErrorInfo in case there was an error communicating with corresponding cluster node
     */
    @GET
    @Path(PATH_GET_ALL_INFOS + "_info")
    public Map<String, Object> getAllInfos(@QueryParam("target") String target) {

        return getBundleInfo(null, target, new BundleInfoRetrievalHandler<Void, Map<String, BundleService.BundleInformation>, Map<String, BundleInfoDto>>() {

            @Override
            public Map<String, Map<String, BundleService.BundleInformation>> getBundleInfo(Void bundleKeys, String target) {
                return getModuleManager().getAllInfos(target);
            }

            @Override
            public Map<String, BundleInfoDto> getBundleInfoDto(Map<String, BundleService.BundleInformation> bundleInfoByBundleKey) {
                return bundleInfosToDtos(bundleInfoByBundleKey);
            }
        });
    }

    /**
     * Get current local state of a single bundle.
     *
     * @param bundleKey the bundle key
     * @return current bundle state
     */
    @GET
    @Path("/{bundleKey:[^\\[\\]]*}/_localState")
    public BundleState getLocalState(@PathParam("bundleKey") String bundleKey) {
        validateBundleKey(bundleKey, "getLocalState");
        return getLocalBundleState(getModuleManager(), bundleKey);
    }

    /**
     * Get current local states of multiple bundles.
     *
     * @param bundleKeys comma separated list of bundle keys
     * @return a map of bundle states by bundle keys
     */
    @GET
    @Path("/[{bundleKeys:.*}]/_localState")
    public Map<String, BundleState> getLocalStates(@PathParam("bundleKeys") String bundleKeys) {

        final ModuleManager moduleManager = getModuleManager();
        final LinkedHashMap<String, BundleState> stateByKey = new LinkedHashMap<>();

        processBundleKeys(bundleKeys, bundleKey -> {
            stateByKey.put(bundleKey, getLocalBundleState(moduleManager, bundleKey));
        });

        return stateByKey;
    }

    /**
     * Get local info about a single bundle.
     *
     * @param bundleKey the bundle key
     * @return local bundle info
     */
    @GET
    @Path(PATH_GET_INFO + "_localInfo")
    public BundleInfoDto getLocalInfo(@PathParam("bundleKey") String bundleKey) {
        validateBundleKey(bundleKey, "getLocalInfo");
        return getLocalBundleInfo(getModuleManager(), bundleKey);
    }

    /**
     * Get local info about multiple bundles.
     *
     * @param bundleKeys comma separated list of bundle keys
     * @return a map of bundle info by bundle keys
     */
    @GET
    @Path(PATH_GET_INFOS + "_localInfo")
    public Map<String, BundleInfoDto> getLocalInfos(@PathParam("bundleKeys") String bundleKeys) {

        final ModuleManager moduleManager = getModuleManager();
        final LinkedHashMap<String, BundleInfoDto> infoByKey = new LinkedHashMap<>();

        processBundleKeys(bundleKeys, bundleKey -> {
            BundleInfoDto info = getLocalBundleInfo(moduleManager, bundleKey);
            infoByKey.put(bundleKey, info);
        });

        return infoByKey;
    }

    /**
     * Get local info about multiple bundles sharing a single bundle group/name.
     *
     * @param bundleBucketKey the bundle group/name
     * @return a map of bundle info by bundle keys
     */
    @GET
    @Path(PATH_GET_BUCKET_INFOS + "_localInfo")
    public Map<String, BundleInfoDto> getBucketLocalInfos(@PathParam("bundleBucketKey") String bundleBucketKey) {
        validateBundleBucketKey(bundleBucketKey, "getBucketLocalInfos");
        Map<String, BundleService.BundleInformation> bundleInfoByKey = getModuleManager().getBucketLocalInfos(bundleBucketKey);
        return bundleInfosToDtos(bundleInfoByKey);
    }

    /**
     * Get local info about all installed bundles.
     *
     * @return a map of bundle info by bundle keys
     */
    @GET
    @Path(PATH_GET_ALL_INFOS + "_localInfo")
    public Map<String, BundleInfoDto> getAllLocalInfos() {
        Map<String, BundleService.BundleInformation> bundleInfoByKey = getModuleManager().getAllLocalInfos();
        return bundleInfosToDtos(bundleInfoByKey);
    }

    /**
     * Store persistent state of all bundles in the internal storage for the purpose of restore in the future.
     *
     * @return return A collection of info objects describing the bundles whose persistent state have been stored
     */
    @POST
    @Path("/_storeAllLocalPersistentStates")
    public Collection<BundlePersistentInfo> storeAllLocalPersistentStates() {
        long startTime = System.currentTimeMillis();
        log.info("Received request to store bundle states");
        try {
            return getModuleManager().storeAllLocalPersistentStates();
        } catch (NonProcessingNodeException e) {
            throw new ClientErrorException(e.getMessage(), Response.Status.FORBIDDEN, e);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof ReadOnlyModeException) {
                throw new ServerErrorException(cause.getMessage(), Response.Status.SERVICE_UNAVAILABLE, e);
            } else {
                log.error("Module management exception when storing bundle states", e);
                throw new InternalServerErrorException("Error while storing bundle states", e);
            }
        } finally {
            log.info("Operation completed in {} ms", System.currentTimeMillis() - startTime);
        }
    }

    private static void validateBundleBucketKey(String bundleBucketKey, String operation) throws ClientErrorException {
        if (StringUtils.isBlank(bundleBucketKey)) {
            throw new ClientErrorException("Bundle bucket key is mandatory for " + operation + " operation.", Status.BAD_REQUEST);
        }
    }

    private static void validateBundleKey(String bundleKey, String operation) throws ClientErrorException {
        if (StringUtils.isBlank(bundleKey)) {
            throw new ClientErrorException("Bundle key is mandatory for " + operation + " operation.", Status.BAD_REQUEST);
        }
    }

    private static <K, I, D> Map<String, Object> getBundleInfo(K bundleKeys, String target, BundleInfoRetrievalHandler<K, I, D> infoRetrievalHandler) {

        Map<String, I> infoByHost;
        try {
            infoByHost = infoRetrievalHandler.getBundleInfo(bundleKeys, target);
        } catch (InvalidModuleKeyException | InvalidTargetException e) {
            throw new ClientErrorException(e.getMessage(), Status.BAD_REQUEST);
        }

        Map<String, Object> result = new LinkedHashMap<>(infoByHost.size());
        for (Map.Entry<String, I> entry : infoByHost.entrySet()) {
            String hostName = entry.getKey();
            I bundleInfo = entry.getValue();
            try {
                D bundleInfoDto = infoRetrievalHandler.getBundleInfoDto(bundleInfo);
                result.put(hostName, bundleInfoDto);
            } catch (ModuleManagementException e) {
                Throwable cause = ExceptionUtils.getRootCause(e);
                ErrorInfoDto errorInfo = new ErrorInfoDto(e.getMessage(), (cause == null ? null : cause.toString()));
                result.put(hostName, errorInfo);
            }
        }

        return result;
    }

    private interface BundleInfoRetrievalHandler<K, I, D> {

        Map<String, I> getBundleInfo(K bundleKeys, String target);

        D getBundleInfoDto(I bundleInfo);
    }

    private static BundleState getLocalBundleState(ModuleManager moduleManager, String bundleKey) {
        try {
            return moduleManager.getLocalState(bundleKey);
        } catch (InvalidModuleKeyException e) {
            throw new ClientErrorException(e.getMessage(), Status.BAD_REQUEST);
        } catch (ModuleNotFoundException e) {
            throw new ClientErrorException(e.getMessage(), Status.NOT_FOUND);
        }
    }

    private static BundleInfoDto getLocalBundleInfo(ModuleManager moduleManager, String bundleKey) {

        BundleService.BundleInformation info;
        try {
            info = moduleManager.getLocalInfo(bundleKey);
        } catch (InvalidModuleKeyException e) {
            throw new ClientErrorException(e.getMessage(), Status.BAD_REQUEST);
        } catch (ModuleNotFoundException e) {
            throw new ClientErrorException(e.getMessage(), Status.NOT_FOUND);
        }

        return bundleInfoToDto(info);
    }

    private static BundleInfoDto bundleInfoToDto(BundleService.BundleInformation info) {
        if (info instanceof BundleService.ModuleInformation) {
            ModuleState.State moduleState = ((BundleService.ModuleInformation) info).getModuleState();
            return new BundleInfoDto(info.getOsgiState(), moduleState);
        } else {
            return new BundleInfoDto(info.getOsgiState(), info instanceof FragmentInformation);
        }
    }

    private static Map<String, BundleInfoDto> bundleInfosToDtos(Map<String, BundleService.BundleInformation> bundleInfoByKey) {
        LinkedHashMap<String, BundleInfoDto> infoByKey = new LinkedHashMap<>(bundleInfoByKey.size());
        for (Map.Entry<String, BundleService.BundleInformation> entry : bundleInfoByKey.entrySet()) {
            String bundleKey = entry.getKey();
            BundleService.BundleInformation bundleInfo = entry.getValue();
            infoByKey.put(bundleKey, bundleInfoToDto(bundleInfo));
        }
        return infoByKey;
    }

    private static void processBundleKeys(String bundleKeys, BundleKeyProcessor bundleKeyProcessor) {
        String[] keys = StringUtils.split(bundleKeys, ',');
        for (String key : keys) {
            key = key.trim();
            if (StringUtils.isEmpty(key)) {
                continue;
            }
            bundleKeyProcessor.process(key);
        }
    }

    private interface BundleKeyProcessor {

        void process(String bundleKey);
    }

    private static class ErrorInfoDto {

        private final String message;
        private final String cause;

        public ErrorInfoDto(String message, String cause) {
            this.message = message;
            this.cause = cause;
        }

        @SuppressWarnings("unused")
        public String getMessage() {
            return message;
        }

        @SuppressWarnings("unused")
        public String getCause() {
            return cause;
        }
    }

    private class UploadedBundle {
        private final Resource bundleResource;
        private final File tempFile;

        public UploadedBundle(Resource bundleResource, File tempFile) {
            this.bundleResource = bundleResource;
            this.tempFile = tempFile;
        }

        public Resource getBundleResource() {
            return bundleResource;
        }

        public File getTempFile() {
            return tempFile;
        }
    }
}
