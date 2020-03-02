import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    const level = 'server';
    const parentTarget = 'administration-server';

    const path = '/administration/manageModules';
    const route = 'manageModules';
    registry.addOrReplace('adminRoute', `${level}-${path.toLowerCase()}`, {
        id: route,
        targets: [`${parentTarget}-systemcomponents:2`],
        path: path,
        route: route,
        defaultPath: path,
        requiredPermission: 'adminTemplates',
        icon: null,
        label: 'module-manager:modules.label',
        childrenTarget: 'systemcomponents',
        isSelectable: true,
        level: level
    });
};
