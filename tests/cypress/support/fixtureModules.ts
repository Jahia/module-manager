import { jfaker } from '@jahia/cypress'

/** The modules under cypress/fixtures/assets/moduleManagement, by id. */
const FIXTURE_MODULE_IDS = ['dummy1', 'qa-module-with-dependency', 'versioningtestmodule']

/** The jar of a fixture module, as `selectFile` takes it. */
export const fixtureJar = (fileName: string) => `cypress/fixtures/assets/moduleManagement/${fileName}`

/** Lowercase letters only, so it fits a site key. */
export const uniqueToken = () => jfaker.string.alpha({ length: 8, casing: 'lower' })

export const removeFixtureModules = () =>
    cy.executeGroovy('groovy/removeFixtureModules.groovy', { MODULE_IDS: FIXTURE_MODULE_IDS.join(',') })
