import static de.kairos.fhir.centraxx.metamodel.AbstractEntity.ID
import static de.kairos.fhir.centraxx.metamodel.RootEntities.diagnosis

/**
 * Represented by a CXX Diagnosis
 * Specified by https://simplifier.net/guide/nNGM-Form/Home/FHIR-Profile/Basisangaben/ErstdiagnoseCondition.guide.md?version=current
 * @author Timo Schneider
 * @since CXX.v.3.17.1.6, v.3.17.2
 */
condition {

    final String icdCode = context.source[diagnosis().icdEntry().code()]
    if (!hasRelevantCode(icdCode)) { // diagnosis without C or D code are filtered
        return
    }

    id = "Condition/" + context.source[diagnosis().id()]

    meta {
        profile "http://uk-koeln.de/fhir/StructureDefinition/Condition/nNGM/FirstDiagnosis"
    }

    subject {
        reference = "Patient/" + context.source[diagnosis().patientContainer().id()]
    }

    final def diagnosisId = context.source[diagnosis().diagnosisId()]
    if (diagnosisId) {
        identifier {
            value = diagnosisId
            type {
                coding {
                    system = "urn:centraxx"
                    code = diagnosisId as String
                }
            }
        }
    }

    final def clinician = context.source[diagnosis().clinician()]
    if (clinician) {
        recorder {
            identifier {
                display = clinician
            }
        }
    }

    onsetDateTime {
        date = normalizeDate(context.source[diagnosis().diagnosisDate().date()] as String)
    }

    // Histologie
    if (context.source[diagnosis().icdOentry()]) {
        final def icdOEntry = context.source[diagnosis().icdOentry().preferredLong()]
        code {
            coding {
                system = "urn:oid:2.16.840.1.113883.6.43.1"
                code = icdOEntry as String
                version = context.source[diagnosis().icdOentry().catalogue().catalogueVersion()]
            }
            text = icdOEntry as String
        }
    }

    // topography
    if(context.source[diagnosis().icdEntry()]) {
        bodySite {
            coding {
                system = "http://terminology.hl7.org/CodeSystem/icd-o-3"
                code = icdCode as String
                version = context.source[diagnosis().icdEntry().catalogue().catalogueVersion()]
            }
            text = context.source[diagnosis().icdEntry().preferredLong()] as String
        }
    }

    if (context.source[diagnosis().diagnosisLocalisation()] != null) {
        bodySite {
            coding {
                system = "http://dktk.dkfz.de/fhir/onco/core/CodeSystem/SeitenlokalisationCS"
                code = context.source[diagnosis().diagnosisLocalisation()] as String
            }
        }
    }

    context.source[diagnosis().samples()]?.each { final sample ->
        extension {
            url = "http://dktk.dkfz.de/fhir/StructureDefinition/onco-core-Extension-Specimen"
            valueReference {
                reference = "Specimen/" + sample[ID]
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
    return icdCode != null && (icdCode.toUpperCase().startsWith('C34'))
}
