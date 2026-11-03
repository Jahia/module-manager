import { ManageModulesPage } from '../page-object/ManageModulesPage'
import { fixtureJar, removeFixtureModules } from '../support/fixtureModules'

/**
 * Version 3.0.0 of definitionstextmodule deploys its definitions, then version 2.0.0, whose definitions
 * add a property, runs in its place. The screen then warns that the definitions in use belong to
 * another version. The versions that the manifests state hold the characters that HTML gives a meaning
 * to.
 */
describe('Warning about the definitions of a module whose versions hold special characters', () => {
    const moduleId = 'definitionstextmodule'
    const deployed = {
        jar: 'definitionstextmodule-3.0.0.jar',
        key: 'org.example/definitionstextmodule/3.0.0',
        version: '3.0-<gamma> & "été"',
    }
    const started = { jar: 'definitionstextmodule-2.0.0.jar', version: "2.0-<beta> & 'été'" }

    before(() => {
        removeFixtureModules()
        cy.login()
        ManageModulesPage.visit()
            .upload(fixtureJar(deployed.jar), { autoStart: true, validateDefinitions: true })
            .expectUploadSuccess()
        cy.executeGroovy('groovy/uninstallModuleVersion.groovy', { MODULE_KEY: deployed.key })
        ManageModulesPage.visit()
            .upload(fixtureJar(started.jar), { autoStart: true, validateDefinitions: true })
            .expectUploadSuccess()
    })

    after(() => {
        removeFixtureModules()
    })

    beforeEach(() => {
        cy.login()
    })

    it('names both versions as their manifests state them in the warning about the definitions', () => {
        const warning = ManageModulesPage.visit().definitionsWarning(moduleId)
        warning.should('contain.text', `definitions of the ${deployed.version} version`)
        warning.should('contain.text', `started version (${started.version})`)
    })
})
