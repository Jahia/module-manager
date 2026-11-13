# module-manager Changelog

## 0.1.0

### New Features

* Improved the module upload API error message for JavaScript module packages (`.tgz`). When the Javascript Modules Engine is missing or stopped, uploading such a package now asks you to install and start it, instead of reporting an invalid OSGi bundle.

### Bug Fixes

* Restricted the Private App Store so a module forge URL must name a routable http(s) endpoint. A forge hosted on an internal address is accepted only when its host is listed in the org.jahia.modules.modulemanager.forge.allowedInternalHosts property.

* Fixed the module upload API so it no longer logs a warning on every successful upload of a JavaScript module package.

* Restricted the module administration screen and its actions to users who hold the permission to manage modules.

* Fixed the definitions warning of the Modules screen so it shows the versions of a module exactly as the module states them.

* Added maven.jahia.com to the Maven repositories module-manager uses to download its dependencies.

* Fixed uploading JavaScript module packages (.tgz) through the Module Manager REST API on Windows.

* Fixed the Modules screen so it shows module names, versions, groups and types exactly as each module defines them.

* Fixed the error messages of the Modules screen so they show module names and versions exactly as the action named them.

* Fixed the Available modules tab of the Modules screen so it shows each module exactly as its app store lists it.

* Fixed the Modules screen so it shows the errors and the state details of a module exactly as the module reports them.

* Fixed the Installed modules tab of the Modules screen so it shows each available update exactly as its app store lists it.
