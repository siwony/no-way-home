package com.nowayhome.housecheck.application

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Component
class DocumentIntakeAiAuthorizationProvider(
    private val aiProperties: DocumentIntakeAiProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val objectMapper = jacksonObjectMapper()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(aiProperties.timeout)
        .build()
    @Volatile
    private var cachedOAuthToken: CachedOAuthToken? = null

    fun authorizationHeader(): String {
        return when (aiProperties.authMode) {
            DocumentIntakeAiAuthMode.API_KEY -> bearerHeader(
                token = aiProperties.apiKey,
                missingMessage = "문서 처리용 AI API key 설정이 없어 자동 추출을 완료할 수 없습니다. 수기 입력으로 계속해 주세요.",
            )

            DocumentIntakeAiAuthMode.ACCESS_TOKEN -> bearerHeader(
                token = aiProperties.accessToken,
                missingMessage = "문서 처리용 AI access token 설정이 없어 자동 추출을 완료할 수 없습니다. 수기 입력으로 계속해 주세요.",
            )

            DocumentIntakeAiAuthMode.OAUTH_CLIENT_CREDENTIALS -> fetchOAuthAuthorizationHeader()
        }
    }

    private fun bearerHeader(token: String?, missingMessage: String): String {
        val normalizedToken = token?.trim().orEmpty()
        if (normalizedToken.isBlank()) {
            throw unavailable(missingMessage)
        }
        return "Bearer $normalizedToken"
    }

    private fun fetchOAuthAuthorizationHeader(): String {
        val cachedToken = cachedOAuthToken
        if (cachedToken != null && cachedToken.expiresAt.isAfter(Instant.now(clock).plusSeconds(30))) {
            return cachedToken.authorizationHeader
        }

        val oauth = aiProperties.oauth
        val tokenUrl = oauth.tokenUrl?.trim().orEmpty()
        val clientId = oauth.clientId?.trim().orEmpty()
        val clientSecret = oauth.clientSecret?.trim().orEmpty()
        if (tokenUrl.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            throw unavailable("OAuth token endpoint, client ID, client secret 설정이 없어 자동 추출을 완료할 수 없습니다. 수기 입력으로 계속해 주세요.")
        }

        val tokenUri = runCatching { URI.create(tokenUrl) }
            .getOrElse { throw unavailable("OAuth token endpoint URL 형식이 올바르지 않습니다. 설정을 확인해 주세요.") }
        val request = buildOAuthTokenRequest(
            tokenUri = tokenUri,
            clientId = clientId,
            clientSecret = clientSecret,
            scope = oauth.scope?.trim().orEmpty(),
            clientAuthentication = oauth.clientAuthentication,
        )
        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        } catch (_: HttpTimeoutException) {
            throw unavailable("OAuth token 발급 응답 시간이 초과되었습니다. 설정을 확인해 주세요.")
        } catch (_: ConnectException) {
            throw unavailable("OAuth token endpoint에 연결하지 못했습니다. 설정을 확인해 주세요.")
        } catch (_: Exception) {
            throw unavailable("OAuth token 발급에 실패했습니다. 설정을 확인해 주세요.")
        }

        if (response.statusCode() !in 200..299) {
            throw unavailable("OAuth token endpoint가 인증 토큰을 발급하지 못했습니다. 설정을 확인해 주세요.")
        }

        return parseOAuthToken(response.body()).also { cachedOAuthToken = it }.authorizationHeader
    }

    private fun buildOAuthTokenRequest(
        tokenUri: URI,
        clientId: String,
        clientSecret: String,
        scope: String,
        clientAuthentication: DocumentIntakeAiOAuthClientAuthentication,
    ): HttpRequest {
        val formParams = mutableListOf(
            "grant_type" to "client_credentials",
        )
        if (scope.isNotBlank()) {
            formParams += "scope" to scope
        }
        if (clientAuthentication == DocumentIntakeAiOAuthClientAuthentication.BODY) {
            formParams += "client_id" to clientId
            formParams += "client_secret" to clientSecret
        }

        val builder = HttpRequest.newBuilder()
            .uri(tokenUri)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(aiProperties.timeout)
            .POST(HttpRequest.BodyPublishers.ofString(formEncode(formParams), StandardCharsets.UTF_8))
        if (clientAuthentication == DocumentIntakeAiOAuthClientAuthentication.BASIC) {
            val credentials = "$clientId:$clientSecret"
            val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
            builder.header("Authorization", "Basic $encoded")
        }
        return builder.build()
    }

    private fun parseOAuthToken(responseBody: String): CachedOAuthToken {
        val responseJson = try {
            objectMapper.readTree(responseBody)
        } catch (_: JsonProcessingException) {
            throw unavailable("OAuth token 응답을 해석하지 못했습니다.")
        }
        val accessToken = responseJson.path("access_token").asText("").trim()
        if (accessToken.isBlank()) {
            throw unavailable("OAuth token 응답에 access_token이 없습니다.")
        }
        val tokenType = responseJson.path("token_type").asText("Bearer").trim().ifBlank { "Bearer" }
        val expiresInSeconds = responseJson.path("expires_in").asLong(Duration.ofMinutes(5).seconds)
            .coerceAtLeast(60)
        return CachedOAuthToken(
            authorizationHeader = "$tokenType $accessToken",
            expiresAt = Instant.now(clock).plusSeconds(expiresInSeconds),
        )
    }

    private fun formEncode(params: List<Pair<String, String>>): String {
        return params.joinToString("&") { (key, value) ->
            "${urlEncode(key)}=${urlEncode(value)}"
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

    private fun unavailable(message: String): DocumentIntakeExtractionFailureException {
        return DocumentIntakeExtractionFailureException(
            code = "AI_PROVIDER_UNAVAILABLE",
            message = message,
        )
    }

    private data class CachedOAuthToken(
        val authorizationHeader: String,
        val expiresAt: Instant,
    )
}
