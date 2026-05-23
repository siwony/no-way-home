package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.AnalysisStatus
import com.nowayhome.housecheck.domain.CalculationStatus
import com.nowayhome.housecheck.domain.DocumentType
import com.nowayhome.housecheck.domain.ReportValueSourceType
import com.nowayhome.housecheck.domain.analysis.BuildingLedgerFindingInput
import com.nowayhome.housecheck.domain.analysis.BuildingRiskAnalyzer
import com.nowayhome.housecheck.domain.analysis.MarketPriceAssessmentService
import com.nowayhome.housecheck.domain.analysis.MarketPriceLookupCommand
import com.nowayhome.housecheck.domain.analysis.MarketPriceProvider
import com.nowayhome.housecheck.domain.analysis.MarketPriceRiskAnalyzer
import com.nowayhome.housecheck.domain.analysis.RegistryFindingInput
import com.nowayhome.housecheck.domain.analysis.RegistryRiskAnalyzer
import com.nowayhome.housecheck.domain.analysis.RecoverySimulationService
import com.nowayhome.housecheck.domain.analysis.RiskAnalysisContext
import com.nowayhome.housecheck.domain.analysis.RiskLevelPolicy
import com.nowayhome.housecheck.domain.analysis.RiskReason
import com.nowayhome.housecheck.domain.analysis.SavedMarketPriceInput
import com.nowayhome.housecheck.persistence.BuildingLedgerManualFindingRepository
import com.nowayhome.housecheck.persistence.HouseCheckDocumentRepository
import com.nowayhome.housecheck.persistence.HouseCheckRequestEntity
import com.nowayhome.housecheck.persistence.HouseCheckRequestRepository
import com.nowayhome.housecheck.persistence.HouseRiskReasonEntity
import com.nowayhome.housecheck.persistence.HouseRiskReasonRepository
import com.nowayhome.housecheck.persistence.HouseRiskReportEntity
import com.nowayhome.housecheck.persistence.HouseRiskReportRepository
import com.nowayhome.housecheck.persistence.MarketPriceSnapshotRepository
import com.nowayhome.housecheck.persistence.RegistryManualFindingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class HouseRiskAnalysisService(
    private val houseCheckRequestRepository: HouseCheckRequestRepository,
    private val houseCheckDocumentRepository: HouseCheckDocumentRepository,
    private val registryManualFindingRepository: RegistryManualFindingRepository,
    private val buildingLedgerManualFindingRepository: BuildingLedgerManualFindingRepository,
    private val marketPriceSnapshotRepository: MarketPriceSnapshotRepository,
    private val houseRiskReportRepository: HouseRiskReportRepository,
    private val houseRiskReasonRepository: HouseRiskReasonRepository,
    private val registryRiskAnalyzer: RegistryRiskAnalyzer,
    private val buildingRiskAnalyzer: BuildingRiskAnalyzer,
    private val marketPriceRiskAnalyzer: MarketPriceRiskAnalyzer,
    private val recoverySimulationService: RecoverySimulationService,
    private val riskLevelPolicy: RiskLevelPolicy,
    private val marketPriceAssessmentService: MarketPriceAssessmentService,
    private val marketPriceProvider: MarketPriceProvider,
) {
    @Transactional
    fun analyze(houseCheck: HouseCheckRequestEntity): HouseRiskReportEntity {
        houseCheck.analysisStatus = AnalysisStatus.RUNNING
        houseCheck.updatedAt = OffsetDateTime.now()
        houseCheckRequestRepository.save(houseCheck)

        return try {
            val report = createOrUpdateReport(houseCheck)
            houseCheck.analysisStatus = AnalysisStatus.COMPLETED
            houseCheck.updatedAt = OffsetDateTime.now()
            houseCheckRequestRepository.save(houseCheck)
            report
        } catch (exception: RuntimeException) {
            houseCheck.analysisStatus = AnalysisStatus.FAILED
            houseCheck.updatedAt = OffsetDateTime.now()
            houseCheckRequestRepository.save(houseCheck)
            throw exception
        }
    }

    fun getReport(checkId: UUID): HouseRiskReportEntity {
        return houseRiskReportRepository.findByHouseCheckId(checkId)
            ?: throw com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.ANALYSIS_NOT_READY)
    }

    fun getReasons(reportId: UUID): List<HouseRiskReasonEntity> {
        return houseRiskReasonRepository.findAllByReportIdOrderByDisplayOrderAsc(reportId)
    }

    private fun createOrUpdateReport(houseCheck: HouseCheckRequestEntity): HouseRiskReportEntity {
        val registryDocument = houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(houseCheck.id, DocumentType.REGISTRY)
        val buildingDocument = houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(houseCheck.id, DocumentType.BUILDING_LEDGER)
        val registryFindings = registryManualFindingRepository.findByHouseCheckId(houseCheck.id)
        val buildingFindings = buildingLedgerManualFindingRepository.findByHouseCheckId(houseCheck.id)
        val savedMarketPrice = marketPriceSnapshotRepository.findByHouseCheckId(houseCheck.id)

        val providerAssessment = if (savedMarketPrice == null) {
            marketPriceAssessmentService.fromProviderResult(
                marketPriceProvider.lookup(
                    MarketPriceLookupCommand(
                        addressRoad = houseCheck.addressRoad,
                        housingType = houseCheck.housingType,
                        contractPlannedDate = houseCheck.contractPlannedDate,
                    ),
                ),
            )
        } else {
            null
        }

        val marketAssessment = savedMarketPrice?.let {
            marketPriceAssessmentService.fromSavedSnapshot(
                SavedMarketPriceInput(
                    estimatedMarketValue = it.estimatedMarketValue,
                    estimatedJeonseValue = it.estimatedJeonseValue,
                    sourceLabel = it.sourceLabel,
                    referenceDate = it.referenceDate,
                ),
            )
        } ?: providerAssessment!!.first
        val providerReasons = providerAssessment?.second.orEmpty()

        val context = RiskAnalysisContext(
            houseCheckId = houseCheck.id,
            depositAmount = houseCheck.depositAmount,
            landlordName = houseCheck.landlordName,
            housingType = houseCheck.housingType,
            registryDocumentIssuedDate = registryDocument?.issuedDate,
            buildingLedgerIssuedDate = buildingDocument?.issuedDate,
            registryFindings = registryFindings?.let {
                RegistryFindingInput(
                    currentOwnerName = it.currentOwnerName,
                    ownerMatchesLandlord = it.ownerMatchesLandlord,
                    hasTrustRegistration = it.hasTrustRegistration,
                    hasSeizure = it.hasSeizure,
                    hasProvisionalSeizure = it.hasProvisionalSeizure,
                    hasProvisionalDisposition = it.hasProvisionalDisposition,
                    hasAuctionProceeding = it.hasAuctionProceeding,
                    hasLeaseRegistration = it.hasLeaseRegistration,
                    hasMortgage = it.hasMortgage,
                    seniorDebtAmount = it.seniorDebtAmount,
                )
            },
            buildingLedgerFindings = buildingFindings?.let {
                BuildingLedgerFindingInput(
                    usage = it.usage,
                    isResidentialUseConfirmed = it.isResidentialUseConfirmed,
                    isViolationBuilding = it.isViolationBuilding,
                    isUnitConfirmed = it.isUnitConfirmed,
                    isContractAreaConsistent = it.isContractAreaConsistent,
                    approvalDate = it.approvalDate,
                    housingTypeObserved = it.housingTypeObserved,
                )
            },
            marketValueAssessment = marketAssessment,
        )

        val registryReasons = registryRiskAnalyzer.analyze(context)
        val buildingReasons = buildingRiskAnalyzer.analyze(context)
        val marketOutput = marketPriceRiskAnalyzer.analyze(context)
        val recoveryResult = recoverySimulationService.simulate(
            depositAmount = houseCheck.depositAmount,
            estimatedMarketValue = marketAssessment.estimatedMarketValue,
            seniorDebtAmount = registryFindings?.seniorDebtAmount,
        )

        val recoveryReasons = if (
            recoveryResult.calculationStatus == CalculationStatus.AVAILABLE &&
            (recoveryResult.shortfallAmount ?: 0L) > 0
        ) {
            listOf(
                RiskReason(
                    code = "RECOVERY_SHORTFALL",
                    riskLevel = if ((recoveryResult.shortfallAmount ?: 0L) >= houseCheck.depositAmount / 5) {
                        com.nowayhome.housecheck.domain.RiskLevel.DANGER
                    } else {
                        com.nowayhome.housecheck.domain.RiskLevel.CAUTION
                    },
                    title = "회수 부족 가능성 확인 필요",
                    detail = "현재 확인된 자료 기준으로 보수적 회수 시뮬레이션에서 보증금 부족액이 발생할 수 있습니다.",
                    sourceType = ReportValueSourceType.CALCULATED,
                ),
            )
        } else {
            emptyList()
        }

        val allReasons = (registryReasons + buildingReasons + providerReasons + marketOutput.reasons + recoveryReasons)
            .sortedByDescending { it.riskLevel.ordinal }
        val finalRiskLevel = riskLevelPolicy.resolve(allReasons)
        val summary = when (finalRiskLevel) {
            com.nowayhome.housecheck.domain.RiskLevel.CRITICAL ->
                "현재 확인된 자료 기준으로 보증금 회수 위험이 높을 수 있습니다."
            com.nowayhome.housecheck.domain.RiskLevel.DANGER ->
                "현재 확인된 자료 기준으로 주요 위험 신호가 있어 추가 확인이 필요합니다."
            com.nowayhome.housecheck.domain.RiskLevel.CAUTION ->
                "현재 확인된 자료 기준으로 추가 확인이 필요한 항목이 있습니다."
            com.nowayhome.housecheck.domain.RiskLevel.SAFE ->
                "현재 확인된 자료 기준 특이 위험은 확인되지 않았습니다."
        }

        val generatedAt = OffsetDateTime.now()
        val persisted = houseRiskReportRepository.findByHouseCheckId(houseCheck.id) ?: HouseRiskReportEntity(
            houseCheckId = houseCheck.id,
        )
        persisted.riskLevel = finalRiskLevel
        persisted.summary = summary
        persisted.registrySummary = registrySummary(registryFindings != null, registryDocument?.issuedDate)
        persisted.buildingSummary = buildingSummary(buildingFindings != null, buildingDocument?.issuedDate)
        persisted.depositSummary = marketOutput.depositSummary
        persisted.jeonseRatio = marketOutput.jeonseRatio
        persisted.totalExposureRatio = marketOutput.totalExposureRatio
        persisted.valuationStatus = marketAssessment.calculationStatus.name
        persisted.valuationNote = marketAssessment.note
        persisted.recoveryStatus = recoveryResult.calculationStatus.name
        persisted.recoveryNote = recoveryResult.note
        persisted.estimatedAuctionValue = recoveryResult.estimatedAuctionValue
        persisted.recoverableDepositAmount = recoveryResult.recoverableDepositAmount
        persisted.shortfallAmount = recoveryResult.shortfallAmount
        persisted.generatedAt = generatedAt

        val savedReport = houseRiskReportRepository.save(persisted)
        houseRiskReasonRepository.deleteAllByReportId(savedReport.id)
        houseRiskReasonRepository.saveAll(
            allReasons.take(5).mapIndexed { index, reason ->
                HouseRiskReasonEntity(
                    reportId = savedReport.id,
                    reasonCode = reason.code,
                    riskLevel = reason.riskLevel,
                    title = reason.title,
                    detail = reason.detail,
                    sourceType = reason.sourceType.name,
                    displayOrder = index,
                )
            },
        )
        return savedReport
    }

    private fun registrySummary(hasFindings: Boolean, issuedDate: java.time.LocalDate?): String {
        if (!hasFindings) {
            return "현재 확인된 자료 기준으로 등기 확인 결과가 없어 권리관계 판단이 제한됩니다."
        }
        return if (issuedDate == null) {
            "현재 확인된 자료 기준으로 등기 확인 결과가 저장됐지만 발급일 확인이 필요합니다."
        } else {
            "현재 확인된 자료 기준으로 등기 확인 결과와 발급일이 저장됐습니다."
        }
    }

    private fun buildingSummary(hasFindings: Boolean, issuedDate: java.time.LocalDate?): String {
        if (!hasFindings) {
            return "현재 확인된 자료 기준으로 건축물대장 확인 결과가 없어 용도와 위반 여부 판단이 제한됩니다."
        }
        return if (issuedDate == null) {
            "현재 확인된 자료 기준으로 건축물대장 확인 결과가 저장됐지만 발급일 확인이 필요합니다."
        } else {
            "현재 확인된 자료 기준으로 건축물대장 확인 결과와 발급일이 저장됐습니다."
        }
    }
}
