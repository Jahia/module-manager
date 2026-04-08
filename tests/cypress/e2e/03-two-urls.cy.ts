import {DocumentNode} from 'graphql'
import {createSite, publishAndWaitJobEnding} from "@jahia/cypress";

describe('Two Forge URLs', () => {

    let addForgeUrl: DocumentNode
    addForgeUrl = require('graphql-tag/loader!../fixtures/graphql/mutation/addForgeUrl.graphql')
    const siteKey = 'mySite'

    before(() => {
        cy.login();
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'forge-mockup',
            serverName: 'jahia',
            locale: 'en',
        });
        publishAndWaitJobEnding('/sites/' + siteKey);
        cy.apolloClient({username: 'root', password: Cypress.env('SUPER_USER_PASSWORD')}).apollo({
            mutation: addForgeUrl
        });
    })

    it('Check the presence of a mockup module', () => {
        cy.login();
        cy.visit('/jahia/administration/manageModules');
        cy.visit('/cms/adminframe/default/en/settings.manageModules.html');
        cy.get('#available-modules-tab').click();
        cy.get('#availableModuleTabs i.material-icons').click();
        cy.get('#siteSettings input.form-control').click();
        cy.get('#siteSettings input.form-control').type('mockup');
        cy.contains('module-mockup').should('be.visible');
    })
})