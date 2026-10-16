import { BasePage } from '@jahia/cypress'

/** The Modules screen of the administration renders in this iframe. */
export const MANAGE_MODULES_IFRAME = 'iframe[src*="manageModules.html"]'

/**
 * Resolves the body of the admin iframe as a retryable query chain.
 *
 * Every action of the screen is a POST that replaces the document of the iframe, so a held
 * reference goes stale. `cy.get()` and `.its()` are queries, so Cypress retries the whole chain,
 * including the `.find()` the caller appends. No assertion here: one would end the query group and
 * freeze the body. The assertion of the caller drives the retry.
 */
export const manageModulesFrame = () => cy.get(MANAGE_MODULES_IFRAME, { timeout: 30000 }).its('0.contentDocument.body')

export type UploadOptions = {
    /** The "Start the module after upload" box, which the screen checks in development mode only */
    autoStart: boolean
    /** The "Validate definitions" box, which the screen checks by default */
    validateDefinitions: boolean
}

export class ManageModulesPage extends BasePage {
    static visit() {
        cy.visit('/jahia/administration/manageModules')
        return new ManageModulesPage().waitForTable()
    }

    /** The module table is a DataTable, so its search box exists once the table is initialized. */
    private waitForTable() {
        manageModulesFrame().find('input[aria-controls*="module_table"]').should('be.visible')
        return this
    }

    upload(
        jar: { contents: Buffer; fileName: string; mimeType: string },
        { autoStart, validateDefinitions }: UploadOptions,
    ) {
        manageModulesFrame().find('#moduleFileUpload').selectFile(jar, { force: true })
        // The change handler of the file input reveals the options and the upload button.
        manageModulesFrame().find('#btnUpload').should('be.visible')
        const setBox = (id: string, checked: boolean) =>
            checked
                ? manageModulesFrame().find(id).check({ force: true })
                : manageModulesFrame().find(id).uncheck({ force: true })
        setBox('#moduleAutoStart', autoStart)
        setBox('#moduleValidateDefinitions', validateDefinitions)
        manageModulesFrame().find('#btnUpload').click()
        // The screen answers the POST with the outcome of the upload, in an alert of the upload form.
        // Call this on a freshly visited screen, which holds no alert yet.
        manageModulesFrame().find('form#moduleFile .alert').should('exist')
        return this
    }

    expectUploadError(text: string) {
        manageModulesFrame().find('form#moduleFile .alert-danger').should('contain.text', text)
        return this
    }

    /** The search box filters the DataTable, which keeps only the rows of the current page in the DOM. */
    search(moduleId: string) {
        manageModulesFrame().find('input[aria-controls*="module_table"]').clear().type(moduleId)
        return this
    }

    moduleRow(moduleId: string) {
        return manageModulesFrame().find(`tr[data-sel-role="module-row-${moduleId}"]`)
    }

    /** The chip of one version of a module, which carries the actions of that version. */
    versionChip(moduleId: string, version: string) {
        return manageModulesFrame().find(`form[data-sel-module="${moduleId}"][data-sel-version="${version}"]`)
    }

    /** Starts one version, and waits for the screen to answer with that version started. */
    startVersion(moduleId: string, version: string) {
        this.versionChip(moduleId, version).find('[data-sel-role="start"]').click()
        this.moduleRow(moduleId).find('[data-sel-role="status-started"]').should('contain.text', version)
        return this
    }

    openDetails(moduleId: string) {
        this.search(moduleId)
        this.moduleRow(moduleId).find('td').first().click()
        return new ModuleDetailsPage()
    }
}

export class ModuleDetailsPage extends BasePage {
    enableOnSite(siteKey: string) {
        manageModulesFrame().find(`label[data-sel-role="siteEnabler-${siteKey}"] input`).check({ force: true })
        // Once enabled, the toggle of the site becomes the form that disables the module.
        manageModulesFrame().find(`#disableModuleForm${siteKey}`).should('exist')
        return this
    }
}
