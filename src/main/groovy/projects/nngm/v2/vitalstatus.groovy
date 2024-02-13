package projects.nngm.v2


import org.hl7.fhir.r4.model.Observation

import java.time.LocalDate
import java.time.Period

import static de.kairos.fhir.centraxx.metamodel.PrecisionDate.DATE
import static de.kairos.fhir.centraxx.metamodel.PrecisionDate.PRECISION
import static de.kairos.fhir.centraxx.metamodel.RootEntities.patientMasterDataAnonymous

/**
 * Represented by a CXX PatientMasterDataAnonymous
 * Specified by https://simplifier.net/guide/nNGM-Form/Home/FHIR-Profile/Anforderung/VitalstatusObservation.guide.md?version=current
 *
 * Hints:
 * A vitalstatus has no separate encounter, but belongs to all encounter of the patient/subject
 *
 * @author Timo Schneider
 * @since CXX.v.3.17.1.6, v.3.17.2
 */
observation {
  id = "Observation/Vitalstatus-" + context.source[patientMasterDataAnonymous().patientContainer().id()]

  meta {
    profile "http://uk-koeln.de/fhir/StructureDefinition/nNGM/Vitalstatus"
  }

  status = Observation.ObservationStatus.UNKNOWN

  category {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/observation-category"
      code = "survey"
    }
  }

  code {
    coding {
      system = "http://loinc.org"
      code = "75186-7"
    }
  }

  subject {
    reference = "Patient/" + context.source[patientMasterDataAnonymous().patientContainer().id()]
  }

  // Should use the date of "last contact" which is not saved anywhere sadly
  // The death date in nNGM belongs to the Patient profile -> Patient.deceased[x]
  effectiveDateTime {
    date = normalizeDate(context.source[patientMasterDataAnonymous().creationDate()] as String)
  }

  String[] vitalState = mapVitalStatus(context.source[patientMasterDataAnonymous().birthdate()], context.source[patientMasterDataAnonymous().dateOfDeath()])
  valueCodeableConcept {
    coding {
      system = "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/VitalstatusCS"
      code = vitalState[0]
      display = vitalState[1]
    }
  }

}

/**
 * removes milli seconds and time zone.
 * @param dateTimeString the date time string
 * @return the result might be something like "1989-01-15T00:00:00"
 */
static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}

/**
 * A date of death with UNKNOWN precision is interpreted as, we are sure the person has died, but we dont know when more exactly.
 * An age, which is older than the oldest known person is interpreted as, the person has been died, but the date of death has not been documented.
 * return lebend, verstorben or unbekannt
 */
static String[] mapVitalStatus(final Object dateOfBirth, final Object dateOfDeath) {
  if (dateOfDeath != null) {
    return ["T","verstorben"]
  }

  if (dateOfBirth == null) {
    return ["A","lost to follow-up"]
  }

  final String dateString = dateOfBirth[DATE]
  final String precisionString = dateOfBirth[PRECISION]
  if (dateString == null || precisionString == "UNKNOWN") {
    return ["A","lost to follow-up"]
  }

  final LocalDate date = LocalDate.parse(dateString.substring(0, 10))
  return isOlderThanTheOldestVerifiedPerson(date) ? ["T","verstorben"] : ["L","lebt"]
}

/**
 * source: https://en.wikipedia.org/wiki/List_of_the_verified_oldest_people
 */
static boolean isOlderThanTheOldestVerifiedPerson(final LocalDate dateOfBirth) {
  final Period age = Period.between(dateOfBirth, LocalDate.now())
  return age.getYears() > 123
}
