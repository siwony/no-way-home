package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.CalculationStatus
import org.springframework.stereotype.Service

@Service
class RecoverySimulationService {
    private val defaultAuctionRate = 0.8.toBigDecimal()

    fun simulate(
        depositAmount: Long,
        estimatedMarketValue: Long?,
        seniorDebtAmount: Long?,
    ): RecoverySimulationResult {
        if (estimatedMarketValue == null) {
            return RecoverySimulationResult(
                calculationStatus = CalculationStatus.NOT_AVAILABLE,
                note = "현재 확인된 자료 기준으로 추정 시세가 없어 회수 시뮬레이션을 계산할 수 없습니다.",
                estimatedAuctionValue = null,
                recoverableDepositAmount = null,
                shortfallAmount = null,
            )
        }

        val estimatedAuctionValue = defaultAuctionRate.multiply(estimatedMarketValue.toBigDecimal()).longValueExact()
        val seniorDebt = seniorDebtAmount ?: 0L
        val recoverable = minOf(depositAmount, maxOf(0L, estimatedAuctionValue - seniorDebt))
        val shortfall = minOf(depositAmount, maxOf(0L, depositAmount - recoverable))

        return RecoverySimulationResult(
            calculationStatus = CalculationStatus.AVAILABLE,
            note = null,
            estimatedAuctionValue = estimatedAuctionValue,
            recoverableDepositAmount = recoverable,
            shortfallAmount = shortfall,
        )
    }
}
