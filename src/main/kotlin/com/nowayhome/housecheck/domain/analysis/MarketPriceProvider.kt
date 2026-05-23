package com.nowayhome.housecheck.domain.analysis

import com.nowayhome.housecheck.domain.HousingType
import com.nowayhome.housecheck.domain.MarketPriceSourceKind
import org.springframework.stereotype.Component
import java.time.LocalDate

data class MarketPriceLookupCommand(
    val addressRoad: String,
    val housingType: HousingType,
    val contractPlannedDate: LocalDate,
)

data class MarketPriceProviderResult(
    val estimatedMarketValue: Long?,
    val sourceLabel: String,
    val referenceDate: LocalDate,
    val sourceKind: MarketPriceSourceKind,
    val sampleCount: Int,
)

interface MarketPriceProvider {
    fun lookup(command: MarketPriceLookupCommand): MarketPriceProviderResult?
}

@Component
class NoOpMarketPriceProvider : MarketPriceProvider {
    override fun lookup(command: MarketPriceLookupCommand): MarketPriceProviderResult? = null
}
