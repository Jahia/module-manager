describe('Tests', () => {
    before(() => {
        cy.login()
    })
    it('First test', () => {
        cy.login()
        cy.visit('/jahia/administration')
    })
})