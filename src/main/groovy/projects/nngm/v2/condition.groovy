package projects.nngm.v2

import de.kairos.fhir.centraxx.metamodel.Diagnosis

import static de.kairos.fhir.centraxx.metamodel.RootEntities.diagnosis

/**
 * Represented by a CXX Diagnosis
 * Tries to find the first Diagnosis of a Patient by first iterating over all Diagnoses.
 * Returns the first diagnosis by date.
 * After processing every diagnosis of one patient, try to match the current processed diagnosis with the first diagnosis
 *
 * So the bad thing is, for every patient this script will have to iterate over all diagnoses first and then iterates over each diagnosis to match the first diagnosis.
 * If there is something better, please let me know.
 *
 * Specified by https://simplifier.net/oncology/primaerdiagnose
 * @author Timo Schneider
 * @since CXX.v.2023.3.2
 */
condition {
    // Get a List of all Diagnoses
    final List<Diagnosis> allDiagnoses = context.source[diagnosis()].findAll()
    if(allDiagnoses == null || allDiagnoses.isEmpty()) {
        return
    }
    final Diagnosis firstDiagnosis = getFirstDiagnosis(allDiagnoses)

    // ID of current processed diagnosis
    final String diagId = context.source[diagnosis().id()] as String

    //ID of firstDiagnosis
    final String firstDiagId = firstDiagnosis.id() as String

    final String icdCode = context.source[firstDiagnosis.icdEntry().code()]

    // only keep the first diagnosis and skip every other diagnosis
    if (firstDiagId != diagId) {
        return
    }

    id = "Condition/" + firstDiagId

    meta {
        source = "urn:centraxx"
        profile "http://uk-koeln.de/fhir/StructureDefinition/Condition/nNGM/FirstDiagnosis"
    }

    subject {
        reference = "Patient/" + context.source[firstDiagnosis.patientContainer().id()]
    }

    final def diagnosisId = context.source[firstDiagnosis.diagnosisId()]
    if (diagnosisId) {
        identifier {
            value = diagnosisId
            type {
                coding {
                    system = "urn:centraxx"
                    code = "diagnosisId"
                }
            }
        }
    }

    final def clinician = context.source[firstDiagnosis.clinician()]
    if (clinician) {
        recorder {
            identifier {
                display = clinician
            }
        }
    }

    onsetDateTime {
        date = normalizeDate(context.source[firstDiagnosis.diagnosisDate().date()] as String)
    }


    code {
        coding {
            system = "http://fhir.de/CodeSystem/bfarm/icd-10-gm"
            code = icdCode as String
            version = context.source[firstDiagnosis.icdEntry().catalogue().catalogueVersion()]
        }
        text = context.source[firstDiagnosis.icdEntry().preferredLong()] as String
    }

    // ICD-O-3 topography
    final String catalogName = context.source[firstDiagnosis.icdEntry().catalogue().name()]
    if (catalogName != null && catalogName.contains("ICD-O-3")) {
        bodySite {
            coding {
                system = "urn:oid:2.16.840.1.113883.6.43.1"
                code = icdCode as String
                version = context.source[firstDiagnosis.icdEntry().catalogue().catalogueVersion()]
            }
            text = context.source[firstDiagnosis.icdEntry().preferredLong()] as String
        }
    }

    if (context.source[firstDiagnosis.diagnosisLocalisation()] != null) {
        bodySite {
            coding {
                system = "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/SeitenlokalisationCS"
                code = context.source[firstDiagnosis.diagnosisLocalisation()] as String
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

/**
 * Return the diagnosis with the oldest date, this will return the first diagnosis
 * @param allDiagnosis List of all Diagnosis
 * @return firstDiagnosis
 */
static Diagnosis getFirstDiagnosis(final List<Diagnosis> allDiagnosis) {
    Diagnosis firstDiagnosis = null

    allDiagnosis.each { diag ->
        if (firstDiagnosis == null || diag.diagnosisDate().date() < firstDiagnosis.diagnosisDate().date()) {
            oldestObject = diag
        }
    }

    return firstDiagnosis
}
