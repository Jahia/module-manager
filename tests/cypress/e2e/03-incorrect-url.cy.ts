import {DocumentNode} from 'graphql';

describe('Incorrect Forge URL', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const setIncorrectForgeUrl: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/setIncorrectForgeUrl.graphql');
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const setCorrectForgeUrl: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/setCorrectForgeUrl.graphql');

    before(() => {
        cy.login();
        cy.apollo({
            mutation: setIncorrectForgeUrl
        });
    });

    it('Check the absence of a "core" module', () => {
        cy.login();
        cy.visit('/jahia/administration/manageModules');
        cy.visit('/cms/adminframe/default/en/settings.manageModules.html');
        cy.get('#available-modules-tab').click();
        cy.get('#availableModuleTabs i.material-icons').click();
        cy.get('#siteSettings input.form-control').clear();
        cy.get('#siteSettings input.form-control').type('jontent');
        cy.contains('jContent').should('not.exist');
    });

    after(() => {
        cy.login();
        cy.apollo({
            mutation: setCorrectForgeUrl
        });
    });
});
