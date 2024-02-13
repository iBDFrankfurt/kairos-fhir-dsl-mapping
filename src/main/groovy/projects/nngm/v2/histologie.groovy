package projects.nngm.v2

import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.histology

/**
 * Represented by a CXX Histology
 * Specified by https://simplifier.net/guide/nNGM-Form/Home/FHIR-Profile/Anforderung/HistologieObservation.guide.md?version=current
 *
 * Needs to be further tested for nNGM
 *
 * hints:
 *  Reference to a single specimen is not clearly determinable, because in CXX the reference might be histology 1->n diagnosis/tumor 1->n sample.
 *  Reference to hasMember is not available. There is no parent child hierarchy of histologies in CXX yet.
 *  Reference to focus condition has been added additionally, because a reverse reference is not possible yet.
 *
 * @author Timo Schneider
 * @since CXX.v.3.18.1.21, CXX.v.3.18.2
 */
observation {
  id = "Observation/Histology-" + context.source[histology().id()]

  meta {
    profile "http://uk-koeln.de/fhir/StructureDefinition/Observation/nNGM/histologie"
  }

  status = Observation.ObservationStatus.UNKNOWN

  category {
    coding {
      system = "http://hl7.org/fhir/observation-category"
      code = "laboratory"
    }
  }
  code {
    coding {
      system = "http://loinc.org"
      code = "59847-4"
    }
  }

  subject {
    reference = "Patient/" + context.source[histology().patientContainer().id()]
  }

  effectiveDateTime {
    date = normalizeDate(context.source[histology().date()] as String)
  }

  if (context.source[histology().icdEntry()]) {
    valueCodeableConcept {
      coding {
        system = "urn:oid:2.16.840.1.113883.6.43.1"
        version = "32"
        code = context.source[histology().icdEntry().code()] as String
      }
      text = context.source[histology().icdEntry().preferredLong()] as String
    }
  }

  if (context.source[histology().tumour()] && hasRelevantCode(context.source[histology().tumour().centraxxDiagnosis().diagnosisCode()] as String)) {
    focus {
      reference = "Condition/" + context.source[histology().tumour().centraxxDiagnosis().id()]
    }
  }

  component {
    code {
      coding {

      }
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

static boolean hasRelevantCode(final String icdCode) {
  return icdCode != null && (icdCode.toUpperCase().startsWith('C') || icdCode.toUpperCase().startsWith('D'))
}
