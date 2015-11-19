package org.jahia.modules.modulemanager.impl;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.util.IOUtils;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.jahia.modules.modulemanager.model.BinaryFile;
import org.jahia.modules.modulemanager.model.Bundle;
import org.jahia.modules.modulemanager.model.ModuleManagement;
import org.jahia.services.content.DefaultEventListener;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventIterator;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by achaabni on 17/11/15.
 */
public class BundleServiceImpl extends DefaultEventListener implements BundleContextAware {

    private static final int STREAM_BUFFER_LENGTH = 1024;
    private BundleContext bundleContext;
    private static final Logger logger = LoggerFactory.getLogger(BundleServiceImpl.class);

    @Override
    public int getEventTypes() {
        return 0;
    }

    @Override
    public void onEvent(EventIterator eventIterator) {

    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Start method
     */
    public void start() {
    }

    /**
     * Stop method
     */
    public void stop() {
    }

    public void populateBundles(ModuleManagement moduleManagement) {
        List<Bundle> bundles = new ArrayList<Bundle>();
        try {
            bundles = getLocalBundles();
        } catch (RepositoryException e) {
            logger.error("Error initializing and verifying cluster JCR structures", e);
        }
        for (Bundle bundle : bundles) {
            moduleManagement.getBundles().put(bundle.getName(), bundle);
        }

    }


    /**
     * Get local bundles from Context
     *
     * @return local bundles
     * @throws RepositoryException
     */
    private List<Bundle> getLocalBundles() throws RepositoryException {
        List<Bundle> result = new ArrayList<Bundle>();
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
                    bundleToAdd.setFile(new BinaryFile("application/java-archive", IOUtils.toByteArray(inputStream)));
                    bundleToAdd.setName(contextBundle.getHeaders().get("Bundle-SymbolicName").toString());
                    bundleToAdd.setDisplayName(contextBundle.getHeaders().get("Bundle-Name").toString());
                    bundleToAdd.setVersion(contextBundle.getHeaders().get("Bundle-Version").toString());
                    bundleToAdd.setChecksum(getChecksum(bundleURL));
                    bundleToAdd.setSymbolicName(bundleToAdd.getName());
                    //bundleToAdd.setState( BundleUtils.getModule(contextBundle).getState().getState().toString().toLowerCase());
                    result.add(bundleToAdd);
                } catch (MalformedURLException e) {
                    logger.error("Couldn't resolve bundle URL " + bundleLocation, e);
                    break;
                } catch (IOException e) {
                    logger.error("Error storing bundle " + contextBundle + " in JCR", e);
                    break;
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }

            }

        }
        return result;
    }

    /**
     * Get the Checksum from a specific URL
     *
     * @param resourceURL url to create the checksum
     * @return checksum value
     */
    public String getChecksum(URL resourceURL) {
        InputStream inputStream = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            inputStream = resourceURL.openStream();
            updateDigest(md, inputStream);
            return new String(Hex.encodeHex(md.digest()));
        } catch (IOException e) {
            logger.error("Error processing contents of resource " + resourceURL, e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error getting digest algorithm for resource " + resourceURL, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("Error closing inputstream for resource URL " + resourceURL, e);
                }
            }
        }
        return null;
    }

    /**
     * Update message digest for an input stream
     *
     * @param digest initial message digest
     * @param data   input stream
     * @return message digest updated
     * @throws IOException
     */
    private static MessageDigest updateDigest(final MessageDigest digest, final InputStream data) throws IOException {
        final byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
        int read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);

        while (read > -1) {
            digest.update(buffer, 0, read);
            read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);
        }

        return digest;
    }
}