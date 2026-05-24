package com.nowayhome.housecheck.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "housecheck.market-price")
data class MarketPriceProperties(
    val provider: MarketPriceProviderMode = MarketPriceProviderMode.NONE,
    val molitOpenapiServiceKey: String? = null,
    val jusoConfirmKey: String? = null,
    val historyMonths: Long = 12,
    val minSampleCount: Int = 3,
    val timeout: Duration = Duration.ofSeconds(10),
    val jusoSearchUrl: String = "https://business.juso.go.kr/addrlink/addrLinkApi.do",
    val legalRegionCodeUrl: String = "https://apis.data.go.kr/1741000/StanReginCd/getStanReginCdList",
    val mlitBaseUrl: String = "https://apis.data.go.kr/1613000",
)

enum class MarketPriceProviderMode {
    NONE,
    MLIT,
}
