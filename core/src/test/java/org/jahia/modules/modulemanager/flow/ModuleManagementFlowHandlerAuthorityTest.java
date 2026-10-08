package org.jahia.modules.modulemanager.flow;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.binding.message.MessageContext;
import org.springframework.binding.message.MessageResolver;
import org.springframework.context.MessageSourceResolvable;

import javax.jcr.RepositoryException;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The authority {@link ModuleManagementFlowHandler} establishes for the screen it drives, and the studio
 * decision it takes from the render.
 * <p>
 * The authority is read off the main resource of the render. The render is of one of the containers the
 * hosting templates apply on, and the caller holds the permission those templates declare on that node.
 * Each half is asserted in both directions. The last cases take a public entry point, which shows the
 * decision reaching a caller.
 */
public class ModuleManagementFlowHandlerAuthorityTest {

    private static final String SERVER_SETTINGS = "jnt:globalSettings";
    private static final String MODULE = "jnt:module";
    private static final String ORDINARY_PAGE = "jnt:page";

    private static RenderContext renderOf(JCRNodeWrapper mainNode) {
        Resource mainResource = mock(Resource.class);
        when(mainResource.getNode()).thenReturn(mainNode);
        RenderContext renderContext = mock(RenderContext.class);
        when(renderContext.getMainResource()).thenReturn(mainResource);
        return renderContext;
    }

    private static JCRNodeWrapper nodeOfType(String nodeType, boolean holdsPermission) throws RepositoryException {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.isNodeType(nodeType)).thenReturn(true);
        when(node.hasPermission(ModuleManagementFlowHandler.REQUIRED_PERMISSION)).thenReturn(holdsPermission);
        return node;
    }

    // --- the realm: the containers the hosting templates apply on --------------------------------

    @Test
    public void grantsAHolderOnTheServerSettings() throws RepositoryException {
        assertTrue(new ModuleManagementFlowHandler().isAdministrationGranted(renderOf(nodeOfType(SERVER_SETTINGS, true))));
    }

    @Test
    public void grantsAHolderOnAModuleNode() throws RepositoryException {
        assertTrue(new ModuleManagementFlowHandler().isAdministrationGranted(renderOf(nodeOfType(MODULE, true))));
    }

    @Test
    public void refusesAHolderOnAnyOtherContainer() throws RepositoryException {
        JCRNodeWrapper page = nodeOfType(ORDINARY_PAGE, true);

        assertFalse(new ModuleManagementFlowHandler().isAdministrationGranted(renderOf(page)));
        verify(page, never()).hasPermission(anyString());
    }

    // --- the permission: what the templates declare, held on that node ---------------------------

    /** A guest and an authenticated caller without the permission read the same on the main resource. */
    @Test
    public void refusesACallerWithoutThePermission() throws RepositoryException {
        assertFalse(new ModuleManagementFlowHandler().isAdministrationGranted(renderOf(nodeOfType(SERVER_SETTINGS, false))));
        assertFalse(new ModuleManagementFlowHandler().isAdministrationGranted(renderOf(nodeOfType(MODULE, false))));
    }

    // --- fails closed when it cannot read either half ------------------------------------------

    @Test
    public void refusesARenderWithoutAMainResource() {
        ModuleManagementFlowHandler handler = new ModuleManagementFlowHandler();

        assertFalse(handler.isAdministrationGranted(null));
        assertFalse(handler.isAdministrationGranted(mock(RenderContext.class)));
        assertFalse(handler.isAdministrationGranted(renderOf(null)));
    }

    @Test
    public void refusesAMainResourceWhoseTypeCannotBeRead() throws RepositoryException {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.isNodeType(anyString())).thenThrow(new RepositoryException("unreadable"));
        when(node.hasPermission(ModuleManagementFlowHandler.REQUIRED_PERMISSION)).thenReturn(true);

        assertFalse(new ModuleManagementFlowHandler().isAdministrationGranted(renderOf(node)));
    }

    // --- the decision reaching a caller --------------------------------------------------------

    @Test
    public void anUploadWithoutTheAuthorityEndsWithThePermissionMessage() throws RepositoryException {
        MessageContext messages = mock(MessageContext.class);

        boolean uploaded = new ModuleManagementFlowHandler().uploadModule(null, messages, false, false, false,
                renderOf(nodeOfType(SERVER_SETTINGS, false)));

        assertFalse(uploaded);
        assertMessageCode(messages, "serverSettings.manageModules.notPermitted");
    }

    @Test
    public void anUploadWithTheAuthorityReachesTheFileCheck() throws RepositoryException {
        MessageContext messages = mock(MessageContext.class);

        boolean uploaded = new ModuleManagementFlowHandler().uploadModule(null, messages, false, false, false,
                renderOf(nodeOfType(SERVER_SETTINGS, true)));

        assertFalse(uploaded);
        assertMessageCode(messages, "serverSettings.manageModules.install.moduleFileRequired");
    }

    private static void assertMessageCode(MessageContext messages, String code) {
        ArgumentCaptor<MessageResolver> message = ArgumentCaptor.forClass(MessageResolver.class);
        verify(messages).addMessage(message.capture());
        String[] codes = ((MessageSourceResolvable) message.getValue()).getCodes();
        assertTrue(Arrays.toString(codes), Arrays.asList(codes).contains(code));
    }

    // --- the studio decision, taken from a render with or without an edit mode -----------------

    @Test
    public void readsTheStudioModesAsStudio() {
        assertTrue(new ModuleManagementFlowHandler().isStudio(renderInEditMode("studiomode")));
        assertTrue(new ModuleManagementFlowHandler().isStudio(renderInEditMode("studiovisualmode")));
    }

    @Test
    public void readsAnyOtherRenderAsNotStudio() {
        assertFalse(new ModuleManagementFlowHandler().isStudio(renderInEditMode("editmode")));
        assertFalse(new ModuleManagementFlowHandler().isStudio(renderInEditMode(null)));
    }

    private static RenderContext renderInEditMode(String editModeConfigName) {
        RenderContext renderContext = mock(RenderContext.class);
        when(renderContext.getEditModeConfigName()).thenReturn(editModeConfigName);
        return renderContext;
    }
}
