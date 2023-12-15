package projects.nngm.v2

import de.kairos.fhir.centraxx.metamodel.Country
import de.kairos.fhir.centraxx.metamodel.MultilingualEntry
import de.kairos.fhir.centraxx.metamodel.PatientAddress
import de.kairos.fhir.centraxx.metamodel.enums.GenderType
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender

import static de.kairos.fhir.centraxx.metamodel.PatientMaster.GENDER_TYPE
import static de.kairos.fhir.centraxx.metamodel.RootEntities.patient
import static de.kairos.fhir.centraxx.metamodel.RootEntities.patientMasterDataAnonymous

/**
 * Represented by a CXX PatientMasterDataAnonymous
 * @author Timo Schneider
 * @since CXX.v.2023.3.2
 */
patient {

    id = "Patient/" + context.source[patientMasterDataAnonymous().patientContainer().id()]

    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/Patient/nNGM/patient"
    }

    identifier {
        system = "urn:centraxx"
        value = context.source[patientMasterDataAnonymous().patientContainer().id()]
    }

    humanName {
        use = "official"
        family = context.source[patient().lastName()]
        given context.source[patient().firstName()] as String
        prefix context.source[patient().title().descMultilingualEntries()]?.find { final def me ->
            me[MultilingualEntry.LANG] == "de"
        }?.getAt(MultilingualEntry.VALUE) as String
    }

    if (context.source[patient().birthName()]) {
        humanName {
            use = "maiden"
            family = context.source[patient().birthName()]
            given context.source[patient().firstName()] as String
        }
    }

    birthDate = normalizeDate(context.source[patientMasterDataAnonymous().birthdate().date()] as String)

    deceasedDateTime = "UNKNOWN" != context.source[patientMasterDataAnonymous().dateOfDeath().precision()] ?
            context.source[patientMasterDataAnonymous().dateOfDeath().date()] : null

    if (context.source[GENDER_TYPE]) {
        gender = mapGender(context.source[GENDER_TYPE] as GenderType)
    }

    context.source[patient().addresses()]?.each { final ad ->
        address {
            type = "physical"
            city = ad[PatientAddress.CITY]
            postalCode = ad[PatientAddress.ZIPCODE]
            country = ad[PatientAddress.COUNTRY]?.getAt(Country.ISO2_CODE)
            final def lineString = getLineString(ad as Map)
            if (lineString) {
                line lineString
            }
        }
    }
}

static AdministrativeGender mapGender(final GenderType genderType) {
    switch (genderType) {
        case GenderType.MALE:
            return AdministrativeGender.MALE
        case GenderType.FEMALE:
            return AdministrativeGender.FEMALE
        case GenderType.UNKNOWN:
            return AdministrativeGender.UNKNOWN
        default:
            return AdministrativeGender.OTHER
    }
}

static String normalizeDate(final String dateTimeString) {
    return dateTimeString != null ? dateTimeString.substring(0, 10) : null // removes the time
}

static String getLineString(final Map address) {
    final def keys = [PatientAddress.STREET, PatientAddress.STREETNO]
    final def addressParts = keys.collect { return address[it] }.findAll()
    return addressParts.findAll() ? addressParts.join(" ") : null
}
