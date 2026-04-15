import {DocumentNode} from 'graphql'

describe('Patch check', () => {
    let getForgeUrl: DocumentNode
    let getNodeTypeByName: DocumentNode
    getForgeUrl = require('graphql-tag/loader!../fixtures/graphql/query/getForgeUrl.graphql')
    getNodeTypeByName = require('graphql-tag/loader!../fixtures/graphql/query/getNodeTypeByName.graphql')

    it('Check the migration of the forge settings to configuration files', () => {
        cy.login();
        cy.executeGroovy('checkTempFolderMarker.groovy').then((result: any) => {
            // .installed if the marker file is there, otherwise an exception is thrown and the result would be .failed
            if(result === ".installed"){
                console.log("Migration has been done")
                cy.apollo({
                    query: getForgeUrl,
                    variables: {
                        "identifier": "default"
                    }
                }).should((response: any) => {
                    expect(response.data.admin.jahia.configuration.value).to.equal('https://store.jahia.com/en/sites/private-app-store');
                })
                cy.apollo({
                    query: getForgeUrl,
                    variables: {
                        "identifier": "httpcustomstore"
                    }
                }).should((response: any) => {
                    expect(response.data.admin.jahia.configuration.value).to.equal('https://store.jahia.org/en/sites/private-app-store');
                })
            }else{
                console.log("Migration has not been done")
            }
        });
    })

    it('Check that the legacy nodetypes are not present anymore', () => {
        cy.login();
        cy.apollo({
            errorPolicy: 'all',
            query: getNodeTypeByName,
            variables: {
                "name": "jnt:forgeServerSettings"
            }
        }).should(response => {
            expect(response.errors[0].message).to.contain('javax.jcr.nodetype.NoSuchNodeTypeException: Unknown type : jnt:forgeServerSettings');
        });
        cy.apollo({
            errorPolicy: 'all',
            query: getNodeTypeByName,
            variables: {
                "name": "jnt:forgesServerSettings"
            }
        }).should(response => {
            expect(response.errors[0].message).to.contain('javax.jcr.nodetype.NoSuchNodeTypeException: Unknown type : jnt:forgesServerSettings');
        });
        cy.apollo({
            errorPolicy: 'all',
            query: getNodeTypeByName,
            variables: {
                "name": "jnt:serverSettingsManageForges"
            }
        }).should(response => {
            expect(response.errors[0].message).to.contain('javax.jcr.nodetype.NoSuchNodeTypeException: Unknown type : jnt:serverSettingsManageForges');
        });
    })
})