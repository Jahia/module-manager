import { jfaker } from '@jahia/cypress'

/** The group of every module the specs build, so a cleanup can find all of them, a failed run's included. */
export const FIXTURE_GROUP_ID = 'org.example.modulemanager'

export type FixtureModule = {
    id: string
    version: string
    /** Entries of Jahia-Depends besides default, such as `other-module=2` */
    depends?: string[]
    /** The content of META-INF/definitions.cnd */
    definitions?: string
}

/** Lowercase letters only, so it fits a module id, a site key and a CND namespace prefix alike. */
export const uniqueToken = () => jfaker.string.alpha({ length: 8, casing: 'lower' })

/** Builds the jar of a fixture module, in the shape `selectFile` takes. */
export const fixtureJar = (module: FixtureModule) =>
    cy.task<string>('buildModuleJar', { ...module, groupId: FIXTURE_GROUP_ID }).then((base64) => ({
        contents: Cypress.Buffer.from(base64, 'base64'),
        fileName: `${module.id}-${module.version}.jar`,
        mimeType: 'application/java-archive',
    }))

export const removeFixtureModules = () =>
    cy.executeGroovy('groovy/removeFixtureModules.groovy', { GROUP_ID: FIXTURE_GROUP_ID })
