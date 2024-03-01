package projects.dnpm

import java.text.SimpleDateFormat

import static de.kairos.fhir.centraxx.metamodel.RootEntities.consent
/**
 * ############### EINWILLIGUNG ###############
 *
 * Represented by a CXX Consent
 * @author Jaqueline Patzek
 * @since KAIROS-FHIR-DSL.v.1.16.0, CXX.v.2022.2.0
 * HINT: binary file attachments are not supported yet.
 */

consent {

  // id
  id = "Consent/Consent-" + context.source[consent().id()]

  // status
  final def validFrom = context.source[consent().validFrom().date()]
  final def validUntil = context.source[consent().validUntil().date()]
  final String interpretedStatus = getStatus(validUntil as String)
  status = interpretedStatus

  // identifier
  identifier {
    value = context.source[consent().id()]
  }

  // patient
  patient {
    reference = "Patient/" + context.source[consent().patientContainer().id()]
  }

  // meta
  meta {
    profile =  	["http://bwhc.de/consent"]
  }

  // scope >> coding
  final boolean hasFlexiStudy = context.source[consent().consentType().flexiStudy()] != null
  scope {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/consentscope"
      code = hasFlexiStudy ? "research" : "patient-privacy" // TODO
    }
  }

  // category >> coding
  category {
    coding {
      system = "http://loinc.org"
      code = "59284-0" // Patient Consent
    }
  }





}

static String getStatus(final String validFromDate) {
  if (!validFromDate) {
    return "active"
  }

  final Date fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(validFromDate.substring(0, 10))
  final Date currDate = new Date()
  final int res = currDate <=> (fromDate)
  return res == 1 ? "rejected" : "active" // TODO
}
