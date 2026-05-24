package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.MarketPriceSourceKind
import com.nowayhome.housecheck.domain.analysis.MarketPriceLookupCommand
import com.nowayhome.housecheck.domain.analysis.MarketPriceProvider
import com.nowayhome.housecheck.domain.analysis.MarketPriceProviderResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
@ConditionalOnProperty(prefix = "housecheck.market-price", name = ["provider"], havingValue = "mlit")
class MlitMarketPriceProvider(
    private val properties: MarketPriceProperties,
    private val roadAddressXmlClient: RoadAddressXmlClient,
    private val legalRegionCodeXmlClient: LegalRegionCodeXmlClient,
    private val mlitRealTransactionXmlClient: MlitRealTransactionXmlClient,
) : MarketPriceProvider {
    override fun lookup(command: MarketPriceLookupCommand): MarketPriceProviderResult? {
        val endpointPair = mlitRealTransactionXmlClient.endpointPairFor(command.housingType) ?: return unavailableResult(
            command = command,
            warnings = listOf("지원하지 않는 주택 유형입니다."),
        )
        val address = roadAddressXmlClient.lookup(command.addressRoad)
            ?: command.addressLot?.let(roadAddressXmlClient::lookup)
            ?: return unavailableResult(command, endpointPair.label, listOf("주소를 법정동 코드로 해석하지 못했습니다."))
        val lawdCode = legalRegionCodeXmlClient.resolveSggCode(address) ?: address.lawdCodeCandidate
        if (lawdCode.length != 5) {
            return unavailableResult(command, endpointPair.label, listOf("실거래가 조회용 시군구 코드를 확인하지 못했습니다."))
        }

        val dealMonths = dealMonths(command)
        val warnings = mutableListOf<String>()
        val saleSamples = dealMonths.flatMap { mlitRealTransactionXmlClient.fetch(endpointPair.saleUrl, lawdCode, it) }
            .selectPreferredSamples(address, warnings)
        val rentSamples = dealMonths.flatMap { mlitRealTransactionXmlClient.fetch(endpointPair.rentUrl, lawdCode, it) }
            .filter { (it.monthlyRent ?: 0L) == 0L }
            .selectPreferredSamples(address, warnings)

        val marketValues = saleSamples.mapNotNull { it.amount }
        val jeonseValues = rentSamples.mapNotNull { it.deposit }
        val estimatedMarketValue = marketValues.takeIf { it.size >= properties.minSampleCount }?.median()
        val estimatedJeonseValue = jeonseValues.takeIf { it.size >= properties.minSampleCount }?.median()

        if (marketValues.size < properties.minSampleCount) {
            warnings += "매매 실거래 표본이 ${properties.minSampleCount}건 미만이라 추정 매매가를 확정하지 않았습니다."
        }
        if (jeonseValues.size < properties.minSampleCount) {
            warnings += "순수 전세 실거래 표본이 ${properties.minSampleCount}건 미만이라 전세 참고 금액을 확정하지 않았습니다."
        }

        val latestDealDate = (saleSamples + rentSamples).mapNotNull { it.dealDate }.maxOrNull()
        val dealYmdFrom = dealMonths.last()
        val dealYmdTo = dealMonths.first()
        return MarketPriceProviderResult(
            estimatedMarketValue = estimatedMarketValue,
            estimatedJeonseValue = estimatedJeonseValue,
            sourceLabel = "국토교통부 실거래가(${endpointPair.label}, $dealYmdFrom~$dealYmdTo, $lawdCode)",
            referenceDate = latestDealDate ?: command.contractPlannedDate,
            sourceKind = MarketPriceSourceKind.MLIT_REAL_TRANSACTION,
            sampleCount = marketValues.size,
            marketSampleCount = marketValues.size,
            jeonseSampleCount = jeonseValues.size,
            lawdCode = lawdCode,
            dealYmdFrom = dealYmdFrom,
            dealYmdTo = dealYmdTo,
            warnings = warnings.distinct(),
        )
    }

    private fun unavailableResult(
        command: MarketPriceLookupCommand,
        label: String = "공공 실거래가",
        warnings: List<String>,
    ): MarketPriceProviderResult {
        return MarketPriceProviderResult(
            estimatedMarketValue = null,
            estimatedJeonseValue = null,
            sourceLabel = label,
            referenceDate = command.contractPlannedDate,
            sourceKind = MarketPriceSourceKind.MLIT_REAL_TRANSACTION,
            sampleCount = 0,
            marketSampleCount = 0,
            jeonseSampleCount = 0,
            warnings = warnings,
        )
    }

    private fun dealMonths(command: MarketPriceLookupCommand): List<String> {
        val baseMonth = YearMonth.from(command.contractPlannedDate)
        val count = properties.historyMonths.coerceAtLeast(1)
        return (0 until count).map { offset -> baseMonth.minusMonths(offset).toString().replace("-", "") }
    }

    private fun List<MlitTransaction>.selectPreferredSamples(
        address: RoadAddressLookupResult,
        warnings: MutableList<String>,
    ): List<MlitTransaction> {
        val buildingName = address.buildingName?.normalizeName()?.takeIf { it.isNotBlank() } ?: return this
        val matched = filter { transaction ->
            val transactionName = transaction.buildingName?.normalizeName()?.takeIf { it.isNotBlank() } ?: return@filter false
            transactionName.contains(buildingName) || buildingName.contains(transactionName)
        }
        return if (matched.size >= properties.minSampleCount) {
            matched
        } else {
            warnings += "건물명 기준 실거래 표본이 부족해 시군구 전체 표본을 참고했습니다."
            this
        }
    }

    private fun List<Long>.median(): Long {
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2
        } else {
            sorted[middle]
        }
    }

    private fun String.normalizeName(): String {
        return lowercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
    }
}
