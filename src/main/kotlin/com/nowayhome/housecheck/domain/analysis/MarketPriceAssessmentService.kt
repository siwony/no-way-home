package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.CalculationStatus
import com.nowayhome.housecheck.domain.MarketPriceSourceKind
import org.springframework.stereotype.Component
import java.time.LocalDate

data class SavedMarketPriceInput(
    val estimatedMarketValue: Long?,
    val estimatedJeonseValue: Long?,
    val sourceLabel: String,
    val referenceDate: LocalDate,
    val sourceKind: MarketPriceSourceKind,
)

@Component
class MarketPriceAssessmentService {
    fun fromSavedSnapshot(saved: SavedMarketPriceInput?): MarketValueAssessment {
        if (saved == null || (saved.estimatedMarketValue == null && saved.estimatedJeonseValue == null)) {
            return MarketValueAssessment(
                calculationStatus = CalculationStatus.NOT_AVAILABLE,
                note = "현재 확인된 자료 기준으로 추정 시세 정보가 없어 전세가율과 총 위험 노출 비율을 계산할 수 없습니다.",
                estimatedMarketValue = null,
                estimatedJeonseValue = null,
                sourceKind = null,
                sourceLabel = saved?.sourceLabel,
                referenceDate = saved?.referenceDate,
            )
        }

        if (saved.estimatedMarketValue == null) {
            return MarketValueAssessment(
                calculationStatus = CalculationStatus.NOT_AVAILABLE,
                note = "현재 확인된 자료 기준으로 추정 전세가만 저장되어 있어 매매 시세 기준 비율과 회수 시뮬레이션은 계산할 수 없습니다.",
                estimatedMarketValue = null,
                estimatedJeonseValue = saved.estimatedJeonseValue,
                sourceKind = saved.sourceKind,
                sourceLabel = saved.sourceLabel,
                referenceDate = saved.referenceDate,
            )
        }

        return MarketValueAssessment(
            calculationStatus = CalculationStatus.AVAILABLE,
            note = null,
            estimatedMarketValue = saved.estimatedMarketValue,
            estimatedJeonseValue = saved.estimatedJeonseValue,
            sourceKind = saved.sourceKind,
            sourceLabel = saved.sourceLabel,
            referenceDate = saved.referenceDate,
        )
    }

    fun fromProviderResult(result: MarketPriceProviderResult?): Pair<MarketValueAssessment, List<RiskReason>> {
        if (result == null || result.estimatedMarketValue == null) {
            return MarketValueAssessment(
                calculationStatus = CalculationStatus.NOT_AVAILABLE,
                note = "현재 확인된 자료 기준으로 추정 시세 정보가 없어 전세가율과 총 위험 노출 비율을 계산할 수 없습니다.",
                estimatedMarketValue = null,
                estimatedJeonseValue = null,
                sourceKind = null,
                sourceLabel = result?.sourceLabel,
                referenceDate = result?.referenceDate,
            ) to listOf(
                RiskReason(
                    code = "MARKET_PRICE_UNAVAILABLE",
                    riskLevel = com.nowayhome.housecheck.domain.RiskLevel.CAUTION,
                    title = "시세 정보 확인 필요",
                    detail = "현재 확인된 자료 기준으로 추정 시세 정보가 없어 보증금 위험 계산이 제한됩니다.",
                    sourceType = com.nowayhome.housecheck.domain.ReportValueSourceType.CALCULATED,
                ),
            )
        }

        if (result.sourceKind == MarketPriceSourceKind.PUBLIC_ANNOUNCED_PRICE || result.sourceKind == MarketPriceSourceKind.REFERENCE_LISTING) {
            return MarketValueAssessment(
                calculationStatus = CalculationStatus.NOT_AVAILABLE,
                note = "현재 확인된 자료 기준으로 참고용 시세 출처만 있어 위험 계산 기준값으로 사용할 수 없습니다.",
                estimatedMarketValue = null,
                estimatedJeonseValue = null,
                sourceKind = result.sourceKind,
                sourceLabel = result.sourceLabel,
                referenceDate = result.referenceDate,
            ) to listOf(
                RiskReason(
                    code = "MARKET_PRICE_SOURCE_UNSUPPORTED",
                    riskLevel = com.nowayhome.housecheck.domain.RiskLevel.CAUTION,
                    title = "참고용 시세 출처만 확인됨",
                    detail = "개별공시지가 또는 공개 매물 호가는 위험 계산의 기준 시세로 사용하지 않습니다.",
                    sourceType = com.nowayhome.housecheck.domain.ReportValueSourceType.CALCULATED,
                ),
            )
        }

        if (result.sampleCount < 3) {
            return MarketValueAssessment(
                calculationStatus = CalculationStatus.NOT_AVAILABLE,
                note = "현재 확인된 자료 기준으로 실거래 표본이 부족해 계산 기준 시세를 확정하지 않았습니다.",
                estimatedMarketValue = null,
                estimatedJeonseValue = null,
                sourceKind = result.sourceKind,
                sourceLabel = result.sourceLabel,
                referenceDate = result.referenceDate,
            ) to listOf(
                RiskReason(
                    code = "MARKET_PRICE_LOW_CONFIDENCE",
                    riskLevel = com.nowayhome.housecheck.domain.RiskLevel.CAUTION,
                    title = "시세 표본 수가 부족합니다",
                    detail = "현재 확인된 자료 기준으로 실거래 표본이 부족해 보증금 위험 계산을 보수적으로 보류했습니다.",
                    sourceType = com.nowayhome.housecheck.domain.ReportValueSourceType.CALCULATED,
                ),
            )
        }

        return MarketValueAssessment(
            calculationStatus = CalculationStatus.AVAILABLE,
            note = null,
            estimatedMarketValue = result.estimatedMarketValue,
            estimatedJeonseValue = result.estimatedJeonseValue,
            sourceKind = result.sourceKind,
            sourceLabel = result.sourceLabel,
            referenceDate = result.referenceDate,
        ) to emptyList()
    }
}
