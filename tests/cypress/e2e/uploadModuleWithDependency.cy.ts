import { context } from '@jahia/cypress'
import { ManageModulesPage } from '../page-object/ManageModulesPage'
import { FixtureModule, fixtureJar, removeFixtureModules, uniqueToken } from '../support/fixtureModules'

/**
 * The tests are the ordered steps of one journey: each one uploads a module, and the next one starts
 * from what the previous upload installed.
 */
describe('Upload of a module with a dependency', () => {
    const token = uniqueToken()
    const dependency = (version: string): FixtureModule => ({ id: `mmdependency${token}`, version })
    const dependsOn = [`mmdependency${token}`, `mmdependency${token}=2`]
    const dependent: FixtureModule = { id: `mmdependent${token}`, version: '3.1.0-SNAPSHOT', depends: dependsOn }

    // The uploads start what they install, so the journey is the same in development mode and in production mode.
    const upload = (module: FixtureModule) =>
        fixtureJar(module).then((jar) => {
            ManageModulesPage.visit().upload(jar, { autoStart: true, validateDefinitions: true })
        })

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
        context.tag('module-manager', 'upload', 'dependency', 'unresolved', 'testrail-case-3183463')
        upload(dependent)

        const modules = ManageModulesPage.visit().search(dependent.id)
        modules.moduleRow(dependent.id).should('contain.text', `Depends on: ${dependsOn.join(', ')}`)
        modules.moduleRow(dependent.id).find('[data-sel-role="status-started"]').should('not.exist')
        modules.moduleRow(dependent.id).find('[data-sel-role^="status-"]').should('exist')
    })

    it('keeps the module unresolved when the wrong version of the dependency is uploaded', () => {
        context.tag('module-manager', 'upload', 'dependency', 'wrong-version', 'testrail-case-3183463')
        upload(dependency('1.0.0'))

        const modules = ManageModulesPage.visit().search(dependent.id)
        modules.moduleRow(dependent.id).should('contain.text', `Depends on: mmdependency${token}=2`)
        modules.moduleRow(dependent.id).should('not.contain.text', `Depends on: mmdependency${token},`)
        modules.moduleRow(dependent.id).find('[data-sel-role="status-started"]').should('not.exist')
        modules.moduleRow(dependent.id).find('[data-sel-role^="status-"]').should('exist')
    })

    it('starts the module when the required version of the dependency is uploaded', () => {
        context.tag('module-manager', 'upload', 'dependency', 'resolved', 'started', 'testrail-case-3183463')
        upload(dependency('2.0.0'))

        const modules = ManageModulesPage.visit().search(dependent.id)
        modules.moduleRow(dependent.id).find('[data-sel-role="status-started"]').should('exist')
        modules.moduleRow(dependent.id).should('not.contain.text', 'Depends on')
    })
})
