package projects.nngm.v2

import de.kairos.fhir.centraxx.metamodel.*
import de.kairos.fhir.centraxx.metamodel.enums.LaborValueDType
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborFindingLaborValue

/**
 * Represented by ECOG https://simplifier.net/guide/nNGM-Form/Home/FHIR-Profile/Anforderung/ECOGObservation-duplicate-2.guide.md?version=current
 *
 * Still trying to figure out which values i need to export for ECOG, Raucherstatus, Progress, Rezidiv
 *
 * @author Timo Schneider
 * @since v.1.7.0. CXX.v.3.17.2
 */
observation {
    id = "Observation/" + context.source[laborFindingLaborValue().id()]

    meta {
        source = "urn:centraxx"
        profile("https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab")
    }

    identifier {
        type {
            coding {
                system = "http://terminology.hl7.org/CodeSystem/v2-0203"
                code = "OBI"
            }
        }
        system = "urn:centraxx"
        value = context.source[laborFindingLaborValue().labFindingLabValId()]
    }

    status = Observation.ObservationStatus.UNKNOWN

    // There is no categorization for LaborFindingLaborValues in Centraxx. Therefore, laboratory is assigned here.
    category {
        coding {
            system = "http://loinc.org"
            code = "26436-6"
        }
        coding {
            system = "http://terminology.hl7.org/CodeSystem/observation-category"
            code = "laboratory"
        }
    }

    code {
        coding {
            system = "urn:centraxx"
            code = context.source[laborFindingLaborValue().laborValue().code()] as String
            display = context.source[laborFindingLaborValue().laborValue().nameMultilingualEntries()].find { final def entry ->
                "de" == entry[MultilingualEntry.LANG]
            }?.getAt(MultilingualEntry.VALUE)
        }
    }

    final def laborMappingWithRelatedPatient = context.source[laborFindingLaborValue().laborFinding().laborMappings()].find {
        it[LaborMapping.RELATED_PATIENT] != null
    }
    if (laborMappingWithRelatedPatient) {
        subject {
            reference = "Patient/" + laborMappingWithRelatedPatient[LaborMapping.RELATED_PATIENT][PatientContainer.ID]
        }
    }


    final def laborMappingWithEpisode = context.source[laborFindingLaborValue().laborFinding().laborMappings()].find {
        it[LaborMapping.EPISODE] != null
    }
    if (laborMappingWithEpisode) {
        encounter {
            reference = "Encounter/" + laborMappingWithEpisode[LaborMapping.EPISODE][Episode.ID]
        }
    }

    // the CXX creation date documents the when the LaborFindingLaborValue got assigned a value. Therefore, the clinical reference date
    // depends on the process of documentation.
    effectiveDateTime {
        date = normalizeDate(context.source[laborFindingLaborValue().creationDate()] as String)
        extension {
            url = "https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/QuelleKlinischesBezugsdatum"
            valueCoding {
                system = "http://snomed.info/sct"
                code = "281271004"
                display = "Date sample received in laboratory (observable entity)"
            }
        }
    }


    final def dType = context.source[laborFindingLaborValue().laborValue().dType()] as LaborValueDType
    if (isNumeric(dType)) {
        final def unit_ = context.source[laborFindingLaborValue().laborValue()]?.getAt(LaborValueNumeric.UNIT)?.getAt(Unity.CODE)
        valueQuantity {
            value = context.source[laborFindingLaborValue().numericValue()]
            unit = unit_ ? unit_ : null
            system = "http://unitsofmeasure.org"
        }

        final def refRangeLow = context.source[laborFindingLaborValue().laborValueDecimal().lowerValue()] as String
        final def refRangeHigh = context.source[laborFindingLaborValue().laborValueDecimal().upperValue()] as String

        referenceRange {
            if (refRangeLow) {
                low {
                    value = refRangeLow
                    unit = unit_ ? unit_ : null
                    system = "http://unitsofmeasure.org"
                }
            }
            if (refRangeHigh) {
                high {
                    value = refRangeHigh
                    unit = unit_ ? unit_ : null
                    system = "http://unitsofmeasure.org"
                }
            }
        }
    }

    if (isBoolean(dType)) {
        component {
            valueBoolean(context.source[laborFindingLaborValue().booleanValue()] as Boolean)
        }
    }

    if (isDate(dType)) {
        component {
            valueDateTime {
                date = laborFindingLaborValue().dateValue()?.getAt(PrecisionDate.DATE)
            }
        }
    }

    if (isTime(dType)) {
        component {
            valueTime(context.source[laborFindingLaborValue().timeValue()] as String)
        }
    }

    if (isString(dType)) {
        component {
            valueString(context.source[laborFindingLaborValue().stringValue()] as String)
        }
    }

    if (isCatalog(dType)) {
        component {
            valueCodeableConcept {
                context.source[laborFindingLaborValue().catalogEntryValue()].each { final entry ->
                    coding {
                        system = "urn:centraxx:CodeSystem/ValueList-" + entry[CatalogEntry.CATALOG]?.getAt(AbstractCatalog.ID)
                        code = entry[CatalogEntry.CODE] as String
                    }
                }
                context.source[laborFindingLaborValue().icdEntryValue()].each { final entry ->
                    coding {
                        system = "urn:centraxx:CodeSystem/IcdCatalog-" + entry[IcdEntry.CATALOGUE]?.getAt(AbstractCatalog.ID)
                        code = entry[IcdEntry.CODE] as String
                    }
                }
                context.source[laborFindingLaborValue().opsEntryValue()].each { final entry ->
                    coding {
                        system = "urn:centraxx:CodeSystem/OpsCatalog-" + entry[OpsEntry.CATALOGUE]?.getAt(AbstractCatalog.ID)
                        code = entry[OpsEntry.CODE] as String
                    }
                }
            }
        }
    }

    // CentraXX does not have explicit methods. This refers to the corresponding measurement profile.
    method {
        coding {
            system = "urn:centraxx"
            version = context.source[laborFindingLaborValue().laborFinding().laborMethod().version()]
            code = context.source[laborFindingLaborValue().laborFinding().laborMethod().code()] as String
        }
    }
}

static boolean isBoolean(final Object dType) {
    return [LaborValueDType.BOOLEAN].contains(dType)
}

static boolean isNumeric(final Object dType) {
    return [LaborValueDType.INTEGER, LaborValueDType.DECIMAL, LaborValueDType.SLIDER].contains(dType)
}


static boolean isDate(final Object dType) {
    return [LaborValueDType.DATE, LaborValueDType.LONGDATE].contains(dType)
}

static boolean isTime(final Object dType) {
    return [LaborValueDType.TIME].contains(dType)
}

static boolean isEnumeration(final Object dType) {
    return [LaborValueDType.ENUMERATION].contains(dType)
}

static boolean isString(final Object dType) {
    return [LaborValueDType.STRING, LaborValueDType.LONGSTRING].contains(dType)
}

static boolean isCatalog(final Object dType) {
    return [LaborValueDType.CATALOG].contains(dType)
}

static boolean isOptionGroup(final Object dType) {
    return [LaborValueDType.OPTIONGROUP].contains(dType)
}

static String normalizeDate(final String dateTimeString) {
    return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}
