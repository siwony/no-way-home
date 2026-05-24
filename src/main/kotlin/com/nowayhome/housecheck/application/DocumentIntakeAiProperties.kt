package com.nowayhome.housecheck.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "housecheck.document-intake.ai")
data class DocumentIntakeAiProperties(
    val authMode: DocumentIntakeAiAuthMode = DocumentIntakeAiAuthMode.API_KEY,
    val apiKey: String? = null,
    val accessToken: String? = null,
    val oauth: DocumentIntakeAiOAuthProperties = DocumentIntakeAiOAuthProperties(),
    val model: String = "gpt-4.1-mini",
    val baseUrl: String = "https://api.openai.com/v1",
    val timeout: Duration = Duration.ofSeconds(20),
)

enum class DocumentIntakeAiAuthMode {
    API_KEY,
    ACCESS_TOKEN,
    OAUTH_CLIENT_CREDENTIALS,
}

enum class DocumentIntakeAiOAuthClientAuthentication {
    BODY,
    BASIC,
}

data class DocumentIntakeAiOAuthProperties(
    val tokenUrl: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val scope: String? = null,
    val clientAuthentication: DocumentIntakeAiOAuthClientAuthentication = DocumentIntakeAiOAuthClientAuthentication.BODY,
)
