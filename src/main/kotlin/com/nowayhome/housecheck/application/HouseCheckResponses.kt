package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.AnalysisStatus
import com.nowayhome.housecheck.domain.CalculationStatus
import com.nowayhome.housecheck.domain.ChecklistStage
import com.nowayhome.housecheck.domain.ReportValueSourceType
import com.nowayhome.housecheck.domain.RiskLevel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

enum class FileStatus {
    MISSING,
    UPLOADED,
}

enum class FindingStatus {
    NOT_STARTED,
    COMPLETED,
}

enum class MarketPriceStatus {
    MISSING,
    SAVED,
}

enum class ReportAvailability {
    NOT_READY,
    AVAILABLE,
}

data class SectionStatusResponse(
    val checkId: UUID,
    val registryFileStatus: FileStatus,
    val registryFindingStatus: FindingStatus,
    val buildingLedgerFileStatus: FileStatus,
    val buildingLedgerFindingStatus: FindingStatus,
    val marketPriceStatus: MarketPriceStatus,
    val analysisStatus: AnalysisStatus,
    val reportAvailability: ReportAvailability,
)

data class StringFieldResponse(
    val value: String?,
    val sourceType: ReportValueSourceType,
    val note: String? = null,
)

data class BooleanFieldResponse(
    val value: Boolean?,
    val sourceType: ReportValueSourceType,
    val note: String? = null,
)

data class LongFieldResponse(
    val value: Long?,
    val sourceType: ReportValueSourceType,
    val note: String? = null,
)

data class DecimalFieldResponse(
    val value: BigDecimal?,
    val sourceType: ReportValueSourceType,
    val calculationStatus: CalculationStatus,
    val note: String? = null,
)

data class DateFieldResponse(
    val value: LocalDate?,
    val sourceType: ReportValueSourceType,
    val note: String? = null,
)

data class RiskReasonResponse(
    val code: String,
    val riskLevel: RiskLevel,
    val title: String,
    val detail: String,
    val sourceType: ReportValueSourceType,
)

data class RegistryReportSectionResponse(
    val summary: String,
    val issuedDate: DateFieldResponse?,
    val currentOwnerName: StringFieldResponse?,
    val ownerMatchesLandlord: BooleanFieldResponse?,
    val seniorDebtAmount: LongFieldResponse?,
)

data class BuildingLedgerReportSectionResponse(
    val summary: String,
    val issuedDate: DateFieldResponse?,
    val usage: StringFieldResponse?,
    val isResidentialUseConfirmed: BooleanFieldResponse?,
    val isViolationBuilding: BooleanFieldResponse?,
)

data class DepositRiskSectionResponse(
    val summary: String,
    val estimatedMarketValue: LongFieldResponse?,
    val estimatedJeonseValue: LongFieldResponse?,
    val sourceLabel: StringFieldResponse?,
    val referenceDate: DateFieldResponse?,
    val jeonseRatio: DecimalFieldResponse,
    val totalExposureRatio: DecimalFieldResponse,
)

data class RecoverySimulationResponse(
    val estimatedAuctionValue: LongFieldResponse?,
    val recoverableDepositAmount: LongFieldResponse?,
    val shortfallAmount: LongFieldResponse?,
    val calculationStatus: CalculationStatus,
    val note: String?,
)

data class HouseRiskReportResponse(
    val checkId: UUID,
    val generatedAt: OffsetDateTime,
    val riskLevel: RiskLevel,
    val summary: String,
    val sectionStatus: SectionStatusResponse,
    val coreReasons: List<RiskReasonResponse>,
    val registry: RegistryReportSectionResponse,
    val buildingLedger: BuildingLedgerReportSectionResponse,
    val depositRisk: DepositRiskSectionResponse,
    val recoverySimulation: RecoverySimulationResponse,
    val additionalChecks: List<String>,
)

data class ChecklistSectionResponse(
    val stage: ChecklistStage,
    val title: String,
    val items: List<String>,
)

data class HouseChecklistResponse(
    val checkId: UUID,
    val analysisStatus: AnalysisStatus,
    val sections: List<ChecklistSectionResponse>,
)

data class ErrorResponse(
    val code: HouseCheckErrorCode,
    val message: String,
    val fieldErrors: List<FieldErrorResponse> = emptyList(),
)

data class FieldErrorResponse(
    val field: String,
    val reason: String,
)
