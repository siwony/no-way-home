package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeFieldReviewStatus
import com.nowayhome.housecheck.domain.DocumentIntakeWarningType
import com.nowayhome.housecheck.persistence.DocumentIntakeExtractedFieldEntity
import com.nowayhome.housecheck.persistence.DocumentIntakeWarningEntity
import org.springframework.stereotype.Component

@Component
class DocumentIntakeWarningPolicy {
    fun buildWarnings(
        fields: List<DocumentIntakeExtractedFieldEntity>,
        persistedWarnings: List<DocumentIntakeWarningEntity>,
    ): List<DocumentIntakeWarningResponse> {
        val responses = linkedMapOf<DocumentIntakeWarningType, DocumentIntakeWarningResponse>()

        persistedWarnings.forEach { warning ->
            responses[warning.warningType] = DocumentIntakeWarningResponse(
                type = warning.warningType,
                message = warning.message,
                relatedFields = warning.relatedFieldKeys.split(',')
                    .mapNotNull { rawValue ->
                        rawValue.takeIf { it.isNotBlank() }?.let(DocumentIntakeFieldKey::fromValue)
                    },
            )
        }

        val currentValues = fields
            .filter { it.reviewStatus != DocumentIntakeFieldReviewStatus.EXCLUDED }
            .associateBy(
            keySelector = { DocumentIntakeFieldKey.fromValue(it.fieldKey) },
            valueTransform = { it.reviewedValue ?: it.rawValue },
        )

        addAddressMismatch(currentValues, responses)
        addLandlordOwnerMismatch(currentValues, responses)

        return responses.values.toList()
    }

    private fun addAddressMismatch(
        currentValues: Map<DocumentIntakeFieldKey, String?>,
        responses: MutableMap<DocumentIntakeWarningType, DocumentIntakeWarningResponse>,
    ) {
        val leaseAddress = currentValues[DocumentIntakeFieldKey.LEASE_ADDRESS_ROAD]?.normalizeForComparison()
        val registryAddress = currentValues[DocumentIntakeFieldKey.REGISTRY_ADDRESS]?.normalizeForComparison()
        if (leaseAddress != null && registryAddress != null && leaseAddress != registryAddress) {
            responses[DocumentIntakeWarningType.ADDRESS_MISMATCH] = DocumentIntakeWarningResponse(
                type = DocumentIntakeWarningType.ADDRESS_MISMATCH,
                message = "계약서와 등기부등본의 주소 표기가 다를 수 있어 확인이 필요합니다.",
                relatedFields = listOf(
                    DocumentIntakeFieldKey.LEASE_ADDRESS_ROAD,
                    DocumentIntakeFieldKey.REGISTRY_ADDRESS,
                ),
            )
        }
    }

    private fun addLandlordOwnerMismatch(
        currentValues: Map<DocumentIntakeFieldKey, String?>,
        responses: MutableMap<DocumentIntakeWarningType, DocumentIntakeWarningResponse>,
    ) {
        val landlordName = currentValues[DocumentIntakeFieldKey.LEASE_LANDLORD_NAME]?.normalizeForComparison()
        val ownerName = currentValues[DocumentIntakeFieldKey.REGISTRY_OWNER_NAME]?.normalizeForComparison()
        if (landlordName != null && ownerName != null && landlordName != ownerName) {
            responses[DocumentIntakeWarningType.LANDLORD_OWNER_MISMATCH] = DocumentIntakeWarningResponse(
                type = DocumentIntakeWarningType.LANDLORD_OWNER_MISMATCH,
                message = "계약서의 임대인명과 등기부등본의 소유자명이 다를 수 있어 직접 확인이 필요합니다.",
                relatedFields = listOf(
                    DocumentIntakeFieldKey.LEASE_LANDLORD_NAME,
                    DocumentIntakeFieldKey.REGISTRY_OWNER_NAME,
                ),
            )
        }
    }

    private fun String.normalizeForComparison(): String {
        return lowercase().replace("\\s+".toRegex(), "")
    }
}
