package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.HousingType
import org.springframework.stereotype.Component
import java.time.LocalDate

data class RoadAddressLookupResult(
    val lawdCodeCandidate: String,
    val siName: String?,
    val sggName: String?,
    val emdName: String?,
    val buildingName: String?,
    val roadAddress: String?,
    val lotAddress: String?,
)

data class MlitTransaction(
    val amount: Long?,
    val deposit: Long?,
    val monthlyRent: Long?,
    val buildingName: String?,
    val dealDate: LocalDate?,
)

data class MlitEndpointPair(
    val label: String,
    val saleUrl: String,
    val rentUrl: String,
)

@Component
class RoadAddressXmlClient(
    private val properties: MarketPriceProperties,
    private val httpClient: MarketPriceXmlHttpClient,
) {
    fun lookup(keyword: String): RoadAddressLookupResult? {
        val key = properties.jusoConfirmKey?.trim().orEmpty()
        if (key.isBlank() || keyword.isBlank()) {
            return null
        }
        val document = httpClient.getXml(
            baseUrl = properties.jusoSearchUrl,
            params = mapOf(
                "confmKey" to key,
                "currentPage" to "1",
                "countPerPage" to "5",
                "keyword" to keyword,
                "resultType" to "xml",
                "firstSort" to "road",
                "addInfoYn" to "Y",
            ),
        )
        if (document.firstText("errorCode") != "0") {
            return null
        }
        val first = document.elements("juso").firstOrNull() ?: return null
        val admCd = first.firstText("admCd") ?: return null
        return RoadAddressLookupResult(
            lawdCodeCandidate = admCd.take(5),
            siName = first.firstText("siNm"),
            sggName = first.firstText("sggNm"),
            emdName = first.firstText("emdNm"),
            buildingName = first.firstText("bdNm"),
            roadAddress = first.firstText("roadAddr", "roadAddrPart1"),
            lotAddress = first.firstText("jibunAddr"),
        )
    }
}

@Component
class LegalRegionCodeXmlClient(
    private val properties: MarketPriceProperties,
    private val httpClient: MarketPriceXmlHttpClient,
) {
    fun resolveSggCode(address: RoadAddressLookupResult): String? {
        val serviceKey = properties.molitOpenapiServiceKey?.trim().orEmpty()
        val siName = address.siName?.trim().orEmpty()
        val sggName = address.sggName?.trim().orEmpty()
        if (serviceKey.isBlank() || siName.isBlank() || sggName.isBlank()) {
            return null
        }
        val query = "$siName $sggName"
        val document = httpClient.getXml(
            baseUrl = properties.legalRegionCodeUrl,
            params = mapOf(
                "ServiceKey" to serviceKey,
                "pageNo" to "1",
                "numOfRows" to "20",
                "type" to "xml",
                "locatadd_nm" to query,
            ),
            rawValueKeys = setOf("ServiceKey"),
        )
        val rows = document.elements("row") + document.elements("item")
        return rows.firstNotNullOfOrNull { row ->
            val regionCode = row.firstText("region_cd") ?: return@firstNotNullOfOrNull null
            val addressName = row.firstText("locatadd_nm").orEmpty()
            val umdCode = row.firstText("umd_cd").orEmpty()
            val riCode = row.firstText("ri_cd").orEmpty()
            if (addressName == query && umdCode == "000" && riCode == "00") {
                regionCode.take(5)
            } else {
                null
            }
        }
    }
}

@Component
class MlitRealTransactionXmlClient(
    private val properties: MarketPriceProperties,
    private val httpClient: MarketPriceXmlHttpClient,
) {
    fun fetch(endpointUrl: String, lawdCode: String, dealYmd: String): List<MlitTransaction> {
        val serviceKey = properties.molitOpenapiServiceKey?.trim().orEmpty()
        if (serviceKey.isBlank()) {
            throw HouseCheckExternalLookupException("MOLIT service key is missing")
        }
        val document = httpClient.getXml(
            baseUrl = endpointUrl,
            params = mapOf(
                "serviceKey" to serviceKey,
                "LAWD_CD" to lawdCode,
                "DEAL_YMD" to dealYmd,
                "pageNo" to "1",
                "numOfRows" to "1000",
            ),
            rawValueKeys = setOf("serviceKey"),
        )
        val resultCode = document.firstText("resultCode").orEmpty()
        if (resultCode.isNotBlank() && resultCode !in setOf("00", "000", "INFO-0")) {
            throw HouseCheckExternalLookupException("MOLIT XML API returned resultCode=$resultCode")
        }
        return document.elements("item").map { item ->
            MlitTransaction(
                amount = parseKrwFromTenThousandUnit(item.firstText("dealAmount", "dealAmt", "거래금액")),
                deposit = parseKrwFromTenThousandUnit(item.firstText("deposit", "보증금액", "보증금")),
                monthlyRent = parseKrwFromTenThousandUnit(item.firstText("monthlyRent", "월세금액", "월세")),
                buildingName = item.firstText("aptNm", "offiNm", "officetelNm", "mhouseNm", "houseNm", "단지", "아파트"),
                dealDate = parseDealDate(item),
            )
        }
    }

    fun endpointPairFor(housingType: HousingType): MlitEndpointPair? {
        val baseUrl = properties.mlitBaseUrl.trim().removeSuffix("/")
        return when (housingType) {
            HousingType.APARTMENT -> MlitEndpointPair(
                label = "아파트",
                saleUrl = "$baseUrl/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade",
                rentUrl = "$baseUrl/RTMSDataSvcAptRent/getRTMSDataSvcAptRent",
            )
            HousingType.OFFICETEL -> MlitEndpointPair(
                label = "오피스텔",
                saleUrl = "$baseUrl/RTMSDataSvcOffiTrade/getRTMSDataSvcOffiTrade",
                rentUrl = "$baseUrl/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent",
            )
            HousingType.VILLA,
            HousingType.MULTI_HOUSEHOLD -> MlitEndpointPair(
                label = "연립다세대",
                saleUrl = "$baseUrl/RTMSDataSvcRHTrade/getRTMSDataSvcRHTrade",
                rentUrl = "$baseUrl/RTMSDataSvcRHRent/getRTMSDataSvcRHRent",
            )
            HousingType.MULTI_FAMILY -> MlitEndpointPair(
                label = "단독다가구",
                saleUrl = "$baseUrl/RTMSDataSvcSHTrade/getRTMSDataSvcSHTrade",
                rentUrl = "$baseUrl/RTMSDataSvcSHRent/getRTMSDataSvcSHRent",
            )
            HousingType.UNKNOWN -> null
        }
    }

    private fun parseKrwFromTenThousandUnit(value: String?): Long? {
        val numeric = value?.replace(",", "")
            ?.replace(" ", "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return numeric.toLongOrNull()?.times(10_000)
    }

    private fun parseDealDate(item: org.w3c.dom.Element): LocalDate? {
        val year = item.firstText("dealYear", "년")?.toIntOrNull() ?: return null
        val month = item.firstText("dealMonth", "월")?.toIntOrNull() ?: return null
        val day = item.firstText("dealDay", "일")?.toIntOrNull() ?: 1
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }
}
