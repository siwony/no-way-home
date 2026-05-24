package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.CalculationStatus
import com.nowayhome.housecheck.domain.HousingType
import com.nowayhome.housecheck.domain.MarketPriceSourceKind
import com.nowayhome.housecheck.domain.ReportValueSourceType
import com.nowayhome.housecheck.domain.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.time.LocalDate
import java.util.UUID

class HouseRiskAnalysisComponentsTest {
    private val registryRiskAnalyzer = RegistryRiskAnalyzer()
    private val marketPriceRiskAnalyzer = MarketPriceRiskAnalyzer()
    private val recoverySimulationService = RecoverySimulationService()
    private val riskLevelPolicy = RiskLevelPolicy()
    private val marketPriceAssessmentService = MarketPriceAssessmentService()

    @Test
    fun calculatesRatiosAndCriticalExposureFromMarketPrice() {
        val output = marketPriceRiskAnalyzer.analyze(
            riskAnalysisContext(
                depositAmount = 80_000_000,
                registryFindings = RegistryFindingInput(
                    currentOwnerName = "owner",
                    ownerMatchesLandlord = true,
                    hasTrustRegistration = false,
                    hasSeizure = false,
                    hasProvisionalSeizure = false,
                    hasProvisionalDisposition = false,
                    hasAuctionProceeding = false,
                    hasLeaseRegistration = false,
                    hasMortgage = true,
                    seniorDebtAmount = 20_000_000,
                ),
                marketValueAssessment = MarketValueAssessment(
                    calculationStatus = CalculationStatus.AVAILABLE,
                    note = null,
                    estimatedMarketValue = 100_000_000,
                    estimatedJeonseValue = null,
                    sourceKind = MarketPriceSourceKind.USER_ENTERED,
                    sourceLabel = "manual",
                    referenceDate = LocalDate.now(),
                ),
            ),
        )

        assertEquals("80.00", output.jeonseRatio.toString())
        assertEquals("100.00", output.totalExposureRatio.toString())
        assertTrue(output.reasons.any { it.code == "JEONSE_RATIO_OVER_80" })
        assertTrue(output.reasons.any { it.code == "TOTAL_EXPOSURE_OVER_100" && it.riskLevel == RiskLevel.CRITICAL })
    }

    @Test
    fun calculatesRecoverySimulationWithinDepositBounds() {
        val result = recoverySimulationService.simulate(
            depositAmount = 80_000_000,
            estimatedMarketValue = 100_000_000,
            seniorDebtAmount = 30_000_000,
        )

        assertEquals(CalculationStatus.AVAILABLE, result.calculationStatus)
        assertEquals(80_000_000, result.estimatedAuctionValue)
        assertEquals(50_000_000, result.recoverableDepositAmount)
        assertEquals(30_000_000, result.shortfallAmount)
    }

    @Test
    fun returnsDangerWhenRegistryFindingsAreMissing() {
        val reasons = registryRiskAnalyzer.analyze(
            riskAnalysisContext(
                depositAmount = 50_000_000,
                registryFindings = null,
                marketValueAssessment = MarketValueAssessment(
                    calculationStatus = CalculationStatus.NOT_AVAILABLE,
                    note = "missing",
                    estimatedMarketValue = null,
                    estimatedJeonseValue = null,
                    sourceKind = null,
                    sourceLabel = null,
                    referenceDate = null,
                ),
            ),
        )

        assertEquals(RiskLevel.DANGER, reasons.single().riskLevel)
        assertEquals("REGISTRY_REVIEW_REQUIRED", reasons.single().code)
    }

    @Test
    fun choosesHighestRiskLevelAcrossReasons() {
        val level = riskLevelPolicy.resolve(
            listOf(
                RiskReason("a", RiskLevel.CAUTION, "caution", "detail", ReportValueSourceType.CALCULATED),
                RiskReason("b", RiskLevel.CRITICAL, "critical", "detail", ReportValueSourceType.CALCULATED),
                RiskReason("c", RiskLevel.DANGER, "danger", "detail", ReportValueSourceType.CALCULATED),
            ),
        )

        assertEquals(RiskLevel.CRITICAL, level)
    }

    @Test
    fun ignoresUnsupportedExternalMarketSourcesForRiskCalculation() {
        val (assessment, reasons) = marketPriceAssessmentService.fromProviderResult(
            MarketPriceProviderResult(
                estimatedMarketValue = 120_000_000,
                sourceLabel = "public",
                referenceDate = LocalDate.now(),
                sourceKind = MarketPriceSourceKind.PUBLIC_ANNOUNCED_PRICE,
                sampleCount = 10,
            ),
        )

        assertEquals(CalculationStatus.NOT_AVAILABLE, assessment.calculationStatus)
        assertEquals("MARKET_PRICE_SOURCE_UNSUPPORTED", reasons.single().code)
    }

    @Test
    fun keepsJeonseOnlySnapshotAsSavedInputWithoutPretendingMarketValueExists() {
        val assessment = marketPriceAssessmentService.fromSavedSnapshot(
            SavedMarketPriceInput(
                estimatedMarketValue = null,
                estimatedJeonseValue = 65_000_000,
                sourceLabel = "manual",
                referenceDate = LocalDate.now(),
                sourceKind = MarketPriceSourceKind.USER_ENTERED,
            ),
        )

        val output = marketPriceRiskAnalyzer.analyze(
            riskAnalysisContext(
                depositAmount = 60_000_000,
                registryFindings = null,
                marketValueAssessment = assessment,
            ),
        )

        assertEquals(CalculationStatus.NOT_AVAILABLE, assessment.calculationStatus)
        assertEquals(65_000_000, assessment.estimatedJeonseValue)
        assertTrue(output.reasons.none { it.code == "MARKET_PRICE_REQUIRED" })
        assertTrue(output.reasons.any { it.code == "MARKET_VALUE_REQUIRED_FOR_RATIO" })
    }

    private fun riskAnalysisContext(
        depositAmount: Long,
        registryFindings: RegistryFindingInput?,
        marketValueAssessment: MarketValueAssessment,
    ): RiskAnalysisContext {
        return RiskAnalysisContext(
            houseCheckId = UUID.randomUUID(),
            depositAmount = depositAmount,
            landlordName = "landlord",
            housingType = HousingType.APARTMENT,
            registryDocumentIssuedDate = LocalDate.now(),
            buildingLedgerIssuedDate = LocalDate.now(),
            registryFindings = registryFindings,
            buildingLedgerFindings = null,
            marketValueAssessment = marketValueAssessment,
        )
    }
}
