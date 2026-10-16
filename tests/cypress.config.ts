import { defineConfig } from 'cypress'

export default defineConfig({
    chromeWebSecurity: false,
    defaultCommandTimeout: 30000,
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
        configFile: 'reporter-config.json',
    },
    screenshotsFolder: './results/screenshots',
    videosFolder: './results/videos',
    viewportWidth: 1366,
    viewportHeight: 768,
    redirectionLimit: 60,
    downloadsFolder: '/tmp',
    e2e: {
        // We've imported your old cypress plugins here.
        // You may want to clean this up later by importing these.
        setupNodeEvents(on, config) {
            // eslint-disable-next-line @typescript-eslint/no-var-requires
            return require('./cypress/plugins/index.js')(on, config)
        },
        excludeSpecPattern: ['**/*.example.ts', '**/*.disabled.ts', '**/quarantined/*'],
        baseUrl: 'http://localhost:8080',
    },
})
