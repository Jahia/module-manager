describe('Correct Forge URL', () => {
    before(() => {
        cy.login()
    })

    it('Check the presence of a "core" module', () => {
        cy.login();
        cy.visit('/jahia/administration/manageModules');
        cy.visit('/cms/adminframe/default/en/settings.manageModules.html');
        cy.get('#available-modules-tab').click();
        cy.get('#availableModuleTabs i.material-icons').click();
        cy.get('#siteSettings input.form-control').clear();
        cy.get('#siteSettings input.form-control').type('jcontent');
        cy.get('#siteSettings b').should('have.text', 'jContent');
    })

    it('Check the download of a Jahia module', () => {
        cy.login();
        cy.visit('/jahia/administration/manageModules');
        cy.visit('/cms/adminframe/default/en/settings.manageModules.html');
        cy.get('#available-modules-tab').click();
        cy.get('#siteSettings input.form-control').type('addstuff');
        cy.contains('AddStuff').should('be.visible') ;
        cy.get('#siteSettings [name="_eventId_installForgeModule"] i.material-icons').click();
        cy.get('#installed-modules-tab').click();
        cy.get('#siteSettings input.form-control').click();
        cy.get('#siteSettings input.form-control').type('addstuff');
        cy.contains('addStuff').should('be.visible');
    })
})