---
module-manager: minor
---

Improved the module upload API error message for JavaScript module packages (`.tgz`). When the Javascript Modules Engine is missing or stopped, uploading such a package now asks you to install and start it, instead of reporting an invalid OSGi bundle.
