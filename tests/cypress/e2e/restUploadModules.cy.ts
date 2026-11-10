import { removeFixtureModules } from '../support/fixtureModules'

/**
 * Upload of modules through the REST API (POST /modules/api/bundles). A JavaScript module package (.tgz) goes
 * through the url transformer of javascript-modules-engine, and a jar does not (Jahia/module-manager#220).
 * Each test starts from a Jahia without the fixture modules, with the engine started.
 */
describe('Upload of modules through the REST API', () => {
    const TGZ = 'org.jahia.modules.javascript/tgz-upload-test'
    const JAR = 'org.jahia.modules/dummy1'
    const ENGINE = 'org.jahia.modules/javascript-modules-engine'
    const API = '/modules/api/bundles'

    type Part = { fileName: string; content?: string }
    type UploadResult = { status: number; body: { bundleInfos?: { key: string }[]; message?: string; cause?: string } }

    const auth = () => ({ user: 'root', pass: Cypress.env('SUPER_USER_PASSWORD'), sendImmediately: true })

    /** A multipart body comes back as an ArrayBuffer, so the response is decoded before it is read. */
    const upload = (parts: Part[]): Cypress.Chainable<UploadResult> => {
        const formData = new FormData()
        formData.append('start', 'true')
        parts.forEach(({ fileName, content }) => {
            if (content !== undefined) {
                formData.append('bundle', Cypress.Blob.binaryStringToBlob(content), fileName)
            } else {
                cy.fixture(`assets/moduleManagement/${fileName}`, 'binary').then((fixture) =>
                    formData.append('bundle', Cypress.Blob.binaryStringToBlob(fixture), fileName),
                )
            }
        })
        return cy
            .then(() => cy.request({ method: 'POST', url: API, auth: auth(), body: formData, failOnStatusCode: false }))
            .then((res) => ({ status: res.status, body: JSON.parse(new TextDecoder().decode(res.body)) }))
    }

    /** The module state of every installed version of a bucket, by version. */
    const states = (bucket: string): Cypress.Chainable<Record<string, string>> =>
        cy.request({ url: `${API}/${bucket}/*/_localInfo`, auth: auth() }).then((res) => {
            const infos = res.body as Record<string, { moduleState: string }>
            const byVersion: Record<string, string> = {}
            Object.keys(infos).forEach((key) => (byVersion[key.split('/').pop()] = infos[key].moduleState))
            return byVersion
        })

    /**
     * Polls the states until they match: the REST call returns once the bundle is started, but a Spring module
     * then goes through SPRING_STARTING before its module state reads STARTED.
     */
    const expectStates = (bucket: string, expected: Record<string, string>, attempts = 20) =>
        states(bucket).then((actual) => {
            if (attempts <= 1 || Cypress._.isEqual(actual, expected)) {
                expect(actual).to.deep.eq(expected)
            } else {
                // The interval of the poll, not a guess at how long the start takes (repeatUntil only polls the DOM).
                // eslint-disable-next-line cypress/no-unnecessary-waiting
                cy.wait(500)
                expectStates(bucket, expected, attempts - 1)
            }
        })

    const operate = (key: string, operation: '_start' | '_stop') =>
        cy.request({ method: 'POST', url: `${API}/${key}/${operation}`, auth: auth(), form: true, body: {} })

    const keys = (result: UploadResult) => result.body.bundleInfos.map((info) => info.key)

    afterEach(() => {
        removeFixtureModules()
        // The last test stops the engine: start it again, even when that test failed halfway.
        states(ENGINE).then((engine) =>
            Object.keys(engine)
                .filter((version) => engine[version] !== 'STARTED')
                .forEach((version) => operate(`${ENGINE}/${version}`, '_start')),
        )
    })

    it('installs and starts a JavaScript module package', () => {
        upload([{ fileName: 'tgz-upload-test-1.0.0.tgz' }]).then((result) => {
            expect(result.status).to.eq(200)
            expect(keys(result)).to.deep.eq([`${TGZ}/1.0.0`])
        })
        expectStates(TGZ, { '1.0.0': 'STARTED' })
    })

    it('upgrades a JavaScript module package', () => {
        upload([{ fileName: 'tgz-upload-test-1.0.0.tgz' }])
            .its('status')
            .should('eq', 200)
        expectStates(TGZ, { '1.0.0': 'STARTED' })

        upload([{ fileName: 'tgz-upload-test-1.1.0.tgz' }]).then((result) => {
            expect(result.status).to.eq(200)
            expect(keys(result)).to.deep.eq([`${TGZ}/1.1.0`])
        })
        expectStates(TGZ, { '1.0.0': 'INSTALLED', '1.1.0': 'STARTED' })
    })

    it('updates a JavaScript module package uploaded again with the same version', () => {
        upload([{ fileName: 'tgz-upload-test-1.1.0.tgz' }])
            .its('status')
            .should('eq', 200)
        expectStates(TGZ, { '1.1.0': 'STARTED' })

        upload([{ fileName: 'tgz-upload-test-1.1.0.tgz' }]).then((result) => {
            expect(result.status).to.eq(200)
            expect(keys(result)).to.deep.eq([`${TGZ}/1.1.0`])
        })
        expectStates(TGZ, { '1.1.0': 'STARTED' })
    })

    it('installs and starts a jar', () => {
        upload([{ fileName: 'dummy1-1.0.0.jar' }]).then((result) => {
            expect(result.status).to.eq(200)
            expect(keys(result)).to.deep.eq([`${JAR}/1.0.0`])
        })
        expectStates(JAR, { '1.0.0': 'STARTED' })
    })

    it('installs a jar and a JavaScript module package uploaded in one request', () => {
        upload([{ fileName: 'dummy1-1.0.0.jar' }, { fileName: 'tgz-upload-test-1.1.0.tgz' }]).then((result) => {
            expect(result.status).to.eq(200)
            expect(keys(result)).to.have.members([`${JAR}/1.0.0`, `${TGZ}/1.1.0`])
        })
        expectStates(JAR, { '1.0.0': 'STARTED' })
        expectStates(TGZ, { '1.1.0': 'STARTED' })
    })

    it('rejects a .tgz that is not a gzip archive with its cause', () => {
        upload([{ fileName: 'tgz-broken-1.0.0.tgz', content: 'not a gzip archive' }]).then((result) => {
            expect(result.status).to.eq(500)
            expect(result.body.cause).to.contain('Not in GZIP format')
        })
    })

    // This asserts the current answer, which does not say that the engine is missing. The module-manager UI shows
    // a dedicated "missing Javascript engine" message for the same upload; whether the REST API should do the same
    // is an open question on Jahia/module-manager#220. Update the expected message if the API gets one.
    it('rejects a JavaScript module package while javascript-modules-engine is stopped', () => {
        states(ENGINE).then((engine) =>
            Object.keys(engine).forEach((version) => operate(`${ENGINE}/${version}`, '_stop')),
        )
        states(ENGINE).then((engine) =>
            expect(Object.keys(engine).map((version) => engine[version])).to.deep.eq(['RESOLVED']),
        )

        upload([{ fileName: 'tgz-upload-test-1.1.0.tgz' }]).then((result) => {
            expect(result.status).to.eq(400)
            expect(result.body.message).to.contain('not a valid OSGi bundle')
        })
        expectStates(TGZ, {})
    })
})
