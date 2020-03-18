import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    registry.addOrReplace('adminRoute', 'manageModules', {
        targets: ['administration-server-systemComponents:2'],
        requiredPermission: 'adminTemplates',
        icon: null,
        label: 'module-manager:modules.label',
        isSelectable: true,
        iframeUrl: window.contextJsParameters.contextPath + '/cms/adminframe/default/en/settings.manageModules.html?redirect=false'
    });
};
