package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeFieldReviewStatus
import com.nowayhome.housecheck.persistence.DocumentIntakeExtractedFieldEntity
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class DocumentIntakeApplicationPayloadAssembler {
    fun assemble(sessionId: UUID, fields: List<DocumentIntakeExtractedFieldEntity>): DocumentIntakeApplicationPayloadResponse {
        val approvedFields = fields.filter {
            it.reviewStatus == DocumentIntakeFieldReviewStatus.APPROVED || it.reviewStatus == DocumentIntakeFieldReviewStatus.EDITED
        }
        val currentValues = approvedFields.associateBy(
            keySelector = { DocumentIntakeFieldKey.fromValue(it.fieldKey) },
            valueTransform = { it.reviewedValue ?: it.rawValue },
        )

        val landlordName = currentValues[DocumentIntakeFieldKey.LEASE_LANDLORD_NAME]
        val ownerName = currentValues[DocumentIntakeFieldKey.REGISTRY_OWNER_NAME]

        return DocumentIntakeApplicationPayloadResponse(
            sessionId = sessionId,
            approvedFieldCount = approvedFields.count { DocumentIntakeFieldKey.fromValue(it.fieldKey).appliesToPayload },
            contractForm = DocumentIntakeContractFormPayload(
                addressRoad = currentValues[DocumentIntakeFieldKey.LEASE_ADDRESS_ROAD],
                addressLot = currentValues[DocumentIntakeFieldKey.LEASE_ADDRESS_LOT],
                contractType = currentValues[DocumentIntakeFieldKey.LEASE_CONTRACT_TYPE],
                depositAmount = currentValues[DocumentIntakeFieldKey.LEASE_DEPOSIT_AMOUNT]?.toLongOrNull(),
                monthlyRentAmount = currentValues[DocumentIntakeFieldKey.LEASE_MONTHLY_RENT_AMOUNT]?.toLongOrNull(),
                contractPlannedDate = currentValues[DocumentIntakeFieldKey.LEASE_CONTRACT_DATE]?.let(LocalDate::parse),
                occupancyPlannedDate = currentValues[DocumentIntakeFieldKey.LEASE_OCCUPANCY_DATE]?.let(LocalDate::parse),
                landlordName = landlordName,
            ),
            registryFindingsForm = DocumentIntakeRegistryFindingsPayload(
                currentOwnerName = ownerName,
                ownerMatchesLandlord = if (landlordName != null && ownerName != null) {
                    landlordName.trim().equals(ownerName.trim(), ignoreCase = true)
                } else {
                    null
                },
                hasTrustRegistration = currentValues[DocumentIntakeFieldKey.REGISTRY_HAS_TRUST_REGISTRATION]?.toBooleanStrictOrNull(),
                hasSeizure = currentValues[DocumentIntakeFieldKey.REGISTRY_HAS_SEIZURE]?.toBooleanStrictOrNull(),
                hasProvisionalSeizure = currentValues[DocumentIntakeFieldKey.REGISTRY_HAS_PROVISIONAL_SEIZURE]?.toBooleanStrictOrNull(),
                hasProvisionalDisposition = currentValues[DocumentIntakeFieldKey.REGISTRY_HAS_PROVISIONAL_DISPOSITION]?.toBooleanStrictOrNull(),
                hasAuctionProceeding = currentValues[DocumentIntakeFieldKey.REGISTRY_HAS_AUCTION_PROCEEDING]?.toBooleanStrictOrNull(),
                hasLeaseRegistration = currentValues[DocumentIntakeFieldKey.REGISTRY_HAS_LEASE_REGISTRATION]?.toBooleanStrictOrNull(),
                hasMortgage = currentValues[DocumentIntakeFieldKey.REGISTRY_HAS_MORTGAGE]?.toBooleanStrictOrNull(),
                seniorDebtAmount = currentValues[DocumentIntakeFieldKey.REGISTRY_SENIOR_DEBT_AMOUNT]?.toLongOrNull(),
            ),
        )
    }
}
