package org.jahia.modules.modulemanager.rest;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The transformer lookup of {@link ModuleManagerResource}: every deployed transformer is asked about the
 * uploaded file, and the first one that can handle it wins. A loop that went on after a successful transform
 * would hand the next transformer a {@code js:} URL, whose {@link org.springframework.core.io.Resource#getFile()}
 * throws, which the REST layer logs as a warning on every successful JavaScript module upload.
 */
public class ModuleManagerResourceTransformTest {

    private final BundleContext bundleContext = mock(BundleContext.class);

    // --- the first transformer that can handle the file wins ------------------------------------

    @Test
    public void transformsTheFileOnceAndAsksNoLaterTransformer() throws Exception {
        ArtifactUrlTransformer javascript = mock(ArtifactUrlTransformer.class);
        ArtifactUrlTransformer other = mock(ArtifactUrlTransformer.class);
        deploy(javascript, other);

        File uploadedFile = File.createTempFile("tgz-upload-test-1.0.0-", ".tgz");
        URL transformedURL = new URL("file:/transformed/tgz-upload-test-1.0.0.jar");
        when(javascript.canHandle(uploadedFile)).thenReturn(true);
        when(javascript.transform(uploadedFile.toURI().toURL())).thenReturn(transformedURL);

        assertEquals(transformedURL, ModuleManagerResource.transformUploadedFile(bundleContext, uploadedFile).getURL());

        verify(other, never()).canHandle(any(File.class));
    }

    @Test
    public void returnsNullWhenNoTransformerCanHandleTheFile() throws Exception {
        ArtifactUrlTransformer javascript = mock(ArtifactUrlTransformer.class);
        deploy(javascript);

        File uploadedFile = File.createTempFile("a-module-", ".jar");
        when(javascript.canHandle(uploadedFile)).thenReturn(false);

        assertNull(ModuleManagerResource.transformUploadedFile(bundleContext, uploadedFile));
    }

    // --- a .tgz that no transformer claimed needs the JavaScript engine ---------------------------

    @Test
    public void aTgzNoTransformerClaimedNeedsTheJavascriptEngine() throws Exception {
        deploy();
        File uploadedFile = File.createTempFile("tgz-upload-test-1.0.0-", ".tgz");
        uploadedFile.deleteOnExit();

        boolean noTransformerClaimed = ModuleManagerResource.transformUploadedFile(bundleContext, uploadedFile) == null;

        assertTrue(ModuleManagerResource.needsJavascriptEngine("tgz-upload-test-1.0.0.tgz", noTransformerClaimed));
    }

    @Test
    public void aTgzTheEngineClaimedDoesNot() throws Exception {
        ArtifactUrlTransformer javascript = mock(ArtifactUrlTransformer.class);
        deploy(javascript);
        File uploadedFile = File.createTempFile("tgz-upload-test-1.0.0-", ".tgz");
        uploadedFile.deleteOnExit();
        when(javascript.canHandle(uploadedFile)).thenReturn(true);
        when(javascript.transform(uploadedFile.toURI().toURL()))
                .thenReturn(new URL("file:/transformed/tgz-upload-test-1.0.0.jar"));

        boolean noTransformerClaimed = ModuleManagerResource.transformUploadedFile(bundleContext, uploadedFile) == null;

        assertFalse(ModuleManagerResource.needsJavascriptEngine("TGZ-UPLOAD-TEST-1.0.0.TGZ", noTransformerClaimed));
    }

    @Test
    public void aJarOrAFailedTransformDoesNot() {
        assertFalse(ModuleManagerResource.needsJavascriptEngine("a-module-1.0.0.jar", true));
        // A transformer that threw: the caller passes false, as some transformer did claim the file.
        assertFalse(ModuleManagerResource.needsJavascriptEngine("a.tgz", false));
    }

    // --- the deployed transformers, in the order the framework returns them -----------------------

    @SuppressWarnings("unchecked")
    private void deploy(ArtifactUrlTransformer... transformers) throws Exception {
        ServiceReference<ArtifactUrlTransformer>[] references = new ServiceReference[transformers.length];
        for (int i = 0; i < transformers.length; i++) {
            references[i] = mock(ServiceReference.class);
            when(bundleContext.getService(references[i])).thenReturn(transformers[i]);
        }
        when(bundleContext.getServiceReferences(ArtifactUrlTransformer.class, null)).thenReturn(Arrays.asList(references));
    }
}
