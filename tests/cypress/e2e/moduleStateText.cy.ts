import { ManageModulesPage } from '../page-object/ManageModulesPage'
import { fixtureJar, removeFixtureModules } from '../support/fixtureModules'

/**
 * The rules of statetextmodule do not compile, and the compile error repeats the line of the rules that
 * holds the characters that HTML gives a meaning to. The screen shows that error in the row of the
 * module. A site uses the module, so the screen also shows the state of its version with that error, in
 * place of the actions of the version.
 */
describe('Error and state of a module whose details hold special characters', () => {
    const moduleId = 'statetextmodule'
    const ruleLine = 'Reports & <Labels> "beta"'
    const useOnSite = (use: boolean) =>
        cy
            .executeGroovy('groovy/useModuleOnSite.groovy', {
                MODULE_ID: moduleId,
                SITE_KEY: 'systemsite',
                USE: String(use),
            })
            .then((result) => expect(String(result)).not.to.contain('.failed'))

    before(() => {
        removeFixtureModules()
        cy.login()
        ManageModulesPage.visit().upload(fixtureJar('statetextmodule-1.0.0.jar'), {
            autoStart: true,
            validateDefinitions: true,
        })
        useOnSite(true)
    })

    after(() => {
        useOnSite(false)
        removeFixtureModules()
    })

    beforeEach(() => {
        cy.login()
    })

    it('shows the error and the details of the state of the version as the module states them', () => {
        const modules = ManageModulesPage.visit().search(moduleId)
        modules.moduleRow(moduleId).find('.alert-danger').should('contain.text', ruleLine)
        modules.moduleRow(moduleId).find('.actions > small').should('contain.text', ruleLine)
    })

    it('shows the error and the details of the state of the version as the module states them in the default theme', () => {
        cy.visit('/cms/adminframe/default/en/settings.manageModules.html?jahia.ui.theme=default')
        // The table shows one page of modules at a time.
        cy.get('input[aria-controls*="module_table"]').clear().type(moduleId)
        cy.get(`input[name="selectedModule"][value="${moduleId}"]`)
            .closest('tr')
            .within(() => {
                cy.get('.alert-error').should('contain.text', ruleLine)
                cy.get('.inactive-version').should('contain.text', ruleLine)
            })
    })
})
