package projects.dnpm

import static de.kairos.fhir.centraxx.metamodel.RootEntities.diagnosis
import static de.kairos.fhir.centraxx.metamodel.RootEntities.diagnosis
import static de.kairos.fhir.centraxx.metamodel.RootEntities.diagnosis

/**
 *  ############### DIAGNOSE ###############
 *
 * Represented by a CXX Diagnosis
 * Specified by https://simplifier.net/bbmri.de/condition
 *
 * @author Jaqueline Patzek
 * @since CXX.v.3.17.0.2
 */
condition {

  //  id = "Condition/" + context.source["id"]
  recordedDate = normalizeDate(context.source["diagnosisDate.date"] as String) // TODO pruefen auf Korrektheit

  // identifier
  final def diagnosisId = context.source["diagnosisId"]
  if (diagnosisId) {
    identifier {
      value = diagnosisId
    }
  }

  // patient / subject
  subject {
    reference = "Patient/" + context.source["patientcontainer.id"]
  }


  // meta
  meta {
    profile "http://bwhc.de/mtb/diagnosis"
  }


  // code / coding
  code {
    coding { // TODO pruefen auf Korrektheit
      system = "http://fhir.de/CodeSystem/dimdi/icd-10-gm"
      code = context.source["icdEntry.code"] as String
      version = context.source["icdEntry.kind"]
      display = context.source["icdEntry.name"]
    }
  }

  // bodysite / ICD-O-3-T (topography) // TODO pruefen auf Korrektheit
  final String catalogName = context.source[diagnosis().icdEntry().catalogue().name()]
  if (catalogName != null && catalogName.contains("ICD-O-3")) {
    bodySite {
      coding {
        system = "urn:oid:2.16.840.1.113883.6.43.1"
        code = icdCode as String
        version = context.source[diagnosis().icdEntry().catalogue().catalogueVersion()]
        display = context.source[diagnosis().icdEntry().preferredLong()]
      }
      text = context.source[diagnosis().icdEntry().preferredLong()] as String
    }
  }

  // stage / Tumorausbreitung // TODO
  stage {
    // TODO for-loop
    extension {
      url = "http://bwhc.de/mtb/diagnosis/tumor-stage-date"
      valueDate = "TODO" ; // Zeitpunkt
    }
    summary {
      coding {
        code = "" // TODO {tumor-free, local, metastasized, unknown}
        system = "MTB-Tumor-Status"
      }
    }
  }

  // Leitlinienbehandlung-Status
  extension {
    url = "http://bwhc.de/mtb/diagnosis/guideline-treatment-status"
    valueCoding {
      code = "unknown" // TODO { exhausted, non-exhausted, impossible, no-guidelines-available, unknown }
      system = "http://bwhc.de/mtb/diagnosis/guideline-treatment-status"
    }
  }

}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null // removes the time
}

