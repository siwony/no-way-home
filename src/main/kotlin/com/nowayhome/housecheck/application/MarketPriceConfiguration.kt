package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.analysis.MarketPriceProvider
import com.nowayhome.housecheck.domain.analysis.NoOpMarketPriceProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MarketPriceProperties::class)
class MarketPriceConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun safeXmlDocumentParser(): SafeXmlDocumentParser = SafeXmlDocumentParser()

    @Bean
    @ConditionalOnMissingBean(MarketPriceProvider::class)
    fun noOpMarketPriceProvider(): MarketPriceProvider = NoOpMarketPriceProvider()
}
