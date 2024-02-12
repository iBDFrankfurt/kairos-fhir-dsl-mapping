package projects.nngm.v2

import groovy.json.JsonSlurper

import static de.kairos.fhir.centraxx.metamodel.RootEntities.attendingDoctor

/**
 * Represents a CXX AttendingDoctor
 * Specified by https://simplifier.net/guide/nNGM-Form/Home/FHIR-Profile/Basisangaben/BehanderPractioner.guide.md?version=current
 *
 * Hints:
 * Get Practitioners via Curl from https://simplifier.net/guide/nNGM-Form/Home/NMS/NMS---Endpunkt-f%C3%BCr-Personen.page.md?version=current
 *
 * @author Timo Schneider
 * @since v.1.15.0, CXX.v.2022.1.0
 */
practitioner {

    id = "Practitioner/" + context.source[attendingDoctor().id()]

    meta {
        profile "http://uk-koeln.de/fhir/StructureDefinition/Practitioner/nNGM"
    }
    final def practitionerInSystem = matchPractitioner(
            context.source[attendingDoctor().contact().contactPersonFirstName()].toString(),
            context.source[attendingDoctor().contact().contactPersonLastName()].toString(),
            context.source[attendingDoctor().contact().contactPersonTitle()].toString(),
    )

    if (practitionerInSystem.internalSequenceIdentifier) {
        identifier {
            system = "http://uk-koeln.de/fhir/sid/nNGM/nms-person"
            value = practitionerInSystem.internalSequenceIdentifier
        }
    } else {
        identifier {
            system = "urn:centraxx"
            value = normalizeName(practitionerInSystem.title, practitionerInSystem.firstName, practitionerInSystem.lastName)
        }
    }

}

class NNGMPractitioner {
    String internalSequenceIdentifier
    String title
    String firstName
    String lastName
    List<String> organizationAssignments = []
}

/**
 * Trying to find a better approach than mapping the centraxx practitioner to the given NNGM file
 * @param firstName
 * @param lastName
 * @return matched practitioner with internalSequenceIdentifier or the local match without internalSequenceIdentifier
 */
static NNGMPractitioner matchPractitioner(String firstName, String lastName, String title) {
    def jsonSlurper = new JsonSlurper()
    def jsonFile = new File('practitioners.json')
    def reader = new FileReader(jsonFile)
    def json = jsonSlurper.parse(reader)

    println("Searching for " + firstName + " " + lastName)

    def foundPractitioner = json.find { it.firstName == firstName && it.lastName == lastName }
    def practitioner = new NNGMPractitioner()
    practitioner.title = title
    practitioner.firstName = firstName
    practitioner.lastName = lastName
    if (foundPractitioner) {
        println("Found practitioner by first and last name")
        practitioner.title = foundPractitioner.title
        practitioner.firstName = foundPractitioner.firstName
        practitioner.lastName = foundPractitioner.lastName
        practitioner.internalSequenceIdentifier = foundPractitioner.internalSequenceIdentifier
        practitioner.organizationAssignments = foundPractitioner.organizationAssignments
    }
    return practitioner
}

static String normalizeName(String title, String firstName, String lastName) {
    if (title) {
        return title + " " + firstName + " " + lastName;
    } else {
        return firstName + " " + lastName
    }
}
