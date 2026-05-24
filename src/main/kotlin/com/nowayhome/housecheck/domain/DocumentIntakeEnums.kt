package com.nowayhome.housecheck.domain

import com.nowayhome.housecheck.application.HouseCheckErrorCode

enum class DocumentIntakeDocumentType(
    val pathValue: String,
) {
    REGISTRY("registry"),
    LEASE_CONTRACT("lease-contract"),
    ;

    companion object {
        fun fromPathValue(value: String): DocumentIntakeDocumentType {
            return entries.firstOrNull { it.pathValue.equals(value.trim(), ignoreCase = true) }
                ?: throw HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_INVALID_DOCUMENT_TYPE)
        }
    }
}

enum class DocumentIntakeProcessingStatus {
    UPLOADED,
    EXTRACTING,
    REVIEW_REQUIRED,
    APPROVED,
    FAILED,
    DELETED,
}

enum class DocumentIntakeFieldReviewStatus {
    REVIEW_REQUIRED,
    APPROVED,
    EDITED,
    EXCLUDED,
}

enum class DocumentIntakeWarningType {
    ADDRESS_MISMATCH,
    LANDLORD_OWNER_MISMATCH,
    DEPOSIT_MISMATCH,
}

enum class DocumentIntakeFieldValueType {
    STRING,
    LONG,
    BOOLEAN,
    DATE,
}

enum class DocumentIntakeFieldKey(
    val documentType: DocumentIntakeDocumentType,
    val valueType: DocumentIntakeFieldValueType,
    val appliesToPayload: Boolean = true,
) {
    LEASE_ADDRESS_ROAD(DocumentIntakeDocumentType.LEASE_CONTRACT, DocumentIntakeFieldValueType.STRING),
    LEASE_ADDRESS_LOT(DocumentIntakeDocumentType.LEASE_CONTRACT, DocumentIntakeFieldValueType.STRING),
    LEASE_CONTRACT_TYPE(DocumentIntakeDocumentType.LEASE_CONTRACT, DocumentIntakeFieldValueType.STRING),
    LEASE_DEPOSIT_AMOUNT(DocumentIntakeDocumentType.LEASE_CONTRACT, DocumentIntakeFieldValueType.LONG),
    LEASE_MONTHLY_RENT_AMOUNT(DocumentIntakeDocumentType.LEASE_CONTRACT, DocumentIntakeFieldValueType.LONG),
    LEASE_CONTRACT_DATE(DocumentIntakeDocumentType.LEASE_CONTRACT, DocumentIntakeFieldValueType.DATE),
    LEASE_OCCUPANCY_DATE(DocumentIntakeDocumentType.LEASE_CONTRACT, DocumentIntakeFieldValueType.DATE),
    LEASE_LANDLORD_NAME(DocumentIntakeDocumentType.LEASE_CONTRACT, DocumentIntakeFieldValueType.STRING),
    LEASE_SPECIAL_TERMS(DocumentIntakeDocumentType.LEASE_CONTRACT, DocumentIntakeFieldValueType.STRING, appliesToPayload = false),
    REGISTRY_ADDRESS(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.STRING, appliesToPayload = false),
    REGISTRY_ISSUED_DATE(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.DATE, appliesToPayload = false),
    REGISTRY_OWNER_NAME(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.STRING),
    REGISTRY_HAS_TRUST_REGISTRATION(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.BOOLEAN),
    REGISTRY_HAS_SEIZURE(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.BOOLEAN),
    REGISTRY_HAS_PROVISIONAL_SEIZURE(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.BOOLEAN),
    REGISTRY_HAS_PROVISIONAL_DISPOSITION(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.BOOLEAN),
    REGISTRY_HAS_AUCTION_PROCEEDING(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.BOOLEAN),
    REGISTRY_HAS_LEASE_REGISTRATION(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.BOOLEAN),
    REGISTRY_HAS_MORTGAGE(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.BOOLEAN),
    REGISTRY_SENIOR_DEBT_AMOUNT(DocumentIntakeDocumentType.REGISTRY, DocumentIntakeFieldValueType.LONG),
    ;

    companion object {
        fun fromValue(value: String): DocumentIntakeFieldKey {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_FIELD_NOT_FOUND)
        }
    }
}
