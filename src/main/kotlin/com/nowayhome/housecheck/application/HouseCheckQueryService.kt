package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.AnalysisStatus
import com.nowayhome.housecheck.domain.CalculationStatus
import com.nowayhome.housecheck.domain.DocumentType
import com.nowayhome.housecheck.domain.ReportValueSourceType
import com.nowayhome.housecheck.persistence.BuildingLedgerManualFindingRepository
import com.nowayhome.housecheck.persistence.HouseCheckDocumentRepository
import com.nowayhome.housecheck.persistence.HouseCheckRequestRepository
import com.nowayhome.housecheck.persistence.MarketPriceSnapshotRepository
import com.nowayhome.housecheck.persistence.RegistryManualFindingRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HouseCheckQueryService(
    private val houseCheckRequestRepository: HouseCheckRequestRepository,
    private val houseCheckDocumentRepository: HouseCheckDocumentRepository,
    private val registryManualFindingRepository: RegistryManualFindingRepository,
    private val buildingLedgerManualFindingRepository: BuildingLedgerManualFindingRepository,
    private val marketPriceSnapshotRepository: MarketPriceSnapshotRepository,
    private val houseCheckAccessGuard: HouseCheckAccessGuard,
    private val houseRiskAnalysisService: HouseRiskAnalysisService,
    private val checklistService: ChecklistService,
) {
    fun getReport(checkId: UUID, ownerId: String): HouseRiskReportResponse {
        val houseCheck = houseCheckAccessGuard.getOwnedHouseCheck(checkId, ownerId)
        if (houseCheck.analysisStatus != AnalysisStatus.COMPLETED) {
            throw com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.ANALYSIS_NOT_READY)
        }
        val sectionStatus = buildSectionStatus(houseCheck.id)
        val report = houseRiskAnalysisService.getReport(checkId)
        val reasons = houseRiskAnalysisService.getReasons(report.id)
        val registryDocument = houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(checkId, DocumentType.REGISTRY)
        val buildingDocument = houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(checkId, DocumentType.BUILDING_LEDGER)
        val registryFinding = registryManualFindingRepository.findByHouseCheckId(checkId)
        val buildingFinding = buildingLedgerManualFindingRepository.findByHouseCheckId(checkId)
        val marketPrice = marketPriceSnapshotRepository.findByHouseCheckId(checkId)

        return HouseRiskReportResponse(
            checkId = checkId,
            generatedAt = report.generatedAt,
            riskLevel = report.riskLevel,
            summary = report.summary,
            sectionStatus = sectionStatus,
            coreReasons = reasons.map {
                RiskReasonResponse(
                    code = it.reasonCode,
                    riskLevel = it.riskLevel,
                    title = it.title,
                    detail = it.detail,
                    sourceType = ReportValueSourceType.valueOf(it.sourceType),
                )
            },
            registry = RegistryReportSectionResponse(
                summary = report.registrySummary,
                issuedDate = registryDocument?.let {
                    DateFieldResponse(it.issuedDate, ReportValueSourceType.UPLOADED_FILE_METADATA)
                },
                currentOwnerName = registryFinding?.let {
                    StringFieldResponse(it.currentOwnerName, ReportValueSourceType.USER_ENTERED)
                },
                ownerMatchesLandlord = registryFinding?.let {
                    BooleanFieldResponse(it.ownerMatchesLandlord, ReportValueSourceType.USER_ENTERED)
                },
                seniorDebtAmount = registryFinding?.let {
                    LongFieldResponse(it.seniorDebtAmount, ReportValueSourceType.USER_ENTERED)
                },
            ),
            buildingLedger = BuildingLedgerReportSectionResponse(
                summary = report.buildingSummary,
                issuedDate = buildingDocument?.let {
                    DateFieldResponse(it.issuedDate, ReportValueSourceType.UPLOADED_FILE_METADATA)
                },
                usage = buildingFinding?.let {
                    StringFieldResponse(it.usage, ReportValueSourceType.USER_ENTERED)
                },
                isResidentialUseConfirmed = buildingFinding?.let {
                    BooleanFieldResponse(it.isResidentialUseConfirmed, ReportValueSourceType.USER_ENTERED)
                },
                isViolationBuilding = buildingFinding?.let {
                    BooleanFieldResponse(it.isViolationBuilding, ReportValueSourceType.USER_ENTERED)
                },
            ),
            depositRisk = DepositRiskSectionResponse(
                summary = report.depositSummary,
                estimatedMarketValue = marketPrice?.let {
                    LongFieldResponse(it.estimatedMarketValue, ReportValueSourceType.USER_ENTERED)
                },
                estimatedJeonseValue = marketPrice?.let {
                    LongFieldResponse(it.estimatedJeonseValue, ReportValueSourceType.USER_ENTERED)
                },
                sourceLabel = marketPrice?.let {
                    StringFieldResponse(it.sourceLabel, ReportValueSourceType.USER_ENTERED)
                },
                referenceDate = marketPrice?.let {
                    DateFieldResponse(it.referenceDate, ReportValueSourceType.USER_ENTERED)
                },
                jeonseRatio = DecimalFieldResponse(
                    value = report.jeonseRatio,
                    sourceType = ReportValueSourceType.CALCULATED,
                    calculationStatus = CalculationStatus.valueOf(report.valuationStatus),
                    note = report.valuationNote,
                ),
                totalExposureRatio = DecimalFieldResponse(
                    value = report.totalExposureRatio,
                    sourceType = ReportValueSourceType.CALCULATED,
                    calculationStatus = CalculationStatus.valueOf(report.valuationStatus),
                    note = report.valuationNote,
                ),
            ),
            recoverySimulation = RecoverySimulationResponse(
                estimatedAuctionValue = LongFieldResponse(
                    report.estimatedAuctionValue,
                    ReportValueSourceType.CALCULATED,
                ),
                recoverableDepositAmount = LongFieldResponse(
                    report.recoverableDepositAmount,
                    ReportValueSourceType.CALCULATED,
                ),
                shortfallAmount = LongFieldResponse(
                    report.shortfallAmount,
                    ReportValueSourceType.CALCULATED,
                ),
                calculationStatus = CalculationStatus.valueOf(report.recoveryStatus),
                note = report.recoveryNote,
            ),
            additionalChecks = checklistService.buildChecklist(houseCheck, registryFinding, buildingFinding, marketPrice)
                .sections
                .flatMap { it.items }
                .distinct()
                .take(5),
        )
    }

    fun getChecklist(checkId: UUID, ownerId: String): HouseChecklistResponse {
        val houseCheck = houseCheckAccessGuard.getOwnedHouseCheck(checkId, ownerId)
        return checklistService.buildChecklist(
            houseCheck = houseCheck,
            registryFinding = registryManualFindingRepository.findByHouseCheckId(checkId),
            buildingFinding = buildingLedgerManualFindingRepository.findByHouseCheckId(checkId),
            marketPrice = marketPriceSnapshotRepository.findByHouseCheckId(checkId),
        )
    }

    private fun buildSectionStatus(checkId: UUID): SectionStatusResponse {
        val houseCheck = houseCheckRequestRepository.findById(checkId)
            .orElseThrow { com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.HOUSE_CHECK_NOT_FOUND) }
        return SectionStatusResponse(
            checkId = checkId,
            registryFileStatus = if (houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(checkId, DocumentType.REGISTRY) == null) {
                FileStatus.MISSING
            } else {
                FileStatus.UPLOADED
            },
            registryFindingStatus = if (registryManualFindingRepository.findByHouseCheckId(checkId) == null) {
                FindingStatus.NOT_STARTED
            } else {
                FindingStatus.COMPLETED
            },
            buildingLedgerFileStatus = if (houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(checkId, DocumentType.BUILDING_LEDGER) == null) {
                FileStatus.MISSING
            } else {
                FileStatus.UPLOADED
            },
            buildingLedgerFindingStatus = if (buildingLedgerManualFindingRepository.findByHouseCheckId(checkId) == null) {
                FindingStatus.NOT_STARTED
            } else {
                FindingStatus.COMPLETED
            },
            marketPriceStatus = if (marketPriceSnapshotRepository.findByHouseCheckId(checkId) == null) {
                MarketPriceStatus.MISSING
            } else {
                MarketPriceStatus.SAVED
            },
            analysisStatus = houseCheck.analysisStatus,
            reportAvailability = if (houseCheck.analysisStatus == AnalysisStatus.COMPLETED) {
                ReportAvailability.AVAILABLE
            } else {
                ReportAvailability.NOT_READY
            },
        )
    }
}
