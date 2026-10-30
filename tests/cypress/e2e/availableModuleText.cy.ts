import { AvailableModulesPage } from '../page-object/ManageModulesPage'

/**
 * The app store entry of a module holds the characters that HTML gives a meaning to, so the
 * Available modules tab shows and submits each value exactly as the entry states it.
 */
describe('Available module whose app store entry holds special characters', () => {
    const token = 'storelabels'
    const entry = {
        id: `${token}-<beta>`,
        name: 'Store & <Labels> "beta" été',
        groupId: 'org.example.<labels>',
        version: '1.0.0-<rc1>',
        icon: 'https://store.example/icons/storelabels.png?size=32&label="beta"',
        remoteUrl: 'https://store.example/modules/storelabels.html?dx=true&title="beta"&note=l\'été',
        downloadUrl: 'https://store.example/modules/storelabels-1.0.0.jar?label="beta"&note=l\'été',
    }

    before(() => {
        cy.executeGroovy('groovy/addAvailableModule.groovy', {
            MODULE_ENTRY: Cypress.Buffer.from(JSON.stringify(entry)).toString('base64'),
        }).then((result) => expect(String(result)).not.to.contain('.failed'))
    })

    after(() => {
        cy.executeGroovy('groovy/reloadAvailableModules.groovy')
    })

    beforeEach(() => {
        cy.login()
    })

    it('shows the name, the id, the version and the icon of the module as its entry states them', () => {
        const modules = AvailableModulesPage.visit().search(token)
        modules.moduleRow(token).find('td').first().find('b').should('have.text', entry.name)
        modules.moduleRow(token).find('td').first().find('span').should('contain.text', `(${entry.id})`)
        modules.moduleRow(token).find('td').first().find('img').should('have.attr', 'src', entry.icon)
        modules
            .moduleRow(token)
            .find('td')
            .eq(1)
            .should(($cell) => expect($cell.text().trim()).to.eq(entry.version))
    })

    it('keeps the page and the download of the module as its entry states them', () => {
        const modules = AvailableModulesPage.visit().search(token)
        modules.moduleRow(token).find('input[name="remoteUrl"]').first().should('have.value', entry.remoteUrl)
        modules.moduleRow(token).find('input[name="moduleUrl"]').should('have.value', entry.downloadUrl)
    })

    it('opens the page of the module as its entry states it in the details', () => {
        const modules = AvailableModulesPage.visit().search(token)
        modules.moduleRow(token).find('td').first().find('b').click()
        modules.detailsFrame().should('have.attr', 'src', entry.remoteUrl)
    })
})
