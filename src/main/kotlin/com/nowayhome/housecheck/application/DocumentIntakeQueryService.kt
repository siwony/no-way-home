package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.persistence.DocumentIntakeDocumentRepository
import com.nowayhome.housecheck.persistence.DocumentIntakeExtractedFieldRepository
import com.nowayhome.housecheck.persistence.DocumentIntakeWarningRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DocumentIntakeQueryService(
    private val documentIntakeAccessGuard: DocumentIntakeAccessGuard,
    private val documentIntakeDocumentRepository: DocumentIntakeDocumentRepository,
    private val documentIntakeExtractedFieldRepository: DocumentIntakeExtractedFieldRepository,
    private val documentIntakeWarningRepository: DocumentIntakeWarningRepository,
    private val documentIntakeSessionResponseAssembler: DocumentIntakeSessionResponseAssembler,
    private val documentIntakeApplicationPayloadAssembler: DocumentIntakeApplicationPayloadAssembler,
) {
    fun getSession(sessionId: UUID, ownerId: String): DocumentIntakeSessionResponse {
        val session = documentIntakeAccessGuard.getOwnedSession(sessionId, ownerId)
        return documentIntakeSessionResponseAssembler.assemble(
            session = session,
            documents = documentIntakeDocumentRepository.findAllBySessionIdOrderByUploadedAtAsc(sessionId),
            fields = documentIntakeExtractedFieldRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId),
            warnings = documentIntakeWarningRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId),
        )
    }

    fun getApplicationPayload(sessionId: UUID, ownerId: String): DocumentIntakeApplicationPayloadResponse {
        documentIntakeAccessGuard.getOwnedSession(sessionId, ownerId)
        val fields = documentIntakeExtractedFieldRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)
        return documentIntakeApplicationPayloadAssembler.assemble(sessionId, fields)
    }
}
