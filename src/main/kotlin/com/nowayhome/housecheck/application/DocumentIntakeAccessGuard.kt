package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.HouseCheckException
import com.nowayhome.housecheck.persistence.DocumentIntakeSessionEntity
import com.nowayhome.housecheck.persistence.DocumentIntakeSessionRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DocumentIntakeAccessGuard(
    private val documentIntakeSessionRepository: DocumentIntakeSessionRepository,
) {
    fun getOwnedSession(sessionId: UUID, ownerId: String): DocumentIntakeSessionEntity {
        val session = documentIntakeSessionRepository.findById(sessionId)
            .orElseThrow { HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_NOT_FOUND) }
        if (session.ownerId != ownerId) {
            throw HouseCheckException(HouseCheckErrorCode.ACCESS_DENIED)
        }
        return session
    }
}
