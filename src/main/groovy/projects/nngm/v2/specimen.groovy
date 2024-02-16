package projects.nngm.v2

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.AbstractIdContainer
import de.kairos.fhir.centraxx.metamodel.IdContainerType

import static de.kairos.fhir.centraxx.metamodel.AbstractSample.PARENT
import static de.kairos.fhir.centraxx.metamodel.RootEntities.abstractSample
import static de.kairos.fhir.centraxx.metamodel.RootEntities.sample

/**
 * Represented by a CXX AbstractSample
 *
 * Specified by https://simplifier.net/guide/nNGM-Form/Home/FHIR-Profile/Anforderung/TumormaterialSpecimen.guide.md?version=current
 *
 * @author Timo Schneider
 * @since CXX.v.3.17.1.6, v.3.17.2
 *
 * Hints:
 *  * CCP-IT 2023-07-13: Always export aliquots with parent references
 *
 */
specimen {

    final String sampleTypeCode = context.source[abstractSample().sampleType().code()] as String
    if (matchIgnoreCase(["TBL", "LES", "UBK", "ZZZ", "NRT"], sampleTypeCode)) {
        return //"Leerschnitt", "Unbekannt" are filtered
    }

    id = "Specimen/" + context.source[abstractSample().id()]

    context.source[sample().idContainer()].each { final def idObj ->
        if (idObj) {
            identifier {
                type {
                    coding {
                        system = "urn:centraxx"
                        code = idObj[AbstractIdContainer.PSN]
                        display = idObj[AbstractIdContainer.ID_CONTAINER_TYPE][IdContainerType.CODE] as String
                    }
                }
                value = idObj[AbstractIdContainer.PSN]
            }
        }
    }

    // Biopsie
    if (context.source[abstractSample().histoNumber()]) {
        identifier {
            value = context.source[abstractSample().histoNumber()]
            type {
                coding {
                    system = "http://terminology.hl7.org/CodeSystem/v2-0203"
                    code = context.source[abstractSample().histoNumber()]
                }
            }
        }
    }

    meta {
        profile "http://uk-koeln.de/fhir/StructureDefinition/nNGM/Specimen"
    }

    status = context.source[abstractSample().restAmount().amount()] > 0 ? "available" : "unavailable"

    if (context.source[PARENT] != null) {
        parent {
            reference = "Specimen/" + context.source[sample().parent().id()]
        }
    }

    type {
        // 0. First coding is the CXX sample type code. If mapping is missing, this code might help to identify the source value.
        coding {
            system = "urn:centraxx"
            code = context.source[abstractSample().sampleType().code()]
        }

        final String sampleKind = context.source[abstractSample().sampleType().kind()] as String
        final String stockType = context.source[abstractSample().stockType().code()] as String

        final String ncitCode = codeToSampleType(sampleTypeCode, stockType, sampleKind)
        println(sampleTypeCode)
        println(ncitCode)
        if (ncitCode != null) {
            coding {
                system = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"
                code = ncitCode
            }
        }

    }

    subject {
        reference = "Patient/" + context.source[abstractSample().patientContainer().id()]
    }

    receivedTime {
        date = normalizeDate(context.source[abstractSample().samplingDate().date()] as String)
        precision = TemporalPrecisionEnum.DAY.name()
    }

    final def ucum = context.conceptMaps.builtin("centraxx_ucum")
    collection {
        collectedDateTime {
            date = normalizeDate(context.source[abstractSample().samplingDate().date()] as String)
            quantity {
                value = context.source[abstractSample().initialAmount().amount()] as Number
                unit = ucum.translate(context.source[abstractSample().initialAmount().unit()] as String)?.code
                system = "http://unitsofmeasure.org"
            }
        }
    }

    container {
        if (context.source[abstractSample().receptable()]) {
            identifier {
                value = context.source[abstractSample().receptable().code()]
                system = "urn:centraxx"
            }
        }

        specimenQuantity {
            value = context.source[abstractSample().restAmount().amount()] as Number
            unit = ucum.translate(context.source[abstractSample().restAmount().unit()] as String)?.code
            system = "http://unitsofmeasure.org"
        }
    }


    final def temperature = toTemperature(context)
    if (temperature) {
        extension {
            url = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/specimen-tumormaterial-lagert-bei"
            valueCodeableConcept {
                coding {
                    system = "http://uk-koeln.de/fhir/StructureDefinition/Extension/nNGM/specimen-tumormaterial-lagert-bei"
                    code = temperature
                }
            }
        }
    }

}

static def toTemperature(final ctx) {
    final def temp = ctx.source[abstractSample().sampleLocation().temperature() as String]
    if (null != temp) {
        switch (temp) {
            case { it >= 2.0 && it <= 10 }: return "temperature2to10"
            case { it <= -18.0 && it >= -35.0 }: return "temperature-18to-35"
            case { it <= -60.0 && it >= -85.0 }: return "temperature-60to-85"
        }
    }

    final def sprec = ctx.source[abstractSample().receptable().sprecCode() as String]
    if (null != sprec) {
        switch (sprec) {
            case ['C', 'F', 'O', 'Q']: return "temperatureLN"
            case ['A', 'D', 'J', 'L', 'N', 'O', 'S']: return "temperature-60to-85"
            case ['B', 'H', 'K', 'M', 'T']: return "temperature-18to-35"
            default: return "temperatureOther"
        }
    }

    return null
}

static String codeToSampleType(final String sampleTypeCode, final String stockType, final String sampleKindCode) {
    if (null == sampleTypeCode) {
        return null
    }

    switch (sampleTypeCode) {
        case { matchIgnoreCase(["whole-blood", "BLD", "VBL", "Vollblut", "TBL"], sampleTypeCode) }: return "C12434"
        case { matchIgnoreCase(["KNM", "bone-marrow", "Knochenmark", "BMA", "EDTAKM"], sampleTypeCode) }: return "C12801"
        case { matchIgnoreCase(["BUFFYCOAT", "BuffyCoat", "BUF", "buffy-coat", "BUFFYCOATNOTVIABLE"], sampleTypeCode) }: return "C12801"
        case { matchIgnoreCase(["TBK", "BF"], sampleTypeCode) }: return "C12434"
        case { matchIgnoreCase(["PBMC", "PBMC-nl", "PBMCs", "PBMC-l", "PEL"], sampleTypeCode) }: return "C12434"
        case {
            matchIgnoreCase(["PLA", "blood-plasma", "PL1", "Plasma", "P", "HEPAP", "P-cf", "EDTAFCPMA", "EDTA-APRPMA", "EP, CP", "EPPL2", "EPPL1",
                             "plasma-edta", "plasma-citrat", "plasma-heparin", "plasma-cell-free", "plasma-other", "HEPAPPBS"], sampleTypeCode)
        }: return "C12434"
        case { matchIgnoreCase(["SER", "blood-serum", "Serum"], sampleTypeCode) }: return "C12434"
        case {
            matchIgnoreCase(["Paraffin (FFPE)", "Paraffin", "FFPE", "NBF"], stockType) &&
                    (matchIgnoreCase(["NRT", "NGW", "TIS", "TGW", "STUGEW", "NRT", "Tumorgewebe", "Normalgewebe", "RDT", "NNB", "PTM", "RZT", "LMT", "MMT", "GEW", "TM", "BTM", "SMT", "TFL", "NBF", "tumor-tissue-ffpe", "normal-tissue-ffpe", "other-tissue-ffpe"], sampleTypeCode)
                            ||
                            matchIgnoreCase(["tissue", "gewebe", "gewebeprobe", "tissue sample"], sampleKindCode))
        }: return "C12801"
        case {
            matchIgnoreCase(["Kryo/Frisch (FF)", "Kryo/Frisch", "FF", "SNP"], stockType) &&
                    (matchIgnoreCase(["NGW", "TIS", "TGW", "STUGEW", "NRT", "Tumorgewebe", "Normalgewebe", "RDT", "NNB", "PTM", "RZT", "LMT", "MMT", "GEW", "TM", "BTM", "SMT", "TFL", "SNP", "tumor-tissue-frozen", "normal-tissue-frozen", "other-tissue-frozen"], sampleTypeCode)
                            || matchIgnoreCase(["tissue", "gewebe", "gewebeprobe", "tissue sample"], sampleKindCode))
        }: return "C12801"
        case { matchIgnoreCase(["tissue-other", "NNB", "HE"], sampleTypeCode) }: return "C12801"
        default: return null // no match
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


static boolean matchIgnoreCase(final List<String> stringList, final String stringToMatch) {
    return stringList.stream().anyMatch({ it.equalsIgnoreCase(stringToMatch) })
}
