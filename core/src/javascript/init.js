import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';
import {registerRoutes} from './registrations/registerRoutes';

export default function () {
    registry.add('callback', 'module-manager', {
        targets: ['jahiaApp-init:50'],
        callback: () => {
            i18next.loadNamespaces('module-manager');
            registerRoutes();
            console.log('%c Module Manager routes have been registered', 'color: #3c8cba');
        }
    });
}
