import {DocumentNode} from 'graphql';
import {createSite, publishAndWaitJobEnding, deleteSite} from '@jahia/cypress';

describe('Two Forge URLs with one private', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addForgeUrl: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/addForgeUrl.graphql');
    const revokeUser: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/revokeUser.graphql');
    const siteKey = 'mySite';
    const sitePath = `/sites/` + siteKey;
    const principal = "u:guest";


    before(() => {
        cy.login();
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'forge-mockup',
            serverName: 'jahia',
            locale: 'en'
        });

        cy.apollo({
            mutation: revokeUser,
            variables: {
                pathOrId: sitePath + '/j:acl',
                name: `DENY_${principal.replace(':', '_')}`,
                principal: principal
            }
        });
        publishAndWaitJobEnding(sitePath);
        cy.apollo({
            mutation: addForgeUrl,
            variables: {
                username: 'root',
                password: Cypress.env('SUPER_USER_PASSWORD')
            }
        });
    });

    after(() => {
        cy.login();
        deleteSite(siteKey);
    });

    it('Check the presence of a mockup module', () => {
        // Check that the site holding the forge mockup is private
        cy.logout();
        cy.request({
            url: `http://jahia:8080`,
            failOnStatusCode: false
        }).its('status').should('eq', 401)
        // Check that the json file is not seen as quest
        cy.request({
            url: `http://jahia:8080/contents/modules-repository.moduleList.json`,
            failOnStatusCode: false
        }).its('status').should('eq', 404)

        cy.login();
        cy.visit('/jahia/administration/manageModules');
        cy.visit('/cms/adminframe/default/en/settings.manageModules.html');
        cy.get('#available-modules-tab').click();
        cy.get('#availableModuleTabs i.material-icons').click();
        cy.get('#siteSettings input.form-control').click();
        cy.get('#siteSettings input.form-control').type('mockup');
        cy.contains('module-mockup').should('be.visible');
    });
});
