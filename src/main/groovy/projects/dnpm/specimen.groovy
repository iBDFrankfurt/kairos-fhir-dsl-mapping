package projects.dnpm

import de.kairos.fhir.centraxx.metamodel.IdContainer
import de.kairos.fhir.centraxx.metamodel.IdContainerType

import static de.kairos.fhir.centraxx.metamodel.RootEntities.abstractSample
import static de.kairos.fhir.centraxx.metamodel.RootEntities.sample

/**
 *  ############### TUMORPROBE ###############
 *
 * Represented by a CXX AbstractSample
 * Specified by https://simplifier.net/bbmri.de/specimen
 *
 * @author Jaqueline Patzek
 * @since CXX.v.3.17.1.6, v.3.17.2
 */
specimen {

  if (!"MASTER".equals(context.source["sampleCategory"])) {
    return  // all not master are filtered.
  }

  //id = "Specimen/" + context.source[abstractSample().id()]

  // identifier
  identifier {
    value = context.source[abstractSample().id()]
  }

  // patient / subject
  subject {
    identifier {
      value = context.source[abstractSample().patientContainer().id()]
    }
    type = "Patient"
  }

  // meta
  meta {
    profile "http://bwhc.de/mtb/tumor-specimen"
  }

  // modifierExtension / Tumor-Entitaet
  modifierExtension {
    url = "https://fhir.bbmri.de/StructureDefinition/SampleDiagnosis"
    valueCodeableConcept {
      coding {
        code = ""
        display = ""
        version = ""
        system = "http://fhir.de/CodeSystem/dimdi/icd-10-gm"
      }
    }
  }

  // Tumor TODO: Pruefen, ob andere Felder gewuenscht sind
  type {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/v2-0487"
      code = "TUMOR"
      display = "Tumor"
    }
  }

  // Probenart
  condition {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/v2-0487"
      code = context.source[abstractSample().sampleType().code()] // TODO {fresh-tissue, cryo-frozen, liquid-biopsy, FFPE, unknown}
      display = context.source[abstractSample().sampleType().code()] // TODO mapping
    }
  }

  // Entnahme (der Tumorprobe)
  collection {
    collectedDateTime = "TODO"
    bodySite {
      coding {
        system = "TODO"
        code = "TODO"
      }
    }
    method {
      coding {
        system = "TODO"
        code = "TODO"
      }
    }
  }


}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null // removes the time
}