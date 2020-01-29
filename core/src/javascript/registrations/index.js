import {registerRoutes as registerModulesRoutes} from './modules/registerRoutes';
import {useTranslation} from 'react-i18next';

export default function () {
    const {t} = useTranslation('module-manager');

    registerModulesRoutes(t);

    return null;
}
