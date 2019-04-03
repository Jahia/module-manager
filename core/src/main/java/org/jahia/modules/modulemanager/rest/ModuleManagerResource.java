/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.modulemanager.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.data.templates.ModuleState;
import org.jahia.data.templates.ModuleState.State;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.osgi.BundleState;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.modulemanager.*;
import org.jahia.services.modulemanager.spi.BundleService;
import org.jahia.services.modulemanager.spi.BundleService.FragmentInformation;
import org.jahia.settings.readonlymode.ReadOnlyModeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * The REST service implementation for module manager API.
 *
 * @author bdjiba
 */
@Path("/api/bundles")
@Produces({ MediaType.APPLICATION_JSON })
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

    private Resource getUploadedFileAsResource(InputStream inputStream, String filename)
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
        return new FileSystemResource(tempFile);
    }

    /**
     * Install the given bundle on the specified group of nodes. If nodes parameter is empty then deploy to default group.
     *
     * @param bundleParts parts of the request body containing binaries of modules to install
     * @param target the group of cluster nodes targeted by the install operation
     * @param start whether the installed bundle should be started right away
     * @return the operation result
     * @throws WebApplicationException when the operation fails
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response install(@FormDataParam("bundle")List<FormDataBodyPart> bundleParts, @FormDataParam("target") String target, @FormDataParam("start") boolean start) throws WebApplicationException {

        if (bundleParts == null || bundleParts.isEmpty()) {
            throw new ClientErrorException("At least one bundle file is required", Response.Status.BAD_REQUEST);
        }

        long startTime = System.currentTimeMillis();
        log.info("Received request to install {} bundles on target {}. Should start the bundles after: {}",
                new Object[] { bundleParts.size(), target, start });

        try {

            ArrayList<Resource> bundleResources = new ArrayList<>(bundleParts.size());

            try {

                for (FormDataBodyPart bundlePart : bundleParts) {
                    FormDataContentDisposition fileDisposition = bundlePart.getFormDataContentDisposition();
                    InputStream bundleInputStream = bundlePart.getValueAs(InputStream.class);
                    try {
                        Resource bundleResource = getUploadedFileAsResource(bundleInputStream, fileDisposition.getFileName());
                        bundleResources.add(bundleResource);
                    } finally {
                        try {
                            bundleInputStream.close();
                        } catch (IOException e) {
                            throw new JahiaRuntimeException(e);
                        }
                    }
                }

                OperationResult result = getModuleManager().install(bundleResources, target, start);
                return Response.ok(result).build();
            } finally {
                for (Resource bundleResource : bundleResources) {
                    try {
                        File bundleFile = bundleResource.getFile();
                        bundleFile.delete();
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Unable to clean installed bundle file. Cause: " + e.getMessage(), e);
                        } else {
                            log.warn("Unable to clean installed bundle file (details in DEBUG logging level). Cause: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (ModuleManagementInvalidArgumentException e) {
            log.error("Unable to install module. Cause: " + e.getMessage());
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
     * Starts the specified bundle.
     *
     * @param bundleKey the bundle key
     * @param target the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws WebApplicationException in case of an error during start operation
     */
    @POST
    @Path("/{bundleKey:.*}/_start")
    public Response start(@PathParam("bundleKey") String bundleKey, @FormParam("target") String target)
            throws WebApplicationException {

        validateBundleKey(bundleKey, "start");
        log.info("Received request to start bundle {} on target {}", new Object[] { bundleKey, target });

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
                log.error("Error while starting bundle " + bundleKey, e);
                throw new InternalServerErrorException("Error while starting bundle " + bundleKey, e);
            }
        }
    }

    /**
     * Stops the specified bundle.
     *
     * @param bundleKey the bundle key
     * @param target the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws WebApplicationException in case of an error during stop operation
     */
    @POST
    @Path("/{bundleKey:.*}/_stop")
    public Response stop(@PathParam("bundleKey") String bundleKey, @FormParam("target") String target)
            throws WebApplicationException {

        validateBundleKey(bundleKey, "stop");
        log.info("Received request to stop bundle {} on target {}", new Object[] { bundleKey, target });

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
                log.error("Error while stopping bundle " + bundleKey, e);
                throw new InternalServerErrorException("Error while stopping bundle " + bundleKey, e);

            }
        }
    }

    /**
     * Uninstalls the specified bundle.
     *
     * @param bundleKey the bundle key
     * @param target the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws WebApplicationException in case of an error during uninstall operation
     */
    @POST
    @Path("/{bundleKey:.*}/_uninstall")
    public Response uninstall(@PathParam("bundleKey") String bundleKey, @FormParam("target") String target)
            throws WebApplicationException {

        validateBundleKey(bundleKey, "stop");
        log.info("Received request to uninstall bundle {} on target {}", new Object[] { bundleKey, target });

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
                log.error("Error while uninstalling bundle " + bundleKey, e);
                throw new InternalServerErrorException("Error while uninstalling bundle " + bundleKey, e);
            }
        }
    }

    /**
     * Refresh the specified bundle.
     *
     * @param bundleKey the bundle key
     * @param target the group of cluster nodes targeted by this operation
     * @return the operation status
     * @throws WebApplicationException in case of an error during uninstall operation
     */
    @POST
    @Path("/{bundleKey:.*}/_refresh")
    public Response refresh(@PathParam("bundleKey") String bundleKey, @FormParam("target") String target)
            throws WebApplicationException {

        validateBundleKey(bundleKey, "refresh");
        log.info("Received request to refresh bundle {} on target {}", new Object[] { bundleKey, target });

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
     * @param target the group of cluster nodes targeted by this operation
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
     * @param target the group of cluster nodes targeted by this operation
     * @return a map of bundle info by cluster node name; each map value is either a nested map of BundleInfoDto by bundle key or a ModuleManagerExceptionMapper.ErrorInfo in case there was an error communicating with corresponding cluster node
     */
    @GET
    @Path(PATH_GET_INFOS + "_info")
    public Map<String, Object> getInfos(@PathParam("bundleKeys") String bundleKeys, @QueryParam("target") String target) {

        final LinkedHashSet<String> keys = new LinkedHashSet<String>();
        processBundleKeys(bundleKeys, new BundleKeyProcessor() {

            @Override
            public void process(String bundleKey) {
                keys.add(bundleKey);
            }
        });

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
     * @param target the group of cluster nodes targeted by this operation
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
        BundleState state = getLocalBundleState(getModuleManager(), bundleKey);
        return state;
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
        final LinkedHashMap<String, BundleState> stateByKey = new LinkedHashMap<String, BundleState>();

        processBundleKeys(bundleKeys, new BundleKeyProcessor() {

            @Override
            public void process(String bundleKey) {
                BundleState state = getLocalBundleState(moduleManager, bundleKey);
                stateByKey.put(bundleKey, state);
            }
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
        BundleInfoDto info = getLocalBundleInfo(getModuleManager(), bundleKey);
        return info;
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
        final LinkedHashMap<String, BundleInfoDto> infoByKey = new LinkedHashMap<String, BundleInfoDto>();

        processBundleKeys(bundleKeys, new BundleKeyProcessor() {

            @Override
            public void process(String bundleKey) {
                BundleInfoDto info = getLocalBundleInfo(moduleManager, bundleKey);
                infoByKey.put(bundleKey, info);
            }
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

        Map<String, Object> result = new LinkedHashMap<String, Object>(infoByHost.size());
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
}
