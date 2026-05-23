package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.HouseCheckException
import com.nowayhome.housecheck.persistence.HouseCheckRequestEntity
import com.nowayhome.housecheck.persistence.HouseCheckRequestRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class HouseCheckAccessGuard(
    private val houseCheckRequestRepository: HouseCheckRequestRepository,
) {
    fun getOwnedHouseCheck(checkId: UUID, ownerId: String): HouseCheckRequestEntity {
        val houseCheck = houseCheckRequestRepository.findById(checkId)
            .orElseThrow { HouseCheckException(HouseCheckErrorCode.HOUSE_CHECK_NOT_FOUND) }
        if (houseCheck.ownerId != ownerId) {
            throw HouseCheckException(HouseCheckErrorCode.ACCESS_DENIED)
        }
        return houseCheck
    }
}
