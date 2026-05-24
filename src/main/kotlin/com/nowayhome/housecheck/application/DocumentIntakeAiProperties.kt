package com.nowayhome.housecheck.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "housecheck.document-intake.ai")
data class DocumentIntakeAiProperties(
    val apiKey: String? = null,
    val model: String = "gpt-4.1-mini",
    val baseUrl: String = "https://api.openai.com/v1",
    val timeout: Duration = Duration.ofSeconds(20),
)
