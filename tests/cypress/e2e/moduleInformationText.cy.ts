import { MANAGE_MODULES_IFRAME, ManageModulesPage, manageModulesFrame } from '../page-object/ManageModulesPage'
import { fixtureJar, removeFixtureModules } from '../support/fixtureModules'

/**
 * The manifests of labelstestmodule and of labelsdependentmodule, which depends on it, hold the
 * characters that HTML gives a meaning to, so the screen shows each value exactly as the manifest
 * states it.
 */
describe('Information of a module whose manifest holds special characters', () => {
    const moduleId = 'labelstestmodule'
    const manifest = {
        name: 'Reports & <Labels> "beta" été',
        version: '1.0.0-<rc1>',
        groupId: 'org.example.<labels>',
        moduleType: '<beta> module',
        vendor: 'Labels & <Co> "team" été',
        description: 'Shows <labels> & "notes" été',
    }
    const dependent = { id: 'labelsdependentmodule', name: 'Notes & <Dependent> "beta" été', version: '1.0.0' }

    /** Declines the next confirmation of the screen, and records the message it shows. */
    const declineConfirmation = () =>
        cy
            .get(MANAGE_MODULES_IFRAME)
            .its('0.contentWindow')
            .then((frameWindow) => cy.stub(frameWindow, 'confirm').as('confirm').returns(false))

    const expectConfirmation = (text: string) => {
        cy.get('@confirm').should('have.been.calledOnce')
        cy.get('@confirm').its('firstCall.args.0').should('contain', text)
    }

    before(() => {
        removeFixtureModules()
        cy.login()
        for (const jar of ['labelstestmodule-1.0.0.jar', 'labelsdependentmodule-1.0.0.jar']) {
            ManageModulesPage.visit()
                .upload(fixtureJar(jar), { autoStart: true, validateDefinitions: true })
                .expectUploadSuccess()
        }
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
        details.infoValues().should(($values) => {
            expect(Cypress._.map($values, (value) => value.textContent.trim())).to.include.members([
                moduleId,
                manifest.groupId,
                manifest.moduleType,
                manifest.vendor,
                manifest.description,
            ])
        })
        details.versionsTable().should('contain.text', manifest.version)
    })

    it('names the modules wired to each other as their manifests state them in their details', () => {
        const details = ManageModulesPage.visit().openDetails(moduleId)
        details.moduleChips().should('contain.text', dependent.name)
        details.wiringLists().should('contain.text', `${dependent.name} (${dependent.version})`)

        ManageModulesPage.visit().openDetails(dependent.id).moduleChips().should('contain.text', manifest.name)
    })

    it('names the module as the manifest states it when asking to confirm that it stops', () => {
        const modules = ManageModulesPage.visit().search(moduleId)
        // The confirmation is declined, so the module stays started.
        declineConfirmation()
        modules.versionChip(moduleId, manifest.version).find('[data-sel-role="stop"]').click()
        expectConfirmation(`${manifest.name} (${manifest.version})`)
    })

    it('names the module as the manifest states it when asking to confirm that it is uninstalled', () => {
        // The screen offers to uninstall a stopped module, so the confirmation of the stop is accepted.
        // The case leaves the dependent module stopped, so it stays the last case of the spec.
        ManageModulesPage.visit().search(dependent.id)
        cy.get(MANAGE_MODULES_IFRAME)
            .its('0.contentWindow')
            .then((frameWindow) => cy.stub(frameWindow, 'confirm').returns(true))
        new ManageModulesPage().versionChip(dependent.id, dependent.version).find('[data-sel-role="stop"]').click()
        manageModulesFrame().should('contain.text', 'has been stopped')

        const modules = ManageModulesPage.visit().search(dependent.id)
        // The confirmation is declined, so the module stays installed.
        declineConfirmation()
        modules.versionChip(dependent.id, dependent.version).find('button[name="_eventId_undeployModule"]').click()
        expectConfirmation(`${dependent.name} (${dependent.version})`)
    })
})
