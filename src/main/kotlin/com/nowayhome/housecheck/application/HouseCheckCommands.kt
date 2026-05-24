package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.HousingType
import com.nowayhome.housecheck.domain.MarketPriceSourceKind
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateHouseCheckRequest(
    @field:NotBlank
    val addressRoad: String,
    val addressLot: String?,
    @field:NotBlank
    val contractType: String,
    @field:NotBlank
    val housingType: String,
    @field:NotNull
    val depositAmount: Long?,
    @field:NotNull
    val monthlyRentAmount: Long?,
    @field:NotNull
    val contractPlannedDate: LocalDate?,
    @field:NotNull
    val occupancyPlannedDate: LocalDate?,
    @field:NotBlank
    val landlordName: String,
)

data class SaveRegistryFindingsRequest(
    @field:NotBlank
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

data class SaveBuildingLedgerFindingsRequest(
    @field:NotBlank
    val usage: String,
    val isResidentialUseConfirmed: Boolean,
    val isViolationBuilding: Boolean,
    val isUnitConfirmed: Boolean,
    val isContractAreaConsistent: Boolean,
    val approvalDate: LocalDate?,
    val housingTypeObserved: String?,
)

data class SaveMarketPriceRequest(
    val estimatedMarketValue: Long?,
    val estimatedJeonseValue: Long?,
    @field:NotBlank
    val sourceLabel: String,
    @field:NotNull
    val referenceDate: LocalDate?,
    val sourceKind: String? = null,
    val sampleCount: Int? = null,
    val lawdCode: String? = null,
    val dealYmdFrom: String? = null,
    val dealYmdTo: String? = null,
) {
    fun validateAmounts() {
        if (estimatedMarketValue == null && estimatedJeonseValue == null) {
            throw com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.MARKET_PRICE_NOT_AVAILABLE)
        }
    }

    fun sourceKindOrDefault(): MarketPriceSourceKind {
        val value = sourceKind?.trim()?.takeIf { it.isNotEmpty() } ?: return MarketPriceSourceKind.USER_ENTERED
        return MarketPriceSourceKind.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.VALIDATION_ERROR)
    }
}

fun SaveBuildingLedgerFindingsRequest.toHousingTypeOrNull(): HousingType? {
    val value = housingTypeObserved?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return HousingType.fromApiValue(value)
}
