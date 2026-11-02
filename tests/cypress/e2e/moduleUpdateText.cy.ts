import { ManageModulesPage } from '../page-object/ManageModulesPage'
import { fixtureJar, removeFixtureModules } from '../support/fixtureModules'

/**
 * An app store lists a newer version of an installed module, and the version and the download of that
 * entry hold the characters that HTML gives a meaning to, so the Modules screen shows and submits each
 * value exactly as the entry states it.
 */
describe('Update of an installed module whose app store entry holds special characters', () => {
    const moduleId = 'dummy1'
    const entry = {
        id: moduleId,
        name: moduleId,
        groupId: 'org.jahia.modules',
        version: '9.0.0-<rc1> "beta" été',
        downloadUrl: 'https://store.example/modules/dummy1-9.0.0.jar?label="beta"&note=l\'été',
    }

    before(() => {
        removeFixtureModules()
        cy.login()
        ManageModulesPage.visit()
            .upload(fixtureJar('dummy1-1.0.0.jar'), { autoStart: true, validateDefinitions: true })
            .expectUploadSuccess()
        cy.executeGroovy('groovy/addAvailableModule.groovy', {
            MODULE_ENTRY: Cypress.Buffer.from(JSON.stringify(entry)).toString('base64'),
        }).then((result) => expect(String(result)).not.to.contain('.failed'))
    })

    after(() => {
        cy.executeGroovy('groovy/reloadAvailableModules.groovy')
        removeFixtureModules()
    })

    beforeEach(() => {
        cy.login()
    })

    it('shows the version of the update and keeps its download as the entry states them', () => {
        const modules = ManageModulesPage.visit().search(moduleId)
        modules
            .moduleRow(moduleId)
            .find('.updateNotif small')
            .should(($version) => expect($version.text().trim()).to.eq(entry.version))
        modules.moduleRow(moduleId).find('input[name="moduleUrl"]').should('have.value', entry.downloadUrl)
    })

    it('shows the version of the update and keeps its download as the entry states them in the default theme', () => {
        cy.visit('/cms/adminframe/default/en/settings.manageModules.html?jahia.ui.theme=default')
        // The table shows one page of modules at a time.
        cy.get('input[aria-controls*="module_table"]').clear().type(moduleId)
        cy.get(`input[name="selectedModule"][value="${moduleId}"]`)
            .closest('tr')
            .within(() => {
                cy.contains(`Update available to version ${entry.version}!`).should('exist')
                cy.get('input[name="moduleUrl"]').should('have.value', entry.downloadUrl)
            })
    })
})
