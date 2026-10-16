import { context, createSite, deleteSite } from '@jahia/cypress'
import { ManageModulesPage } from '../page-object/ManageModulesPage'
import { SiteModulesPage } from '../page-object/SiteModulesPage'
import { fixtureJar, removeFixtureModules, uniqueToken } from '../support/fixtureModules'

/**
 * The tests are the ordered steps of one journey: each one acts on the module that the previous one
 * installed.
 */
describe('Update of the version of a module used by a site', () => {
    const siteKey = `moduleVersionUpdate${uniqueToken()}`
    const moduleId = 'versioningtestmodule'
    // Version 2.0.0 changes the type of a property, which is a major change of the definitions.
    const version1 = { jar: 'versioningtestmodule-1.0.0-SNAPSHOT.jar', version: '1.0.0-SNAPSHOT' }
    const version2 = { jar: 'versioningtestmodule-2.0.0-SNAPSHOT.jar', version: '2.0.0-SNAPSHOT' }

    before(() => {
        removeFixtureModules()
        deleteSite(siteKey)
        createSite(siteKey, { templateSet: 'templates-system', serverName: 'localhost', locale: 'en' })
    })

    after(() => {
        deleteSite(siteKey)
        removeFixtureModules()
    })

    beforeEach(() => {
        cy.login()
    })

    it('shows the version of a module uploaded and enabled on a site in the modules of the site', () => {
        context.tag('module-manager', 'upload', 'enable-on-site', 'version')
        ManageModulesPage.visit().upload(fixtureJar(version1.jar), { autoStart: true, validateDefinitions: true })
        ManageModulesPage.visit().openDetails(moduleId).enableOnSite(siteKey)

        SiteModulesPage.visit(siteKey).expectVersion(moduleId, version1.version)
    })

    it('refuses the upload of a version that brings a major change in the definitions', () => {
        context.tag('module-manager', 'upload', 'definition-check', 'refused')
        ManageModulesPage.visit()
            .upload(fixtureJar(version2.jar), { autoStart: false, validateDefinitions: true })
            .expectUploadError('Module upload failed: Major change in definition')

        const modules = ManageModulesPage.visit().search(moduleId)
        modules.versionChip(moduleId, version1.version).should('exist')
        modules.versionChip(moduleId, version2.version).should('not.exist')
    })

    it('makes the site run the new version once uploaded without the definition check and started', () => {
        context.tag('module-manager', 'upload', 'definition-check', 'disabled', 'version-upgrade')
        // Without auto start in both operating modes, so the test always starts the version itself,
        // the way the legacy test did outside development mode.
        ManageModulesPage.visit().upload(fixtureJar(version2.jar), { autoStart: false, validateDefinitions: false })
        ManageModulesPage.visit().search(moduleId).startVersion(moduleId, version2.version)

        SiteModulesPage.visit(siteKey).expectVersion(moduleId, version2.version)
    })
})
