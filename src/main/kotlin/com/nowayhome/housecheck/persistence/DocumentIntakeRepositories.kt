package com.nowayhome.housecheck.persistence

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DocumentIntakeSessionRepository : JpaRepository<DocumentIntakeSessionEntity, UUID>

interface DocumentIntakeDocumentRepository : JpaRepository<DocumentIntakeDocumentEntity, UUID> {
    fun findAllBySessionIdOrderByUploadedAtAsc(sessionId: UUID): List<DocumentIntakeDocumentEntity>

    fun findBySessionIdAndDocumentType(sessionId: UUID, documentType: DocumentIntakeDocumentType): DocumentIntakeDocumentEntity?
}

interface DocumentIntakeExtractedFieldRepository : JpaRepository<DocumentIntakeExtractedFieldEntity, UUID> {
    fun findAllBySessionIdOrderByCreatedAtAsc(sessionId: UUID): List<DocumentIntakeExtractedFieldEntity>

    fun findAllByDocumentIdOrderByCreatedAtAsc(documentId: UUID): List<DocumentIntakeExtractedFieldEntity>

    fun findBySessionIdAndFieldKey(sessionId: UUID, fieldKey: String): DocumentIntakeExtractedFieldEntity?

    fun deleteAllByDocumentId(documentId: UUID)
}

interface DocumentIntakeWarningRepository : JpaRepository<DocumentIntakeWarningEntity, UUID> {
    fun findAllBySessionIdOrderByCreatedAtAsc(sessionId: UUID): List<DocumentIntakeWarningEntity>

    fun deleteAllByDocumentId(documentId: UUID)
}
