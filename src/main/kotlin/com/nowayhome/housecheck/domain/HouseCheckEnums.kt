package com.nowayhome.housecheck.domain

import com.nowayhome.housecheck.application.HouseCheckErrorCode

enum class ContractType {
    JEONSE,
    MONTHLY_RENT,
    ;

    companion object {
        fun fromApiValue(value: String): ContractType {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw HouseCheckException(HouseCheckErrorCode.INVALID_CONTRACT_TYPE)
        }
    }
}

enum class HousingType {
    APARTMENT,
    OFFICETEL,
    VILLA,
    MULTI_HOUSEHOLD,
    MULTI_FAMILY,
    UNKNOWN,
    ;

    companion object {
        fun fromApiValue(value: String): HousingType {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw HouseCheckException(HouseCheckErrorCode.INVALID_HOUSING_TYPE)
        }
    }
}

enum class AnalysisStatus {
    NOT_RUN,
    RUNNING,
    COMPLETED,
    FAILED,
}

enum class RiskLevel {
    SAFE,
    CAUTION,
    DANGER,
    CRITICAL,
    ;
}

enum class DocumentType {
    REGISTRY,
    BUILDING_LEDGER,
}

enum class ReportValueSourceType {
    USER_ENTERED,
    UPLOADED_FILE_METADATA,
    CALCULATED,
}

enum class CalculationStatus {
    AVAILABLE,
    NOT_AVAILABLE,
}

enum class ChecklistStage {
    BEFORE_CONTRACT,
    RIGHT_BEFORE_CONTRACT,
    AFTER_CONTRACT,
}

enum class MarketPriceSourceKind {
    USER_ENTERED,
    MLIT_REAL_TRANSACTION,
    PUBLIC_ANNOUNCED_PRICE,
    REFERENCE_LISTING,
}

