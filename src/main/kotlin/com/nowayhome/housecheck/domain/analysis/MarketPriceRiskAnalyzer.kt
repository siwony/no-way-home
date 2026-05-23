package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.CalculationStatus
import com.nowayhome.housecheck.domain.ReportValueSourceType
import com.nowayhome.housecheck.domain.RiskLevel
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

data class MarketPriceRiskOutput(
    val reasons: List<RiskReason>,
    val jeonseRatio: BigDecimal?,
    val totalExposureRatio: BigDecimal?,
    val depositSummary: String,
)

@Component
class MarketPriceRiskAnalyzer {
    fun analyze(context: RiskAnalysisContext): MarketPriceRiskOutput {
        val reasons = mutableListOf<RiskReason>()
        val estimatedMarketValue = context.marketValueAssessment.estimatedMarketValue
        val seniorDebtAmount = context.registryFindings?.seniorDebtAmount ?: 0L

        if (context.marketValueAssessment.calculationStatus == CalculationStatus.NOT_AVAILABLE || estimatedMarketValue == null) {
            val hasJeonseOnlyInput = context.marketValueAssessment.estimatedJeonseValue != null
            reasons += if (hasJeonseOnlyInput) {
                RiskReason(
                    code = "MARKET_VALUE_REQUIRED_FOR_RATIO",
                    riskLevel = RiskLevel.CAUTION,
                    title = "매매 시세 확인이 더 필요합니다",
                    detail = context.marketValueAssessment.note
                        ?: "현재 확인된 자료 기준으로 전세 추정가만 있어 매매 시세 기준 계산이 제한됩니다.",
                    sourceType = ReportValueSourceType.CALCULATED,
                )
            } else {
                RiskReason(
                    code = "MARKET_PRICE_REQUIRED",
                    riskLevel = RiskLevel.CAUTION,
                    title = "시세 정보 확인이 필요합니다",
                    detail = context.marketValueAssessment.note
                        ?: "현재 확인된 자료 기준으로 추정 시세가 없어 보증금 위험 계산이 제한됩니다.",
                    sourceType = ReportValueSourceType.CALCULATED,
                )
            }
            return MarketPriceRiskOutput(
                reasons = reasons,
                jeonseRatio = null,
                totalExposureRatio = null,
                depositSummary = if (hasJeonseOnlyInput) {
                    "현재 확인된 자료 기준으로 전세 추정가만 저장되어 매매 시세 기준 보증금 위험 계산 일부가 제한됩니다."
                } else {
                    "현재 확인된 자료 기준으로 추정 시세 정보가 없어 보증금 위험 계산 일부가 제한됩니다."
                },
            )
        }

        val jeonseRatio = percentage(context.depositAmount, estimatedMarketValue)
        val totalExposureRatio = percentage(context.depositAmount + seniorDebtAmount, estimatedMarketValue)

        if (totalExposureRatio >= BigDecimal("100.00")) {
            reasons += RiskReason(
                code = "TOTAL_EXPOSURE_OVER_100",
                riskLevel = RiskLevel.CRITICAL,
                title = "총 위험 노출 비율이 높습니다",
                detail = "현재 확인된 자료 기준으로 선순위 채권과 보증금 합계가 추정 시세를 넘을 수 있습니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        } else if (totalExposureRatio >= BigDecimal("80.00")) {
            reasons += RiskReason(
                code = "TOTAL_EXPOSURE_OVER_80",
                riskLevel = RiskLevel.DANGER,
                title = "총 위험 노출 비율이 높을 수 있습니다",
                detail = "현재 확인된 자료 기준으로 선순위 채권과 보증금 합계 비율이 높아 배당 여력이 제한될 수 있습니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }

        if (jeonseRatio >= BigDecimal("80.00")) {
            reasons += RiskReason(
                code = "JEONSE_RATIO_OVER_80",
                riskLevel = RiskLevel.DANGER,
                title = "전세가율이 높을 수 있습니다",
                detail = "현재 확인된 자료 기준으로 보증금이 추정 시세 대비 높아 회수 위험을 추가 확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        } else if (jeonseRatio >= BigDecimal("70.00")) {
            reasons += RiskReason(
                code = "JEONSE_RATIO_OVER_70",
                riskLevel = RiskLevel.CAUTION,
                title = "전세가율을 확인해 주세요",
                detail = "현재 확인된 자료 기준으로 보증금 비율이 높아 유사 거래 시세를 더 확인하는 것이 좋습니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }

        return MarketPriceRiskOutput(
            reasons = reasons,
            jeonseRatio = jeonseRatio,
            totalExposureRatio = totalExposureRatio,
            depositSummary = "현재 확인된 자료 기준으로 보증금과 선순위 채권 비율을 계산했습니다.",
        )
    }

    private fun percentage(numerator: Long, denominator: Long): BigDecimal {
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal("100"))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
    }
}
