import { context, createSite, deleteSite } from '@jahia/cypress'
import { ManageModulesPage } from '../page-object/ManageModulesPage'
import { SiteModulesPage } from '../page-object/SiteModulesPage'
import { FixtureModule, fixtureJar, removeFixtureModules, uniqueToken } from '../support/fixtureModules'

/**
 * Migrated from the legacy Selenium suite: ModulesManagementTest.updateModuleVersion
 * (Jahia/selenium ModulesManagementTest.java:104-136). Tracking issue: Jahia/selenium#1629, FT-004 to FT-006.
 *
 * The tests are the ordered steps of one journey: each one acts on the module that the previous one
 * installed.
 */
describe('Update of the version of a module used by a site', () => {
    const token = uniqueToken()
    const siteKey = `mmversion${token}`
    const moduleId = `mmversioning${token}`
    // The prefix and the URI of a namespace stay registered once a module brought them, so each run brings its own.
    const definitions = (valueType: string) =>
        [
            "<jnt = 'http://www.jahia.org/jahia/nt/1.0'>",
            "<jmix = 'http://www.jahia.org/jahia/mix/1.0'>",
            `<mm${token} = 'http://www.example.org/modulemanager/${token}/1.0'>`,
            '',
            `[mm${token}:item] > jnt:content, jmix:structuredContent`,
            ` - value (${valueType})`,
            '',
        ].join('\n')
    const version1: FixtureModule = { id: moduleId, version: '1.0.0-SNAPSHOT', definitions: definitions('string') }
    // A property that changes its type is a major change of the definitions.
    const version2: FixtureModule = { id: moduleId, version: '2.0.0-SNAPSHOT', definitions: definitions('long') }

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

    it('FT-004: shows the version of a module uploaded and enabled on a site in the modules of the site', () => {
        context.tag('module-manager', 'upload', 'enable-on-site', 'version', 'testrail-case-2071208')
        fixtureJar(version1).then((jar) => {
            ManageModulesPage.visit().upload(jar, { autoStart: true, validateDefinitions: true })
        })
        ManageModulesPage.visit().openDetails(moduleId).enableOnSite(siteKey)

        SiteModulesPage.visit(siteKey).expectVersion(moduleId, version1.version)
    })

    it('FT-005: refuses the upload of a version that brings a major change in the definitions', () => {
        context.tag('module-manager', 'upload', 'definition-check', 'refused', 'testrail-case-2071208')
        fixtureJar(version2).then((jar) => {
            ManageModulesPage.visit()
                .upload(jar, { autoStart: false, validateDefinitions: true })
                .expectUploadError('Module upload failed: Major change in definition')
        })

        const modules = ManageModulesPage.visit().search(moduleId)
        modules.versionChip(moduleId, version1.version).should('exist')
        modules.versionChip(moduleId, version2.version).should('not.exist')
    })

    it('FT-006: makes the site run the new version once uploaded without the definition check and started', () => {
        context.tag(
            'module-manager',
            'upload',
            'definition-check',
            'disabled',
            'version-upgrade',
            'testrail-case-2071208',
        )
        // Without auto start in both operating modes, so the test always starts the version itself,
        // the way the legacy test did outside development mode.
        fixtureJar(version2).then((jar) => {
            ManageModulesPage.visit().upload(jar, { autoStart: false, validateDefinitions: false })
        })
        ManageModulesPage.visit().search(moduleId).startVersion(moduleId, version2.version)

        SiteModulesPage.visit(siteKey).expectVersion(moduleId, version2.version)
    })
})
