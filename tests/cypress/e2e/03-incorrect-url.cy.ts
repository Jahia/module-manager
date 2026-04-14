import {DocumentNode} from 'graphql'

describe('Incorrect Forge URL', () => {
    let setIncorrectForgeUrl: DocumentNode
    let setCorrectForgeUrl: DocumentNode
    setIncorrectForgeUrl = require('graphql-tag/loader!../fixtures/graphql/mutation/setIncorrectForgeUrl.graphql')
    setCorrectForgeUrl = require('graphql-tag/loader!../fixtures/graphql/mutation/setCorrectForgeUrl.graphql')

    before(() => {
        cy.login();
        cy.apollo({
            mutation: setIncorrectForgeUrl
        });
    })

    it('Check the absence of a "core" module', () => {
        cy.login();
        cy.visit('/jahia/administration/manageModules');
        cy.visit('/cms/adminframe/default/en/settings.manageModules.html');
        cy.get('#available-modules-tab').click();
        cy.get('#availableModuleTabs i.material-icons').click();
        cy.get('#siteSettings input.form-control').clear();
        cy.get('#siteSettings input.form-control').type('jontent');
        cy.contains('jContent').should('not.exist');
    })

    after(() => {
        cy.login();
        cy.apollo({
            mutation: setCorrectForgeUrl
        });
    })
})