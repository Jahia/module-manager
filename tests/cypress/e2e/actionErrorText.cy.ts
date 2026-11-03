import { ManageModulesPage } from '../page-object/ManageModulesPage'
import { fixtureJar, removeFixtureModules } from '../support/fixtureModules'

/**
 * The stop action of a module is sent under a module name that no module has, so the Modules screen
 * answers with an error that repeats the name. The name holds the characters that HTML gives a meaning
 * to.
 */
describe('Error of a module action whose module name holds special characters', () => {
    const moduleId = 'dummy1'
    const moduleName = 'unknown-<gamma> & "été"'
    const expectedError = `Module '${moduleName}' could not stopped`

    before(() => {
        removeFixtureModules()
        cy.login()
        ManageModulesPage.visit()
            .upload(fixtureJar('dummy1-1.0.0.jar'), { autoStart: true, validateDefinitions: true })
            .expectUploadSuccess()
    })

    after(() => {
        removeFixtureModules()
    })

    beforeEach(() => {
        cy.login()
    })

    it('names the module exactly as the action sent it', () => {
        ManageModulesPage.visit().search(moduleId).stopUnderName(moduleId, moduleName).expectUploadError(expectedError)
    })
})
