package com.nowayhome.housecheck.application

enum class HouseCheckErrorCode(
    val defaultMessage: String,
) {
    HOUSE_CHECK_NOT_FOUND("진단 요청을 찾을 수 없습니다."),
    INVALID_CONTRACT_TYPE("계약 유형이 올바르지 않습니다."),
    INVALID_HOUSING_TYPE("주택 유형이 올바르지 않습니다."),
    INVALID_DEPOSIT_AMOUNT("보증금은 0 이상이어야 합니다."),
    INVALID_MONTHLY_RENT_AMOUNT("월세는 0 이상이어야 합니다."),
    INVALID_MARKET_PRICE_AMOUNT("시세 금액은 0 이상이어야 합니다."),
    MARKET_PRICE_NOT_AVAILABLE("시세 정보를 확인할 수 없습니다."),
    REGISTRY_FILE_REQUIRED("등기부등본 업로드가 필요합니다."),
    BUILDING_LEDGER_FILE_REQUIRED("건축물대장 업로드가 필요합니다."),
    INVALID_FILE_TYPE("PDF 파일만 업로드할 수 있습니다."),
    ACCESS_DENIED("해당 진단 요청에 접근할 수 없습니다."),
    ANALYSIS_NOT_READY("분석 결과가 아직 준비되지 않았습니다."),
    VALIDATION_ERROR("요청 값을 다시 확인해 주세요."),
}
