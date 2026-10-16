import { BasePage } from '@jahia/cypress'

/** The Modules screen of the settings of a site, which lists the modules the site uses. */
export class SiteModulesPage extends BasePage {
    static visit(siteKey: string) {
        cy.visit(`/cms/editframe/default/en/sites/${siteKey}.manageModules.html`)
        return new SiteModulesPage()
    }

    /** A row reads: position, name, id, group, version, type, vendor. */
    moduleRow(moduleId: string) {
        return cy.contains('tr', moduleId)
    }

    expectVersion(moduleId: string, version: string) {
        this.moduleRow(moduleId).find('td').eq(4).should('have.text', version)
        return this
    }
}
