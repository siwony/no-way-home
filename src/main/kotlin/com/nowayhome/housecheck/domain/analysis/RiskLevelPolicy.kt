package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.RiskLevel
import org.springframework.stereotype.Component

@Component
class RiskLevelPolicy {
    private val priority = mapOf(
        RiskLevel.SAFE to 0,
        RiskLevel.CAUTION to 1,
        RiskLevel.DANGER to 2,
        RiskLevel.CRITICAL to 3,
    )

    fun resolve(reasons: List<RiskReason>): RiskLevel {
        return reasons.maxByOrNull { priority.getValue(it.riskLevel) }?.riskLevel ?: RiskLevel.SAFE
    }
}
