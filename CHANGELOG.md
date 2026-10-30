# module-manager Changelog

## 0.0.1

* Restricted the Private App Store so a module forge URL must name a routable http(s) endpoint. A forge hosted on an internal address is accepted only when its host is listed in the org.jahia.modules.modulemanager.forge.allowedInternalHosts property.

* Restricted the module administration screen and its actions to users who hold the permission to manage modules.

* Added maven.jahia.com to the Maven repositories module-manager uses to download its dependencies.

* Fixed the Modules screen so it shows module names, versions, groups and types exactly as each module defines them.

* Fixed the Available modules tab of the Modules screen so it shows each module exactly as its app store lists it.
