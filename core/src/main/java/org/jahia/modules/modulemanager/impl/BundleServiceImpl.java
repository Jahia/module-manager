package org.jahia.modules.modulemanager.impl;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.util.IOUtils;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.jahia.modules.modulemanager.model.BinaryFile;
import org.jahia.modules.modulemanager.model.Bundle;
import org.jahia.modules.modulemanager.model.ModuleManagement;
import org.jahia.osgi.BundleUtils;
import org.osgi.framework.BundleContext;
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

/**
 * Created by achaabni on 17/11/15.
 */
public class BundleServiceImpl implements BundleContextAware {

    private static final Logger logger = LoggerFactory.getLogger(BundleServiceImpl.class);
    
    private BundleContext bundleContext;

    /**
     * Get local bundles from Context
     *
     * @return local bundles with states
     * @throws RepositoryException
     */
    private Map<Bundle, String> getLocalBundles() throws RepositoryException {
        Map<Bundle, String> result = new HashMap<>();
        for (org.osgi.framework.Bundle contextBundle : bundleContext.getBundles()) {
            if (contextBundle.getHeaders().get("Jahia-Module-Type") != null || contextBundle.getHeaders().get("Jahia-Cluster-Deployment") != null) {
                Bundle bundleToAdd = new Bundle();
                String bundleLocation = contextBundle.getLocation();
                InputStream inputStream = null;
                URL bundleURL = null;
                try {
                    bundleURL = new URL(bundleLocation);
                    bundleToAdd.setFileName(FilenameUtils.getName(bundleURL.getPath()));
                    inputStream = bundleURL.openStream();
                    DigestInputStream digestInputStream = ModuleManagerImpl.toDigestInputStream(inputStream);
                    bundleToAdd.setFile(new BinaryFile(IOUtils.toByteArray(digestInputStream)));
                    bundleToAdd.setSymbolicName(contextBundle.getHeaders().get("Bundle-SymbolicName").toString());
                    bundleToAdd.setDisplayName(contextBundle.getHeaders().get("Bundle-Name").toString());
                    bundleToAdd.setVersion(contextBundle.getHeaders().get("Bundle-Version").toString());
                    bundleToAdd.setChecksum(Hex.encodeHexString(digestInputStream.getMessageDigest().digest()));
                    bundleToAdd.setName(bundleToAdd.getSymbolicName() + "-" + bundleToAdd.getVersion());
                    result.put(bundleToAdd, BundleUtils.getModule(contextBundle).getState().getState().toString().toLowerCase());
                } catch (MalformedURLException e) {
                    logger.error("Couldn't resolve bundle URL " + bundleLocation, e);
                } catch (IOException e) {
                    logger.error("Error storing bundle " + contextBundle + " in JCR", e);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }

            }

        }
        return result;
    }

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


    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}