package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeFieldReviewStatus
import com.nowayhome.housecheck.domain.DocumentIntakeProcessingStatus
import com.nowayhome.housecheck.domain.DocumentIntakeWarningType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class DocumentIntakeDocumentFailureResponse(
    val code: String,
    val message: String,
)

data class DocumentIntakeDocumentResponse(
    val documentType: DocumentIntakeDocumentType,
    val processingStatus: DocumentIntakeProcessingStatus,
    val fileName: String?,
    val mimeType: String?,
    val uploadedAt: OffsetDateTime?,
    val processedAt: OffsetDateTime?,
    val failure: DocumentIntakeDocumentFailureResponse?,
)

data class DocumentIntakeFieldResponse(
    val fieldKey: DocumentIntakeFieldKey,
    val value: String?,
    val sourceDocument: DocumentIntakeDocumentType,
    val sourcePage: Int?,
    val sourceText: String?,
    val confidence: BigDecimal,
    val reviewStatus: DocumentIntakeFieldReviewStatus,
)

data class DocumentIntakeWarningResponse(
    val type: DocumentIntakeWarningType,
    val message: String,
    val relatedFields: List<DocumentIntakeFieldKey>,
)

data class DocumentIntakeSessionResponse(
    val sessionId: UUID,
    val documents: List<DocumentIntakeDocumentResponse>,
    val fields: List<DocumentIntakeFieldResponse>,
    val warnings: List<DocumentIntakeWarningResponse>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
)

data class DocumentIntakeContractFormPayload(
    val addressRoad: String?,
    val addressLot: String?,
    val contractType: String?,
    val depositAmount: Long?,
    val monthlyRentAmount: Long?,
    val contractPlannedDate: LocalDate?,
    val occupancyPlannedDate: LocalDate?,
    val landlordName: String?,
)

data class DocumentIntakeRegistryFindingsPayload(
    val currentOwnerName: String?,
    val ownerMatchesLandlord: Boolean?,
    val hasTrustRegistration: Boolean?,
    val hasSeizure: Boolean?,
    val hasProvisionalSeizure: Boolean?,
    val hasProvisionalDisposition: Boolean?,
    val hasAuctionProceeding: Boolean?,
    val hasLeaseRegistration: Boolean?,
    val hasMortgage: Boolean?,
    val seniorDebtAmount: Long?,
)

data class DocumentIntakeApplicationPayloadResponse(
    val sessionId: UUID,
    val approvedFieldCount: Int,
    val contractForm: DocumentIntakeContractFormPayload,
    val registryFindingsForm: DocumentIntakeRegistryFindingsPayload,
)
