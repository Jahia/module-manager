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

moduleLifeCycleConstraints[1].moduleId="bookmarks"
moduleLifeCycleConstraints[1].version="[4,5.2]"
moduleLifeCycleConstraints[1].disableOperations[0]="START"
moduleLifeCycleConstraints[1].disableOperations[1]="DEPLOY"
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

## Implementation notes/questions

* Do we need to worry about overriding default config file? I'm not sure if there's a way to protect that. Or safe to assume we're not going to override default config?
* (Maybe for @tdraier) Constraint checks already in the module manager UI but need more work on the deployment scenario specifically for certain versions (to be implemented)
* Do we need to handle priority? What happens when constraints are defined in more than one configuration?
  * Currently latest edits overwrite old defintions but maybe we can keep track of them separately and apply them with logical AND (apply all rules). Or not sure if safe to assume no conflicts for now.
* * I'm assuming configuration deployment applies to all nodes in a cluster (to be tested)?
