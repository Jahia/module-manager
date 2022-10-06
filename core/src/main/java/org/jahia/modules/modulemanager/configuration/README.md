# Disable operations per module

* Disable certain module management functions through configuration: `org.jahia.modules.modulemanager.configuration.constraints-*.yaml/cfg`
* Default set of restrictions included in the `module-manager` module deployed: `org.jahia.modules.modulemanager.configuration.constraints-default.yaml`
* Constraints can be extended by deploying another configuration e.g. `org.jahia.modules.modulemanager.configuration.constraints-cloud.yaml/cfg`
* To update, simply re-deploy same configuration file with the changes. This will refresh all previous definitions for a given configuration file.

## Deployment

 * Supports standard configuration deployment options
   * Through provisioning (see [Provisioning - Install/edit configuration](https://github.com/Jahia/jahia/tree/master/bundles/provisioning#install--edit-configuration))
   * Manual deploy in `karaf/etc` folder

## Format

### *yaml format*

```
moduleLifeCycleConstraints:
  - moduleId: "article"
    disableOperations:
      - STOP
  - moduleId: "bookmarks"
    disableOperations: [START, DEPLOY]
    version: "[4,5.2]"
```

### *cfg format*

```
moduleLifeCycleConstraints[0].moduleId=article
moduleLifeCycleConstraints[0].disableOperations[0]=STOP

moduleLifeCycleConstraints[1].moduleId=bookmarks
moduleLifeCycleConstraints[1].version=[4,5.2]
moduleLifeCycleConstraints[1].disableOperations[0]=START
moduleLifeCycleConstraints[1].disableOperations[1]=DEPLOY
```

### Attribute definitions

* **moduleId** (required) - module name
* **version** (optional) - Specific version to which constraint should apply to for a specific module.
  * Supports semantic versioning
    * e.g. `version: 3` means constraint applies for all `3.x.x` versions only
  * Supports standard OSGI version ranges
    * e.g. `[3.1, 4)` constraint applies to module versions between `3.1.x` (inclusive) up to `4.0` (exclusive)
  * Constraint applies to all versions of the module if not specified
* **disableOperations** (optional) - List of operations to be disabled for a given module/version
  * Current list of operations to be restricted (case-sensitive)
    * START
    * STOP
    * UNDEPLOY
    * DEPLOY
  * Defaults to ***all*** operations disabled if not specified or given an empty list

### Duplicate definitions

Duplicate definitions across different configurations for a given module ID are all applied individually. e.g. given the following rules from different configuration files:

```
moduleLifeCycleConstraints:
  - moduleId: "my-module"
    version: 3
    disableOperations:
      - STOP
```

and 

```
moduleLifeCycleConstraints:
  - moduleId: "my-module"
    version: 4
    disableOperations:
      - START
```

Then `module@v3.0.0` will only have STOP operation disabled and `module@v4.0.0` will only have START operation disabled (rules are applied individually and not merged).

