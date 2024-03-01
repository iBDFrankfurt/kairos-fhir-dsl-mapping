package projects.dnpm

import de.kairos.fhir.centraxx.metamodel.LaborFindingLaborValue
import de.kairos.fhir.centraxx.metamodel.LaborValue
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborMapping

/**
 *  ############### ECOG Performance Status Befund ###############
 *
 * Represented by a CXX ???
 * Specified by ???
 *
 * @author Jaqueline Patzek
 * @since v.1.7.0. CXX.v.3.17.2
 */
observation {

  // TODO find ECOG
  if ("BodyHeight" != context.source[laborMapping().laborFinding().laborMethod().code()]) {
    return // no export
  }
  // top-lvl stats
  status = Observation.ObservationStatus.FINAL // TODO
  effectiveDateTime = "" // TODO


  // identifier
  identifier {
    value = context.source["patientcontainer.id"] // TODO
  }

  // subject / patient
  subject {
    identifier {

    }
    type = "Patient"
  }

  // ECOG-Performance-Status
  valueCodeableConcept { // TODO whole block
    coding = {
      code = "1"
      display = "ECOG 1"
      system = "ECOG-Performance-Status"
    }
  }

  // code / coding / loinc
  code { // TODO whole block
    coding {
      code = "89247-1"
      system = "http://loinc.org"
    }
  }

  meta {
    profile("http://bwhc.de/obs-ecog-performance-status")
  }
}
