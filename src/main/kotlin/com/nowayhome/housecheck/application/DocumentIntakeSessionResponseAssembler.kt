package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeProcessingStatus
import com.nowayhome.housecheck.persistence.DocumentIntakeDocumentEntity
import com.nowayhome.housecheck.persistence.DocumentIntakeExtractedFieldEntity
import com.nowayhome.housecheck.persistence.DocumentIntakeSessionEntity
import com.nowayhome.housecheck.persistence.DocumentIntakeWarningEntity
import org.springframework.stereotype.Component

@Component
class DocumentIntakeSessionResponseAssembler(
    private val documentIntakeWarningPolicy: DocumentIntakeWarningPolicy,
) {
    fun assemble(
        session: DocumentIntakeSessionEntity,
        documents: List<DocumentIntakeDocumentEntity>,
        fields: List<DocumentIntakeExtractedFieldEntity>,
        warnings: List<DocumentIntakeWarningEntity>,
    ): DocumentIntakeSessionResponse {
        val documentMap = documents.associateBy { it.id }
        val visibleFields = fields.filter { field ->
            documentMap[field.documentId]?.processingStatus != DocumentIntakeProcessingStatus.DELETED
        }
        return DocumentIntakeSessionResponse(
            sessionId = session.id,
            documents = documents.sortedBy { it.documentType.pathValue }.map { document ->
                DocumentIntakeDocumentResponse(
                    documentType = document.documentType,
                    processingStatus = document.processingStatus,
                    fileName = if (document.processingStatus == DocumentIntakeProcessingStatus.DELETED) null else document.originalFileName,
                    mimeType = if (document.processingStatus == DocumentIntakeProcessingStatus.DELETED) null else document.mimeType,
                    uploadedAt = document.uploadedAt,
                    processedAt = document.processedAt,
                    failure = document.failureCode?.let { code ->
                        DocumentIntakeDocumentFailureResponse(
                            code = code,
                            message = document.failureMessage ?: HouseCheckErrorCode.VALIDATION_ERROR.defaultMessage,
                        )
                    },
                )
            },
            fields = visibleFields.sortedWith(compareBy({ DocumentIntakeFieldKey.fromValue(it.fieldKey).documentType.pathValue }, { it.fieldKey }))
                .map { field ->
                    DocumentIntakeFieldResponse(
                        fieldKey = DocumentIntakeFieldKey.fromValue(field.fieldKey),
                        value = field.reviewedValue ?: field.rawValue,
                        sourceDocument = documentMap.getValue(field.documentId).documentType,
                        sourcePage = field.sourcePage,
                        sourceText = field.sourceText,
                        confidence = field.confidence,
                        reviewStatus = field.reviewStatus,
                    )
                },
            warnings = documentIntakeWarningPolicy.buildWarnings(visibleFields, warnings),
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
            expiresAt = session.expiresAt,
        )
    }
}
