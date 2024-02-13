package projects.nngm.v2

import groovy.json.JsonSlurper

import static de.kairos.fhir.centraxx.metamodel.MultilingualEntry.LANG
import static de.kairos.fhir.centraxx.metamodel.MultilingualEntry.VALUE
import static de.kairos.fhir.centraxx.metamodel.RootEntities.organizationUnit

/**
 * Represents a CXX Organization
 * Specified by https://simplifier.net/nngm-form/profilenngmorganizationorganisation
 * Endpoint: https://nngm-nms.medicalsyn.com/api/v1.0/Public/Organization
 *
 * Currently this stays untested as we do not have the correct organizations.json inside of our CentraXX
 *
 * @author Timo Schneider
 * @since v.1.13.0, CXX.v.2022.1.0 exportable,
 * @since CXX.v.3.18.3.16, CXX.v.3.18.4.0, CXX.v.2023.1.0 importable
 */
organization {

    id = "Organization/" + context.source[organizationUnit().id()]

    meta {
        profile "http://uk-koeln.de/fhir/StructureDefinition/Organisation/nNGM"
    }

    final def normalizeOrganizationName = mappingList(context.source[organizationUnit().code()].toString())

    final def organizationInSystem = matchOrganization(normalizeOrganizationName)

    if (organizationInSystem.internalSequenceIdentifier) {
        identifier {
            system = "http://uk-koeln.de/fhir/sid/nNGM/nms-organization"
            value = organizationInSystem.internalSequenceIdentifier
        }
    } else {
        identifier {
            system = "urn:centraxx"
            value = context.source[organizationUnit().nameMultilingualEntries()]?.find { final def me -> me[LANG] == "de" }?.getAt(VALUE) as String
        }
    }

    active = true

    name = context.source[organizationUnit().nameMultilingualEntries()]?.find { final def me -> me[LANG] == "de" }?.getAt(VALUE) as String

}

class NNGMOrganization {
    String internalSequenceIdentifier
    String displayName
    boolean isActive
    boolean isDigiNet
    String networkPartnerIdentifier
}

/**
 * Trying to find a better approach than mapping the CentraXX organization to the given nNGM file
 * You may need to add a list of organizations.json that maps to your local organization {@see mappingList}
 * @param firstName
 * @param lastName
 * @return matched practitioner with internalSequenceIdentifier or the local match without internalSequenceIdentifier
 */
static NNGMOrganization matchOrganization(String displayName) {
    def jsonSlurper = new JsonSlurper()
    def jsonFile = new File('organizations.json')
    def reader = new FileReader(jsonFile)
    def json = jsonSlurper.parse(reader)

    println("Searching for " + displayName)

    def foundOrganization = json.find { it.displayName == displayName }
    def organization = new NNGMOrganization()
    organization.displayName = displayName

    if (foundOrganization) {
        println("Found organization by display name")
        organization.internalSequenceIdentifier = foundOrganization.internalSequenceIdentifier
        organization.networkPartnerIdentifier = foundOrganization.networkPartnerIdentifier
    }
    return organization
}


/**
 * Feel free to add more on your case. Maps the organization codes in CentraXX to the nNGM Partner list before trying to find
 * them in the NMS- Endpoint
 * @param organizationCode
 * @return
 */
static String mappingList(String organizationCode) {
    switch (organizationCode) {
        case "HÄMA":  return "Universitätsklinikum Frankfurt "
        case "KGU":  return "Universitätsklinikum Frankfurt "
    }
}
