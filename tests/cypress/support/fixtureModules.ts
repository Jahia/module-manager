import { jfaker } from '@jahia/cypress'

/** The modules under cypress/fixtures/assets/moduleManagement, by id. */
const FIXTURE_MODULE_IDS = [
    'dummy1',
    'labelsdependentmodule',
    'labelstestmodule',
    'qa-module-with-dependency',
    'versioningtestmodule',
]

/** The jar of a fixture module, as `selectFile` takes it. */
export const fixtureJar = (fileName: string) => `cypress/fixtures/assets/moduleManagement/${fileName}`

/** Lowercase letters only, to make a name unique to the run. */
export const uniqueToken = () => jfaker.string.alpha({ length: 8, casing: 'lower' })

/** `executeGroovy` only logs a failed script, so the cleanup asserts the result to fail where it breaks. */
export const removeFixtureModules = () =>
    cy
        .executeGroovy('groovy/removeFixtureModules.groovy', { MODULE_IDS: FIXTURE_MODULE_IDS.join(',') })
        .then((result) => expect(String(result)).not.to.contain('.failed'))
