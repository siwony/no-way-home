package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.MarketPriceSourceKind
import com.nowayhome.housecheck.domain.analysis.MarketPriceLookupCommand
import com.nowayhome.housecheck.domain.analysis.MarketPriceProvider
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

enum class MarketPriceLookupConfidence {
    AVAILABLE,
    LOW_CONFIDENCE,
    UNAVAILABLE,
}

data class MarketPriceLookupResponse(
    val estimatedMarketValue: Long?,
    val estimatedJeonseValue: Long?,
    val sourceLabel: String,
    val referenceDate: LocalDate,
    val sourceKind: MarketPriceSourceKind,
    val sampleCount: Int,
    val marketSampleCount: Int,
    val jeonseSampleCount: Int,
    val lawdCode: String?,
    val dealYmdFrom: String?,
    val dealYmdTo: String?,
    val confidence: MarketPriceLookupConfidence,
    val warnings: List<String>,
)

@Service
class MarketPriceLookupService(
    private val houseCheckAccessGuard: HouseCheckAccessGuard,
    private val marketPriceProvider: MarketPriceProvider,
) {
    fun lookup(checkId: UUID, ownerId: String): MarketPriceLookupResponse {
        val houseCheck = houseCheckAccessGuard.getOwnedHouseCheck(checkId, ownerId)
        val result = try {
            marketPriceProvider.lookup(
                MarketPriceLookupCommand(
                    addressRoad = houseCheck.addressRoad,
                    addressLot = houseCheck.addressLot,
                    housingType = houseCheck.housingType,
                    contractPlannedDate = houseCheck.contractPlannedDate,
                ),
            )
        } catch (exception: HouseCheckExternalLookupException) {
            throw com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.MARKET_PRICE_LOOKUP_FAILED)
        }

        if (result == null) {
            throw com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.MARKET_PRICE_PROVIDER_UNAVAILABLE)
        }

        val confidence = when {
            result.estimatedMarketValue != null -> MarketPriceLookupConfidence.AVAILABLE
            result.marketSampleCount > 0 || result.jeonseSampleCount > 0 -> MarketPriceLookupConfidence.LOW_CONFIDENCE
            else -> MarketPriceLookupConfidence.UNAVAILABLE
        }
        return MarketPriceLookupResponse(
            estimatedMarketValue = result.estimatedMarketValue,
            estimatedJeonseValue = result.estimatedJeonseValue,
            sourceLabel = result.sourceLabel,
            referenceDate = result.referenceDate,
            sourceKind = result.sourceKind,
            sampleCount = result.sampleCount,
            marketSampleCount = result.marketSampleCount,
            jeonseSampleCount = result.jeonseSampleCount,
            lawdCode = result.lawdCode,
            dealYmdFrom = result.dealYmdFrom,
            dealYmdTo = result.dealYmdTo,
            confidence = confidence,
            warnings = result.warnings,
        )
    }
}
