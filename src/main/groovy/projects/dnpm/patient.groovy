package projects.dnpm
import de.kairos.fhir.centraxx.metamodel.PatientInsurances


/**
 * ############### PATIENT ###############
 *
 * Represented by a CXX PatientMasterDataAnonymous
 * Specified by https://ibmi-ut.atlassian.net/wiki/spaces/DAM/pages/2589011/Datenmodell+V2.0#DatenmodellV2.0-Patient
 *
 * @author Jaqueline Patzek
 * @since CXX.v.3.17.0.2
 */
patient {

  // id
  id = "Patient/" + context.source["patientcontainer.id"]

  // gender
  if (context.source["genderType"]) {
    gender = mapGender(context.source["genderType"])
  }

  // birthdate & deceasedDateTime
  birthDate = normalizeDate(context.source["birthdate.date"] as String)
  deceasedDateTime = "UNKNOWN" != context.source["dateOfDeath.precision"] ? normalizeDate(context.source["dateOfDeath.date"] as String) : null


  // identifier
    identifier {
      value = context.source["patientcontainer.id"]
    }

  // meta
  meta {
    profile "http://bwhc.de/mtb/patient"
  }

  // Krankenkasse - Institutskennzeichen
  // TODO auf Institutskennzeichen mappen!
  final def insurance = context.source[patient]

  if (insurance) {
    contact {
      organization {
        type = "Organization"
      }
      identifier {
        value = insurance[PatientInsurances.INSURANCE_COMPANY];
      }
      relationship {
        coding {
          code = "I"
          system = "http://hl7.org/fhir/ValueSet/patient-contactrelationship"
        }
      }
    }
  }



}

static def mapGender(final Object cxx) {
  switch (cxx) {
    case 'MALE':
      return "male"
    case 'FEMALE':
      return "female"
    case 'UNKNOWN':
      return "unknown"
    default:
      return "other"
  }
}

// Auf Institutskennzeichen mappen
static def mapInsurance(final Object cxx) {
  switch (cxx) {
    case 'MALE':
      return "male"
    case 'FEMALE':
      return "female"
    case 'UNKNOWN':
      return "unknown"
    default:
      return "other"
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null // removes the time
}
