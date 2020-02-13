import registrations from './registrations';
import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';

registry.add('callback', 'module-manager', {
    targets: ['jahiaApp-init:50'],
    callback: async () => {
        await i18next.loadNamespaces('module-manager');
        registrations();
        console.log('%c Module Manager routes have been registered', 'color: #3c8cba');
    }
});
