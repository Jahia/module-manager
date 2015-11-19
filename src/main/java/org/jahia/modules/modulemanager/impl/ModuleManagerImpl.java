/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.2 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
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
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
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
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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
package org.jahia.modules.modulemanager.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.jcr.RepositoryException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.jahia.modules.modulemanager.ModuleManagementException;
import org.jahia.modules.modulemanager.ModuleManager;
import org.jahia.modules.modulemanager.OperationResult;
import org.jahia.modules.modulemanager.OperationScope;
import org.jahia.modules.modulemanager.model.BinaryFile;
import org.jahia.modules.modulemanager.model.Bundle;
import org.jahia.modules.modulemanager.model.Operation;
import org.jahia.modules.modulemanager.persistence.ModuleInfoPersister;
import org.jahia.modules.modulemanager.persistence.ModuleInfoPersister.OCMCallback;
import org.jahia.services.content.JCRContentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * The main entry point service for the module management service, providing functionality for module deployment, undeployment, start and
 * stop operations, which are performed in a seamless way on a standalone installation as well as across the platform cluster.
 * 
 * @author Sergiy Shyrkov
 */
public class ModuleManagerImpl implements ModuleManager {

    private static final Logger logger = LoggerFactory.getLogger(ModuleManagerImpl.class);

    private static void populateFromManifest(Bundle bundle, File bundleFile) throws IOException {
        JarInputStream jarIs = new JarInputStream(new FileInputStream(bundleFile));
        try {
            Manifest mf = jarIs.getManifest();
            if (mf != null) {
                bundle.setSymbolicName(mf.getMainAttributes().getValue("Bundle-SymbolicName"));
                bundle.setVersion(mf.getMainAttributes().getValue("Bundle-Version"));
                bundle.setDisplayName(mf.getMainAttributes().getValue("Bundle-Name"));
            }
        } finally {
            IOUtils.closeQuietly(jarIs);
        }
    }

    private static Bundle toBundle(Resource bundleResource, File tmpFile) throws IOException {
        // store bundle into a temporary file
        DigestInputStream dis = toDigestInputStream(bundleResource.getInputStream());
        FileUtils.copyInputStreamToFile(dis, tmpFile);

        Bundle b = new Bundle();
        // populate data from manifest
        populateFromManifest(b, tmpFile);

        if (StringUtils.isBlank(b.getSymbolicName()) || StringUtils.isBlank(b.getVersion())) {
            // not a valid JAR or bundle information is missing -> we stop here
            return null;
        }

        b.setName(b.getSymbolicName() + "-" + b.getVersion());
        b.setPath("/module-management/bundles/" + b.getName());

        // calculate checksum
        b.setChecksum(Hex.encodeHexString(dis.getMessageDigest().digest()));

        // keep original filename if available
        b.setFileName(StringUtils.defaultIfBlank(bundleResource.getFilename(),
                b.getSymbolicName() + "-" + b.getVersion() + ".jar"));

        // TODO file
        b.setFile(new BinaryFile(FileUtils.readFileToByteArray(tmpFile)));

        return b;
    }

    static DigestInputStream toDigestInputStream(InputStream is) {
        try {
            return new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private OperationProcessor operationProcessor;

    private ModuleInfoPersister persister;

    private void doInstall(final Bundle bundle, final OperationScope operationScope) throws RepositoryException {
        persister.doExecute(new OCMCallback<Object>() {
            @Override
            public Object doInOCM(ObjectContentManager ocm) throws RepositoryException {
                // store the bundle in JCR
                ocm.insert(bundle);

                // create operation node
                Operation op = new Operation();
                op.setBundle(bundle);
                op.setAction("install");
                op.setState("open");
                op.setName(JCRContentUtils.findAvailableNodeName(
                        ocm.getSession().getNode("/module-management/operations"), "install-" + bundle.getName()));
                op.setPath("/module-management/operations/" + op.getName());
                ocm.insert(op);

                ocm.save();

                return null;
            }
        });
    }

    @Override
    public OperationResult install(Resource bundleResource, OperationScope operationScope)
            throws ModuleManagementException {

        // save to a temporary file and create Bundle data object
        File tmp = null;
        try {
            tmp = File.createTempFile(bundleResource.getFilename() != null
                    ? FilenameUtils.getBaseName(bundleResource.getFilename()) : "bundle", ".jar");
            Bundle bundle = toBundle(bundleResource, tmp);
            if (bundle == null) {
                return OperationResult.NOT_VALID_BUNDLE;
            }

            // check, if we have this bundle already installed
            Bundle existingBundle = persister.lookupBundle(bundle.getName());
            if (existingBundle != null && existingBundle.getChecksum() != null
                    && existingBundle.getChecksum().equals(bundle.getChecksum())) {
                // we have exactly same bundle installed already -> refuse
                logger.debug("Bundle {} already installed. Skipping it", existingBundle.getName());
                return OperationResult.ALREADY_INSTALLED;
            }

            // store bundle in JCR and create operation node
            doInstall(bundle, operationScope);

            // notify the processor
            notifyOperationProcessor();
        } catch (Exception e) {
            throw new ModuleManagementException(e);
        } finally {
            FileUtils.deleteQuietly(tmp);
        }

        return OperationResult.SUCCESS;
    }

    private void notifyOperationProcessor() {
        // TODO move into a background job
        try {
            operationProcessor.process();
        } catch (ModuleManagementException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setOperationProcessor(OperationProcessor operationProcessor) {
        this.operationProcessor = operationProcessor;
    }

    public void setPersister(ModuleInfoPersister persister) {
        this.persister = persister;
    }

    @Override
    public OperationResult start(String bundleKey, OperationScope operationScope) {
        // TODO Auto-generated method stub
        return OperationResult.SUCCESS;
    }

    @Override
    public OperationResult stop(String bundleKey, OperationScope operationScope) {
        // TODO Auto-generated method stub
        return OperationResult.SUCCESS;
    }

    @Override
    public OperationResult uninstall(String bundleKey, OperationScope operationScope) {
        // TODO Auto-generated method stub
        return OperationResult.SUCCESS;
    }
}
