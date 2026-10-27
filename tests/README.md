# Cypress end-to-end tests

The suite drives the Modules screen of the administration (`/jahia/administration/manageModules`)
against a running Jahia.

## Running the suite

Against a local Jahia on `http://localhost:8080` (`root` / `root1234`), with node 20.19 or later:

```bash
yarn install
yarn e2e:ci        # headless
yarn e2e:debug     # the interactive runner
```

`JAHIA_URL` and `SUPER_USER_PASSWORD` point the run at another instance. In CI, the pull-request
workflow builds the module, installs it with `provisioning-manifest-build.yml`, and runs the suite in
the container that `Dockerfile` describes.

## Fixture modules

The specs upload the jars under `cypress/fixtures/assets/moduleManagement/`:

- `qa-module-with-dependency` 3.1.0-SNAPSHOT, which depends on `dummy1` version 2, and `dummy1` in
  versions 1.0.0 and 2.0.0.
- `versioningtestmodule` in versions 1.0.0-SNAPSHOT and 2.0.0-SNAPSHOT. Version 2.0.0 changes the
  type of a property from `string` to `long`, which the definition check refuses as a major change.
  Its group is `org.jahia.test`: the jar is not signed, and Jahia refuses an unsigned module of the
  `org.jahia.modules` group.
- `labelstestmodule` 1.0.0, a jar that holds only a manifest. Its name, version, group, type, vendor
  and description hold some of `&`, `<`, `>`, double quotes and an accent, which the screen shows as
  written.
- `labelsdependentmodule` 1.0.0, a manifest-only jar that depends on `labelstestmodule`. Its name
  holds the same characters.

`cypress/fixtures/groovy/removeFixtureModules.groovy` uninstalls every version of these modules
before and after each spec, including what a failed run left behind.

The Modules screen checks "Start the module after upload" in development mode only, so a spec sets
that box itself and runs the same way in both operating modes.
