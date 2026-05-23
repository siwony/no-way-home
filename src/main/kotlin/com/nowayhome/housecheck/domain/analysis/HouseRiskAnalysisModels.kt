package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.CalculationStatus
import com.nowayhome.housecheck.domain.HousingType
import com.nowayhome.housecheck.domain.MarketPriceSourceKind
import com.nowayhome.housecheck.domain.ReportValueSourceType
import com.nowayhome.housecheck.domain.RiskLevel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class RiskReason(
    val code: String,
    val riskLevel: RiskLevel,
    val title: String,
    val detail: String,
    val sourceType: ReportValueSourceType,
)

data class RecoverySimulationResult(
    val calculationStatus: CalculationStatus,
    val note: String?,
    val estimatedAuctionValue: Long?,
    val recoverableDepositAmount: Long?,
    val shortfallAmount: Long?,
)

data class MarketValueAssessment(
    val calculationStatus: CalculationStatus,
    val note: String?,
    val estimatedMarketValue: Long?,
    val estimatedJeonseValue: Long?,
    val sourceKind: MarketPriceSourceKind?,
    val sourceLabel: String?,
    val referenceDate: LocalDate?,
)

data class RiskAnalysisContext(
    val houseCheckId: UUID,
    val depositAmount: Long,
    val landlordName: String,
    val housingType: HousingType,
    val registryDocumentIssuedDate: LocalDate?,
    val buildingLedgerIssuedDate: LocalDate?,
    val registryFindings: RegistryFindingInput?,
    val buildingLedgerFindings: BuildingLedgerFindingInput?,
    val marketValueAssessment: MarketValueAssessment,
)

data class RegistryFindingInput(
    val currentOwnerName: String,
    val ownerMatchesLandlord: Boolean,
    val hasTrustRegistration: Boolean,
    val hasSeizure: Boolean,
    val hasProvisionalSeizure: Boolean,
    val hasProvisionalDisposition: Boolean,
    val hasAuctionProceeding: Boolean,
    val hasLeaseRegistration: Boolean,
    val hasMortgage: Boolean,
    val seniorDebtAmount: Long?,
)

data class BuildingLedgerFindingInput(
    val usage: String,
    val isResidentialUseConfirmed: Boolean,
    val isViolationBuilding: Boolean,
    val isUnitConfirmed: Boolean,
    val isContractAreaConsistent: Boolean,
    val approvalDate: LocalDate?,
    val housingTypeObserved: HousingType?,
)

data class RiskAnalysisResult(
    val riskLevel: RiskLevel,
    val summary: String,
    val registrySummary: String,
    val buildingSummary: String,
    val depositSummary: String,
    val jeonseRatio: BigDecimal?,
    val totalExposureRatio: BigDecimal?,
    val valuationStatus: CalculationStatus,
    val valuationNote: String?,
    val recoveryResult: RecoverySimulationResult,
    val reasons: List<RiskReason>,
    val generatedAt: OffsetDateTime,
)
