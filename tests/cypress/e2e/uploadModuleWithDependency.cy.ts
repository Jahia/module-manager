import { ManageModulesPage } from '../page-object/ManageModulesPage'
import { fixtureJar, removeFixtureModules } from '../support/fixtureModules'

/**
 * The tests are the ordered steps of one journey: each one uploads a module, and the next one starts
 * from what the previous upload installed.
 */
describe('Upload of a module with a dependency', () => {
    // qa-module-with-dependency depends on dummy1 version 2.
    const dependent = 'qa-module-with-dependency'

    // The uploads start what they install, so the journey is the same in development mode and in production mode.
    const upload = (fileName: string) =>
        ManageModulesPage.visit().upload(fixtureJar(fileName), { autoStart: true, validateDefinitions: true })

    // A module waiting for its dependency stays an installed bundle that OSGi has not resolved.
    const unresolved = '[data-sel-role="status-installed"]'

    before(() => {
        removeFixtureModules()
    })

    after(() => {
        removeFixtureModules()
    })

    beforeEach(() => {
        cy.login()
    })

    it('lists the missing dependency of a module uploaded without it', () => {
        upload('qa-module-with-dependency-3.1.0-SNAPSHOT.jar').expectUploadError('Missing dependencies:')

        const modules = ManageModulesPage.visit().search(dependent)
        modules.moduleRow(dependent).should('contain.text', 'Depends on: dummy1, dummy1=2')
        modules.moduleRow(dependent).find(unresolved).should('exist')
    })

    it('keeps the module unresolved when the wrong version of the dependency is uploaded', () => {
        upload('dummy1-1.0.0.jar').expectUploadSuccess()

        const modules = ManageModulesPage.visit().search(dependent)
        modules.moduleRow(dependent).should('contain.text', 'Depends on: dummy1=2')
        modules.moduleRow(dependent).should('not.contain.text', 'Depends on: dummy1,')
        modules.moduleRow(dependent).find(unresolved).should('exist')
    })

    it('starts the module when the required version of the dependency is uploaded', () => {
        upload('dummy1-2.0.0.jar').expectUploadSuccess()

        const modules = ManageModulesPage.visit().search(dependent)
        modules.moduleRow(dependent).find('[data-sel-role="status-started"]').should('exist')
        modules.moduleRow(dependent).should('not.contain.text', 'Depends on')
    })
})
