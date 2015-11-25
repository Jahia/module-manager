package org.jahia.modules.modulemanager.impl;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.felix.framework.cache.BundleArchive;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.poi.util.IOUtils;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.jahia.modules.modulemanager.model.BinaryFile;
import org.jahia.modules.modulemanager.model.Bundle;
import org.jahia.modules.modulemanager.model.ClusterNode;
import org.jahia.modules.modulemanager.model.ModuleManagement;
import org.jahia.modules.modulemanager.model.NodeBundle;
import org.jahia.osgi.BundleUtils;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.DigestInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bundle Service Implementation to manage the cluster nodes bundles.
 * 
 * @author achaabni
 */
public class BundleServiceImpl implements BundleContextAware {

    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(BundleServiceImpl.class);

    /**
     * bundle context
     */
    private BundleContext bundleContext;

    /**
     * Get local bundles from Context
     *
     * @return local bundles with states
     * @throws RepositoryException
     */
    private Map<Bundle, String> getLocalBundles() throws RepositoryException {
        Map<Bundle, String> result = new HashMap<>();
        BundleArchive[] archives = null;
        // Get Felix Bundle Archives.
        try {
            archives = getBundleArchives();
        } catch (Exception e) {
            logger.error(e.getMessage());
            // Continue
        }

        for (org.osgi.framework.Bundle contextBundle : bundleContext.getBundles()) {
            if (contextBundle.getHeaders().get("Jahia-Module-Type") != null || contextBundle.getHeaders().get("Jahia-Cluster-Deployment") != null) {
                Bundle bundleToAdd = new Bundle();
                String bundleLocation = contextBundle.getLocation();
                String fileName = null;
                URL bundleURL = null;
                try {
                    bundleURL = new URL(bundleLocation);
                } catch (MalformedURLException e) {
                    logger.error("Couldn't resolve bundle URL " + bundleLocation, e);
                    continue;
                }
                try {
                    fileName = FilenameUtils.getName(bundleURL.getPath());
                    bundleToAdd.setFileName(fileName);
                    DigestInputStream digestInputStream = getBundleInputStream(bundleURL, fileName, archives);
                    BinaryFile file = new BinaryFile(IOUtils.toByteArray(digestInputStream));
                    bundleToAdd.setFile(file);
                    bundleToAdd.setSymbolicName(contextBundle.getHeaders().get("Bundle-SymbolicName"));
                    bundleToAdd.setDisplayName(contextBundle.getHeaders().get("Bundle-Name"));
                    String version = contextBundle.getHeaders().get("Implementation-Version");
                    if (version == null) {
                        version = contextBundle.getHeaders().get("Bundle-Version");
                    }
                    bundleToAdd.setVersion(version);
                    bundleToAdd.setChecksum(Hex.encodeHexString(digestInputStream.getMessageDigest().digest()));
                    bundleToAdd.setName(bundleToAdd.getSymbolicName() + "-" + bundleToAdd.getVersion());
                    result.put(bundleToAdd, BundleUtils.getModule(contextBundle).getState().getState().toString().toLowerCase());
                } catch (IOException e) {
                    logger.error("Error storing bundle " + contextBundle + " in JCR", e);
                } catch (Exception e) {
                    logger.error("Error finding bundle file from history " + contextBundle , e);
                }
            }

        }
        return result;
    }

    /**
     * Get the digest input stream from an URL or from a list of bundle archives
     * @param bundleURL bundleUrl
     * @param fileName fileName
     * @param archives list of bundle archives
     * @return digest input stream
     */
    private DigestInputStream getBundleInputStream(URL bundleURL, String fileName, BundleArchive[] archives) {
        InputStream result = null;
        try {
            result = bundleURL.openStream();
        } catch (IOException e) {
            // unable to find original file
            logger.warn(e.getMessage(), e);
        }
        if (result == null) {
            try {
                BundleArchive archive = findArchiveByFileName(fileName, archives);
                if (archive != null) {
                    result = new URL(archive.getLocation()).openStream();
                }
            } catch (Exception e) {
                // cannot get it from the bundle archive
                logger.warn(e.getMessage(), e);
            }
        }
        
        return result != null ? ModuleManagerImpl.toDigestInputStream(result) : null;
    }

    /**
     * Find bundle archive by file name from a list of bundle archives
     * @param fileName file name to search the bundle with
     * @param archives list of bundle archives
     * @return the bundle archive
     */
    private BundleArchive findArchiveByFileName(String fileName, BundleArchive[] archives) {
        BundleArchive result = null;
        try {
            for (BundleArchive archive : archives) {
                if (archive.getLocation().contains(fileName)) {
                    result = archive;
                    break;
                }

            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
        return result;
    }

    /**
     * Get Bundle Archives from Bundle Cache.
     *
     * @return list of archives
     */
    private BundleArchive[] getBundleArchives() throws Exception {
        BundleArchive[] result = null;
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(Constants.FRAMEWORK_STORAGE, SettingsBean.getInstance().getJahiaVarDiskPath() + "/bundles-deployed");
        configMap.put(BundleCache.CACHE_LOCKING_PROP, "false");
        BundleCache bundleCache = new BundleCache(new org.apache.felix.framework.Logger(), configMap);
        result = bundleCache.getArchives();
        return result;
    }

    /**
     * populate the list of bundles in module management entity
     * @param moduleManagement module management entity
     * @return map of the bundle list states
     */
    public Map<String, String> populateBundles(ModuleManagement moduleManagement) {
        Map<String, String> states = new HashMap<>();
        try {
            for (Map.Entry<Bundle, String> entry : getLocalBundles().entrySet()) {
                Bundle bundle = entry.getKey();
                moduleManagement.getBundles().put(bundle.getName(), bundle);
                states.put(bundle.getName(), entry.getValue());
            }
        } catch (RepositoryException e) {
            logger.error("Error initializing and verifying cluster JCR structures", e);
        }

        return states;
    }

    /**
     * Populate the list of bundles in the cluster node
     * 
     * @param clusterNode
     *            cluster node to update its bundles
     * @param bundleSources
     *            bundles sources
     * @param bundleStates the map bundle-to-state 
     */
    public void populateNodeBundles(ClusterNode clusterNode, TreeMap<String, Bundle> bundleSources, Map<String, String> bundleStates) {
        for (Map.Entry<String, Bundle> entry : bundleSources.entrySet()) {
            NodeBundle nodeBundle = new NodeBundle(entry.getKey());
            nodeBundle.setBundle(entry.getValue());
            if (bundleStates != null) {
                String state = bundleStates.get(entry.getKey());
                if (state != null) {
                    nodeBundle.setState(state);
                }
            }
            clusterNode.getBundles().put(nodeBundle.getName(), nodeBundle);
        }
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}