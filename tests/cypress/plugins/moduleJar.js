// Builds the jar of a minimal Jahia module in memory, so a spec can upload a module whose id,
// version, dependencies and definitions it chooses, with no binary fixture in the repository.
const JSZip = require('jszip')

// A manifest line holds at most 72 bytes, and a longer value continues on lines that start with a space.
const manifestLine = (name, value) => {
    const line = `${name}: ${value}`
    const chunks = [line.slice(0, 72)]
    for (let i = 72; i < line.length; i += 71) {
        chunks.push(' ' + line.slice(i, i + 71))
    }
    return chunks.join('\r\n')
}

// Maven 1.0.0-SNAPSHOT is OSGi 1.0.0.SNAPSHOT
const toOsgiVersion = (version) => version.replace('-', '.')

/**
 * @param {object} module
 * @param {string} module.groupId the group of the module, which must stay out of org.jahia.modules
 *     because Jahia refuses an unsigned module of that group
 * @param {string} module.id the module id, which is also the bundle symbolic name
 * @param {string} module.version a Maven version, such as 1.0.0 or 1.0.0-SNAPSHOT
 * @param {string[]} [module.depends] entries of Jahia-Depends besides default, such as other-module=2
 * @param {string} [module.definitions] the content of META-INF/definitions.cnd
 * @returns {Promise<string>} the jar, base64-encoded
 */
const buildModuleJar = async ({ groupId, id, version, depends = [], definitions }) => {
    const headers = [
        ['Manifest-Version', '1.0'],
        ['Bundle-ManifestVersion', '2'],
        ['Bundle-SymbolicName', id],
        ['Bundle-Name', id],
        ['Bundle-Version', toOsgiVersion(version)],
        ['Implementation-Version', version],
        ['Bundle-Vendor', 'Module Manager Cypress tests'],
        ['Bundle-Category', 'jahia-module'],
        ['Jahia-GroupId', groupId],
        ['Jahia-Module-Type', 'module'],
        ['Jahia-Required-Version', '8.0.0.0'],
        ['Jahia-Depends', ['default', ...depends].join(',')],
    ]
    const manifest = headers.map(([name, value]) => manifestLine(name, value)).join('\r\n') + '\r\n\r\n'

    const zip = new JSZip()
    // The manifest comes first, which is where java.util.jar.JarInputStream looks for it.
    zip.file('META-INF/MANIFEST.MF', manifest)
    if (definitions) {
        zip.file('META-INF/definitions.cnd', definitions)
    }
    return zip.generateAsync({ type: 'base64', compression: 'DEFLATE' })
}

module.exports = { buildModuleJar }
