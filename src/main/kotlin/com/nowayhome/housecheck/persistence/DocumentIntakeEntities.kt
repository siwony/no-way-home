package com.nowayhome.housecheck.persistence

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.nowayhome.housecheck.domain.DocumentIntakeFieldReviewStatus
import com.nowayhome.housecheck.domain.DocumentIntakeProcessingStatus
import com.nowayhome.housecheck.domain.DocumentIntakeWarningType
import com.nowayhome.housecheck.security.EncryptedStringAttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "document_intake_session")
class DocumentIntakeSessionEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "owner_id", nullable = false, length = 120)
    var ownerId: String = "",
    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(
    name = "document_intake_document",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_document_intake_document_type", columnNames = ["session_id", "document_type"]),
        UniqueConstraint(name = "uk_document_intake_storage_key", columnNames = ["storage_key"]),
    ],
)
class DocumentIntakeDocumentEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "session_id", nullable = false)
    var sessionId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    var documentType: DocumentIntakeDocumentType = DocumentIntakeDocumentType.REGISTRY,
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 40)
    var processingStatus: DocumentIntakeProcessingStatus = DocumentIntakeProcessingStatus.UPLOADED,
    @Convert(converter = EncryptedStringAttributeConverter::class)
    @Column(name = "original_file_name", nullable = false)
    var originalFileName: String = "",
    @Column(name = "mime_type", nullable = false, length = 120)
    var mimeType: String = "",
    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0,
    @Column(name = "storage_key")
    var storageKey: String? = null,
    @Column(name = "failure_code", length = 80)
    var failureCode: String? = null,
    @Column(name = "failure_message", length = 255)
    var failureMessage: String? = null,
    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "processed_at")
    var processedAt: OffsetDateTime? = null,
    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,
)

@Entity
@Table(
    name = "document_intake_extracted_field",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_document_intake_field_key", columnNames = ["document_id", "field_key"]),
    ],
)
class DocumentIntakeExtractedFieldEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "session_id", nullable = false)
    var sessionId: UUID = UUID.randomUUID(),
    @Column(name = "document_id", nullable = false)
    var documentId: UUID = UUID.randomUUID(),
    @Column(name = "field_key", nullable = false, length = 80)
    var fieldKey: String = "",
    @Convert(converter = EncryptedStringAttributeConverter::class)
    @Column(name = "raw_value", nullable = false, columnDefinition = "text")
    var rawValue: String = "",
    @Convert(converter = EncryptedStringAttributeConverter::class)
    @Column(name = "reviewed_value", columnDefinition = "text")
    var reviewedValue: String? = null,
    @Column(name = "source_page")
    var sourcePage: Int? = null,
    @Convert(converter = EncryptedStringAttributeConverter::class)
    @Column(name = "source_text", columnDefinition = "text")
    var sourceText: String? = null,
    @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
    var confidence: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 40)
    var reviewStatus: DocumentIntakeFieldReviewStatus = DocumentIntakeFieldReviewStatus.REVIEW_REQUIRED,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "document_intake_warning")
class DocumentIntakeWarningEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "session_id", nullable = false)
    var sessionId: UUID = UUID.randomUUID(),
    @Column(name = "document_id")
    var documentId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false, length = 40)
    var warningType: DocumentIntakeWarningType = DocumentIntakeWarningType.ADDRESS_MISMATCH,
    @Column(name = "message", nullable = false, length = 255)
    var message: String = "",
    @Column(name = "related_field_keys", nullable = false, length = 255)
    var relatedFieldKeys: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
)
