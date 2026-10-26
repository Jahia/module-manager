import { MANAGE_MODULES_IFRAME, ManageModulesPage } from '../page-object/ManageModulesPage'
import { fixtureJar, removeFixtureModules } from '../support/fixtureModules'

/**
 * The manifest of labelstestmodule holds the characters that HTML gives a meaning to, so the screen
 * shows each value exactly as the manifest states it.
 */
describe('Information of a module whose manifest holds special characters', () => {
    const moduleId = 'labelstestmodule'
    const text = 'Reports & <Labels> "beta" été'
    const manifest = {
        name: text,
        version: '1.0.0-<rc1>',
        groupId: 'org.example.<labels>',
        moduleType: '<beta> module',
        vendor: text,
        description: text,
    }

    before(() => {
        removeFixtureModules()
        cy.login()
        ManageModulesPage.visit()
            .upload(fixtureJar('labelstestmodule-1.0.0.jar'), { autoStart: true, validateDefinitions: true })
            .expectUploadSuccess()
    })

    after(() => {
        removeFixtureModules()
    })

    beforeEach(() => {
        cy.login()
    })

    it('shows the name and the version of the module as the manifest states them in the list of modules', () => {
        const modules = ManageModulesPage.visit().search(moduleId)
        modules.moduleName(moduleId).should('have.text', manifest.name)
        modules.moduleRow(moduleId).find('[data-sel-role^="status-"]').should('contain.text', manifest.version)
    })

    it('shows the information of the module as the manifest states it in its details', () => {
        const details = ManageModulesPage.visit().openDetails(moduleId)
        details.title().should('contain.text', manifest.name)
        details
            .infoValues()
            .should('include.members', [
                moduleId,
                manifest.groupId,
                manifest.moduleType,
                manifest.vendor,
                manifest.description,
            ])
        details.versionsTable().should('contain.text', manifest.version)
    })

    it('names the module as the manifest states it when asking to confirm that it stops', () => {
        const modules = ManageModulesPage.visit().search(moduleId)
        // The confirmation is declined, so the module stays started.
        cy.get(MANAGE_MODULES_IFRAME)
            .its('0.contentWindow')
            .then((frameWindow) => cy.stub(frameWindow, 'confirm').as('confirm').returns(false))
        modules.versionChip(moduleId, manifest.version).find('[data-sel-role="stop"]').click()
        cy.get('@confirm').should('have.been.calledOnce')
        cy.get('@confirm').its('firstCall.args.0').should('contain', `${manifest.name} (${manifest.version})`)
    })
})
