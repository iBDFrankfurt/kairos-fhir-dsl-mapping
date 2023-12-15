package projects.nngm.v2

import de.kairos.centraxx.fhir.r4.utils.FhirUrls
import de.kairos.fhir.centraxx.metamodel.ConsentPolicy
import de.kairos.fhir.centraxx.metamodel.ConsentableAction
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Consent

import java.text.SimpleDateFormat

import static de.kairos.fhir.centraxx.metamodel.RootEntities.consent
/**
 * Represented by a CXX Consent
 * @author Timo Schneider
 * @since CXX.v.2023.3.2
 * TODO untested, needs to find specific consents for nNGM https://simplifier.net/nngm-form/valueset-nngm-consentpolicy
 */

consent {

  id = "Consent/" + context.source[consent().id()]

  meta {
    source = "urn:centraxx"
    profile "http://uk-koeln.de/fhir/StructureDefinition/nNGM/ConsentV4"
  }

  patient {
    reference = "Patient/" + context.source[consent().patientContainer().id()]
  }

  final def validFrom = context.source[consent().validFrom().date()]
  final def validUntil = context.source[consent().validUntil().date()]

  provision {
    period {
      start = validFrom
      end = validUntil
    }

    purpose {
      system = FhirUrls.System.Consent.Type.BASE_URL
      code = context.source[consent().consentType().code()] as String
    }

    // add consent parts
    final def consentElements = context.source[consent().consentType().policies()]
    for (final policy in consentElements) {
      getProvision().add(new Consent.provisionComponent()
          .addPurpose(new Coding()
              .setSystem(FhirUrls.System.Consent.Action.BASE_URL)
              .setCode(policy[ConsentPolicy.CONSENTABLE_ACTION][ConsentableAction.CODE] as String)))
    }


    // permit partly
    final boolean isPartsOnly = context.source[consent().consentPartsOnly()]
    final boolean isDeclined = context.source[consent().declined()]
    if (isPartsOnly) {
      for (final policy in context.source[consent().consentElements()]) {
        getProvision().find {
          it.getPurposeFirstRep().getCode() == policy[ConsentPolicy.CONSENTABLE_ACTION][ConsentableAction.CODE]
        }?.setType(Consent.ConsentProvisionType.PERMIT)
      }
    } else if (isDeclined) {
      provision.each { it.setType(Consent.ConsentProvisionType.PERMIT) }
    }

    // revocation
    if (context.source[consent().revocation()]) {
      final boolean isRevokePartsOnly = context.source[consent().revocation().revokePartsOnly()]
      extension {
        url = FhirUrls.Extension.Consent.REVOCATION
        extension {
          url = FhirUrls.Extension.Consent.Revocation.REVOCATION_PARTLY
          valueBoolean = isRevokePartsOnly
        }

        if (context.source[consent().revocation().signedOn()]) {
          extension {
            url = FhirUrls.Extension.Consent.Revocation.REVOCATION_DATE
            valueDateTime = context.source[consent().revocation().signedOn()]
          }
        }
      }

      //revoke partly
      if (isRevokePartsOnly && isPartsOnly) {
        final def revocationElements = context.source[consent().revocation().revocationElements()]
        for (final def policy : revocationElements) {
          getProvision().find {
            it.getPurposeFirstRep().getCode() == policy[ConsentPolicy.CONSENTABLE_ACTION][ConsentableAction.CODE]
          }?.setType(Consent.ConsentProvisionType.DENY)
        }
      } else {
        provision.each { it.setType(Consent.ConsentProvisionType.DENY) }
      }
    }
  }

  final String interpretedStatus = getStatus(validUntil as String)
  status = interpretedStatus

  final boolean hasFlexiStudy = context.source[consent().consentType().flexiStudy()] != null
  scope {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/consentscope"
      code = hasFlexiStudy ? "research" : "patient-privacy"
    }
  }

  category {
    coding {
      system = "http://loinc.org"
      code = "59284-0" // Patient Consent
    }
  }

  dateTime {
    date = context.source[consent().creationDate()]
  }

  policyRule {
    coding {
      system = "http://terminology.hl7.org/CodeSystem/v3-ActCode"
      code = "OPTINR" // opt-in with restrictions
    }
  }

  if (context.source[consent().signedOn()]) {
    verification {
      verified = true
      verificationDate {
        date = context.source[consent().signedOn().date()]
      }
    }
  }

  extension {
    url = FhirUrls.Extension.Consent.NOTES
    valueString = context.source[consent().notes()]
  }


  if (context.source[consent().revocation()] && context.source[consent().revocation().notes()]) {
    extension {
      url = FhirUrls.Extension.Consent.Revocation.REVOCATION_NOTES
      valueString = context.source[consent().revocation().notes()]
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
  return res == 1 ? "inactive" : "active"
}
