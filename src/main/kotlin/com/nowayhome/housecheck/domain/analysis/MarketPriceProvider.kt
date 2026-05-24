package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.HousingType
import com.nowayhome.housecheck.domain.MarketPriceSourceKind
import java.time.LocalDate

data class MarketPriceLookupCommand(
    val addressRoad: String,
    val addressLot: String?,
    val housingType: HousingType,
    val contractPlannedDate: LocalDate,
)

data class MarketPriceProviderResult(
    val estimatedMarketValue: Long?,
    val estimatedJeonseValue: Long? = null,
    val sourceLabel: String,
    val referenceDate: LocalDate,
    val sourceKind: MarketPriceSourceKind,
    val sampleCount: Int,
    val marketSampleCount: Int = sampleCount,
    val jeonseSampleCount: Int = 0,
    val lawdCode: String? = null,
    val dealYmdFrom: String? = null,
    val dealYmdTo: String? = null,
    val warnings: List<String> = emptyList(),
)

interface MarketPriceProvider {
    fun lookup(command: MarketPriceLookupCommand): MarketPriceProviderResult?
}

class NoOpMarketPriceProvider : MarketPriceProvider {
    override fun lookup(command: MarketPriceLookupCommand): MarketPriceProviderResult? = null
}
