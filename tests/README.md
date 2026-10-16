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

The specs upload modules that they build at run time: `cypress/plugins/moduleJar.js` writes the jar of
a minimal module (a manifest, and a `definitions.cnd` when a spec needs one), and
`cypress/support/fixtureModules.ts` hands it to `selectFile`. So no binary lives in the repository, and
each run gets module ids of its own.

- Every fixture module belongs to the group `org.example.modulemanager`. Jahia refuses an unsigned
  module of the `org.jahia.modules` group, and the cleanup script
  `cypress/fixtures/groovy/removeFixtureModules.groovy` uninstalls every module of the fixture group,
  including what a failed run left behind.
- The Modules screen checks "Start the module after upload" in development mode only, so a spec sets
  that box itself and runs the same way in both operating modes.

## Tags

A spec migrated from the legacy Selenium suite (`Jahia/selenium`) tags each of its tests with the tags
of its row in the migration tracking issue, including `testrail-case-<n>`, which `jahia-reporter`
syncs to the TestRail case that the test replaces. A spec written here first replaces no TestRail
case, so it carries no tag.
