package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.ReportValueSourceType
import com.nowayhome.housecheck.domain.RiskLevel
import org.springframework.stereotype.Component

@Component
class RegistryRiskAnalyzer {
    fun analyze(context: RiskAnalysisContext): List<RiskReason> {
        val findings = context.registryFindings
        if (findings == null) {
            return listOf(
                RiskReason(
                    code = "REGISTRY_REVIEW_REQUIRED",
                    riskLevel = RiskLevel.DANGER,
                    title = "등기 확인이 필요합니다",
                    detail = "현재 확인된 자료 기준으로 등기부등본 확인 결과가 없어 권리관계 판단이 제한됩니다.",
                    sourceType = ReportValueSourceType.CALCULATED,
                ),
            )
        }

        val reasons = mutableListOf<RiskReason>()

        if (!findings.ownerMatchesLandlord) {
            reasons += RiskReason(
                code = "OWNER_MISMATCH",
                riskLevel = RiskLevel.CRITICAL,
                title = "임대인과 소유자 일치 확인 필요",
                detail = "현재 확인된 자료 기준으로 임대인과 등기상 소유자가 일치하지 않아 임대 권한을 다시 확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }
        if (findings.hasAuctionProceeding || findings.hasSeizure) {
            reasons += RiskReason(
                code = "AUCTION_OR_SEIZURE",
                riskLevel = RiskLevel.CRITICAL,
                title = "압류 또는 경매 진행 이력이 확인됩니다",
                detail = "현재 확인된 자료 기준으로 압류 또는 경매 관련 등기가 있어 보증금 회수 위험이 높을 수 있습니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }
        if (findings.hasProvisionalSeizure || findings.hasProvisionalDisposition) {
            reasons += RiskReason(
                code = "PROVISIONAL_RIGHT",
                riskLevel = RiskLevel.DANGER,
                title = "가압류 또는 가처분 확인 필요",
                detail = "현재 확인된 자료 기준으로 가압류 또는 가처분이 있어 권리관계 변동 가능성을 추가 확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }
        if (findings.hasLeaseRegistration) {
            reasons += RiskReason(
                code = "LEASE_REGISTRATION",
                riskLevel = RiskLevel.DANGER,
                title = "임차권등기 확인 필요",
                detail = "현재 확인된 자료 기준으로 임차권등기가 있어 선순위 임차권 관계를 확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }
        if (findings.hasTrustRegistration) {
            reasons += RiskReason(
                code = "TRUST_REGISTRATION",
                riskLevel = RiskLevel.DANGER,
                title = "신탁등기 확인 필요",
                detail = "현재 확인된 자료 기준으로 신탁등기가 있어 임대 권한과 계약 주체를 다시 확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }
        if (findings.hasMortgage) {
            reasons += RiskReason(
                code = "MORTGAGE_PRESENT",
                riskLevel = RiskLevel.CAUTION,
                title = "근저당권 확인 필요",
                detail = "현재 확인된 자료 기준으로 선순위 채권이 있어 보증금보다 먼저 배당될 금액을 확인해야 합니다.",
                sourceType = ReportValueSourceType.CALCULATED,
            )
        }

        return reasons
    }
}
