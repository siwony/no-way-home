package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeFieldReviewStatus
import com.nowayhome.housecheck.domain.DocumentIntakeProcessingStatus
import com.nowayhome.housecheck.persistence.DocumentIntakeDocumentEntity
import com.nowayhome.housecheck.persistence.DocumentIntakeDocumentRepository
import com.nowayhome.housecheck.persistence.DocumentIntakeExtractedFieldEntity
import com.nowayhome.housecheck.persistence.DocumentIntakeExtractedFieldRepository
import com.nowayhome.housecheck.persistence.DocumentIntakeSessionEntity
import com.nowayhome.housecheck.persistence.DocumentIntakeSessionRepository
import com.nowayhome.housecheck.persistence.DocumentIntakeWarningEntity
import com.nowayhome.housecheck.persistence.DocumentIntakeWarningRepository
import com.nowayhome.housecheck.storage.DocumentIntakeDocumentStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime
import java.util.UUID

@Service
class DocumentIntakeCommandService(
    private val documentIntakeSessionRepository: DocumentIntakeSessionRepository,
    private val documentIntakeDocumentRepository: DocumentIntakeDocumentRepository,
    private val documentIntakeExtractedFieldRepository: DocumentIntakeExtractedFieldRepository,
    private val documentIntakeWarningRepository: DocumentIntakeWarningRepository,
    private val documentIntakeAccessGuard: DocumentIntakeAccessGuard,
    private val documentIntakeFilePolicy: DocumentIntakeFilePolicy,
    private val documentIntakeExtractionPort: DocumentIntakeExtractionPort,
    private val documentIntakeFieldValueParser: DocumentIntakeFieldValueParser,
    private val documentIntakeDocumentStorage: DocumentIntakeDocumentStorage,
    private val documentIntakeSessionResponseAssembler: DocumentIntakeSessionResponseAssembler,
    @param:Value("\${housecheck.document-intake.retention-days:30}") private val retentionDays: Long,
) {
    @Transactional
    fun createSession(ownerId: String): DocumentIntakeSessionResponse {
        val now = OffsetDateTime.now()
        val session = documentIntakeSessionRepository.save(
            DocumentIntakeSessionEntity(
                ownerId = ownerId,
                createdAt = now,
                updatedAt = now,
                expiresAt = now.plusDays(retentionDays),
            ),
        )
        return buildSessionResponse(session)
    }

    @Transactional
    fun uploadDocument(
        sessionId: UUID,
        ownerId: String,
        documentType: DocumentIntakeDocumentType,
        file: MultipartFile,
    ): DocumentIntakeSessionResponse {
        val session = documentIntakeAccessGuard.getOwnedSession(sessionId, ownerId)
        val validatedFile = documentIntakeFilePolicy.validate(documentType, file)
        val now = OffsetDateTime.now()

        val existingDocument = documentIntakeDocumentRepository.findBySessionIdAndDocumentType(sessionId, documentType)
        existingDocument?.storageKey?.let(documentIntakeDocumentStorage::delete)
        existingDocument?.let {
            documentIntakeExtractedFieldRepository.deleteAllByDocumentId(it.id)
            documentIntakeWarningRepository.deleteAllByDocumentId(it.id)
        }

        val storedDocument = documentIntakeDocumentStorage.store(
            sessionId = sessionId,
            documentType = documentType,
            fileName = validatedFile.originalFileName,
            bytes = validatedFile.bytes,
        )

        val document = (existingDocument ?: DocumentIntakeDocumentEntity(
            sessionId = sessionId,
            documentType = documentType,
        )).apply {
            originalFileName = validatedFile.originalFileName
            mimeType = validatedFile.mimeType
            fileSize = validatedFile.bytes.size.toLong()
            storageKey = storedDocument.storageKey
            processingStatus = DocumentIntakeProcessingStatus.UPLOADED
            failureCode = null
            failureMessage = null
            deletedAt = null
            uploadedAt = now
            processedAt = null
        }
        documentIntakeDocumentRepository.save(document)

        document.processingStatus = DocumentIntakeProcessingStatus.EXTRACTING
        documentIntakeDocumentRepository.save(document)

        try {
            val extractedResult = documentIntakeExtractionPort.extract(
                documentType = documentType,
                originalFileName = validatedFile.originalFileName,
                contentType = validatedFile.mimeType,
                bytes = validatedFile.bytes,
            )
            saveExtractedFields(sessionId, document, extractedResult, now)
            document.processingStatus = DocumentIntakeProcessingStatus.REVIEW_REQUIRED
            document.processedAt = now
            documentIntakeDocumentRepository.save(document)
        } catch (exception: DocumentIntakeExtractionFailureException) {
            document.processingStatus = DocumentIntakeProcessingStatus.FAILED
            document.failureCode = exception.code
            document.failureMessage = exception.message
            document.processedAt = now
            documentIntakeDocumentRepository.save(document)
        }

        session.updatedAt = now
        documentIntakeSessionRepository.save(session)
        return buildSessionResponse(session)
    }

    @Transactional
    fun reviewField(
        sessionId: UUID,
        ownerId: String,
        fieldKeyValue: String,
        request: ReviewDocumentIntakeFieldRequest,
    ): DocumentIntakeSessionResponse {
        val session = documentIntakeAccessGuard.getOwnedSession(sessionId, ownerId)
        val action = DocumentIntakeReviewAction.fromValue(request.action)
        val field = documentIntakeExtractedFieldRepository.findBySessionIdAndFieldKey(
            sessionId = sessionId,
            fieldKey = DocumentIntakeFieldKey.fromValue(fieldKeyValue).name,
        ) ?: throw com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_FIELD_NOT_FOUND)
        val now = OffsetDateTime.now()

        when (action) {
            DocumentIntakeReviewAction.APPROVE -> {
                field.reviewedValue = null
                field.reviewStatus = DocumentIntakeFieldReviewStatus.APPROVED
            }

            DocumentIntakeReviewAction.EDIT -> {
                val fieldKey = DocumentIntakeFieldKey.fromValue(field.fieldKey)
                field.reviewedValue = documentIntakeFieldValueParser.normalize(fieldKey, request.editedValue)
                field.reviewStatus = DocumentIntakeFieldReviewStatus.EDITED
            }

            DocumentIntakeReviewAction.EXCLUDE -> {
                field.reviewedValue = null
                field.reviewStatus = DocumentIntakeFieldReviewStatus.EXCLUDED
            }
        }

        field.updatedAt = now
        documentIntakeExtractedFieldRepository.save(field)
        refreshDocumentStatus(field.documentId, now)

        session.updatedAt = now
        documentIntakeSessionRepository.save(session)
        return buildSessionResponse(session)
    }

    @Transactional
    fun deleteDocument(
        sessionId: UUID,
        ownerId: String,
        documentType: DocumentIntakeDocumentType,
    ): DocumentIntakeSessionResponse {
        val session = documentIntakeAccessGuard.getOwnedSession(sessionId, ownerId)
        val document = documentIntakeDocumentRepository.findBySessionIdAndDocumentType(sessionId, documentType)
            ?: throw com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_NOT_FOUND)
        val now = OffsetDateTime.now()

        document.storageKey?.let(documentIntakeDocumentStorage::delete)
        document.storageKey = null
        document.processingStatus = DocumentIntakeProcessingStatus.DELETED
        document.deletedAt = now
        document.processedAt = now
        document.failureCode = null
        document.failureMessage = null
        documentIntakeDocumentRepository.save(document)

        documentIntakeExtractedFieldRepository.deleteAllByDocumentId(document.id)
        documentIntakeWarningRepository.deleteAllByDocumentId(document.id)

        session.updatedAt = now
        documentIntakeSessionRepository.save(session)
        return buildSessionResponse(session)
    }

    private fun saveExtractedFields(
        sessionId: UUID,
        document: DocumentIntakeDocumentEntity,
        extractedResult: ExtractedDocumentResult,
        now: OffsetDateTime,
    ) {
        documentIntakeExtractedFieldRepository.saveAll(
            extractedResult.fields.map { extractedField ->
                DocumentIntakeExtractedFieldEntity(
                    sessionId = sessionId,
                    documentId = document.id,
                    fieldKey = extractedField.fieldKey.name,
                    rawValue = extractedField.value,
                    reviewedValue = null,
                    sourcePage = extractedField.sourcePage,
                    sourceText = extractedField.sourceText,
                    confidence = extractedField.confidence,
                    reviewStatus = DocumentIntakeFieldReviewStatus.REVIEW_REQUIRED,
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
        documentIntakeWarningRepository.saveAll(
            extractedResult.warnings.map { warning ->
                DocumentIntakeWarningEntity(
                    sessionId = sessionId,
                    documentId = document.id,
                    warningType = warning.warningType,
                    message = warning.message,
                    relatedFieldKeys = warning.relatedFieldKeys.joinToString(",") { it.name },
                    createdAt = now,
                )
            },
        )
    }

    private fun refreshDocumentStatus(documentId: UUID, now: OffsetDateTime) {
        val document = documentIntakeDocumentRepository.findById(documentId)
            .orElseThrow { com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_NOT_FOUND) }
        if (document.processingStatus == DocumentIntakeProcessingStatus.DELETED || document.processingStatus == DocumentIntakeProcessingStatus.FAILED) {
            return
        }
        val hasPendingReview = documentIntakeExtractedFieldRepository.findAllByDocumentIdOrderByCreatedAtAsc(documentId)
            .any { it.reviewStatus == DocumentIntakeFieldReviewStatus.REVIEW_REQUIRED }
        document.processingStatus = if (hasPendingReview) {
            DocumentIntakeProcessingStatus.REVIEW_REQUIRED
        } else {
            DocumentIntakeProcessingStatus.APPROVED
        }
        document.processedAt = now
        documentIntakeDocumentRepository.save(document)
    }

    private fun buildSessionResponse(session: DocumentIntakeSessionEntity): DocumentIntakeSessionResponse {
        return documentIntakeSessionResponseAssembler.assemble(
            session = session,
            documents = documentIntakeDocumentRepository.findAllBySessionIdOrderByUploadedAtAsc(session.id),
            fields = documentIntakeExtractedFieldRepository.findAllBySessionIdOrderByCreatedAtAsc(session.id),
            warnings = documentIntakeWarningRepository.findAllBySessionIdOrderByCreatedAtAsc(session.id),
        )
    }
}
