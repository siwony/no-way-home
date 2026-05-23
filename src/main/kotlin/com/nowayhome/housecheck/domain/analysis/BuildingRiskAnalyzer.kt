package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.ReportValueSourceType
import com.nowayhome.housecheck.domain.RiskLevel
import org.springframework.stereotype.Component

@Component
class BuildingRiskAnalyzer {
    fun analyze(context: RiskAnalysisContext): List<RiskReason> {
        val findings = context.buildingLedgerFindings
        if (findings == null) {
            return listOf(
                RiskReason(
                    code = "BUILDING_LEDGER_REVIEW_REQUIRED",
                    riskLevel = RiskLevel.CAUTION,
                    title = "건축물대장 확인이 필요합니다",
                    detail = "현재 확인된 자료 기준으로 건축물대장 확인 결과가 없어 용도와 위반 여부 판단이 제한됩니다.",
                    sourceType = ReportValueSourceType.CALCULATED,
                ),
            )
        }

        val reasons = mutableListOf<RiskReason>()
        if (!findings.isResidentialUseConfirmed) {
            reasons += RiskReason(
                code = "NON_RESIDENTIAL_USAGE",
                riskLevel = RiskLevel.DANGER,
                title = "주거용 여부를 다시 확인해 주세요",
                detail = "현재 확인된 자료 기준으로 주거용 확인이 되지 않아 계약 목적과 공부상 용도 일치 여부를 재확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }
        if (findings.isViolationBuilding) {
            reasons += RiskReason(
                code = "VIOLATION_BUILDING",
                riskLevel = RiskLevel.DANGER,
                title = "위반건축물 여부가 확인됩니다",
                detail = "현재 확인된 자료 기준으로 위반건축물 가능성이 있어 사용 승인 및 대출·보증 제한 여부를 다시 확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }
        if (!findings.isUnitConfirmed) {
            reasons += RiskReason(
                code = "UNIT_NOT_CONFIRMED",
                riskLevel = RiskLevel.CAUTION,
                title = "호실 확인이 필요합니다",
                detail = "현재 확인된 자료 기준으로 계약 대상 호실이 공부상 명확하지 않아 동일 물건 여부를 다시 확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }
        if (!findings.isContractAreaConsistent) {
            reasons += RiskReason(
                code = "AREA_MISMATCH",
                riskLevel = RiskLevel.CAUTION,
                title = "계약 면적 일치 여부 확인 필요",
                detail = "현재 확인된 자료 기준으로 계약 면적과 공부상 면적이 다를 수 있어 면적 기준을 다시 확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }
        if (findings.housingTypeObserved != null && findings.housingTypeObserved != context.housingType) {
            reasons += RiskReason(
                code = "HOUSING_TYPE_MISMATCH",
                riskLevel = RiskLevel.CAUTION,
                title = "주택 유형 확인이 필요합니다",
                detail = "현재 확인된 자료 기준으로 입력한 주택 유형과 문서에서 확인한 유형이 달라 추가 확인이 필요합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }

        return reasons
    }
}
