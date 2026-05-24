package com.nowayhome.housecheck.application

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentIntakeAiAuthorizationProviderTest {
    private var server: HttpServer? = null
    private var receivedRequestBody: String? = null
    private var receivedAuthorizationHeader: String? = null

    @AfterEach
    fun stopServer() {
        server?.stop(0)
    }

    @Test
    fun returnsApiKeyBearerHeader() {
        val provider = DocumentIntakeAiAuthorizationProvider(
            aiProperties = DocumentIntakeAiProperties(
                authMode = DocumentIntakeAiAuthMode.API_KEY,
                apiKey = "test-api-key",
            ),
        )

        assertEquals("Bearer test-api-key", provider.authorizationHeader())
    }

    @Test
    fun returnsAccessTokenBearerHeader() {
        val provider = DocumentIntakeAiAuthorizationProvider(
            aiProperties = DocumentIntakeAiProperties(
                authMode = DocumentIntakeAiAuthMode.ACCESS_TOKEN,
                accessToken = "oauth-access-token",
            ),
        )

        assertEquals("Bearer oauth-access-token", provider.authorizationHeader())
    }

    @Test
    fun fetchesOAuthClientCredentialsTokenWithBodyAuthentication() {
        startTokenServer(
            """
            {
              "access_token": "issued-token",
              "token_type": "Bearer",
              "expires_in": 3600
            }
            """.trimIndent(),
        )
        val provider = DocumentIntakeAiAuthorizationProvider(
            aiProperties = oauthProperties(
                clientAuthentication = DocumentIntakeAiOAuthClientAuthentication.BODY,
            ),
        )

        assertEquals("Bearer issued-token", provider.authorizationHeader())
        assertTrue(receivedRequestBody.orEmpty().contains("grant_type=client_credentials"))
        assertTrue(receivedRequestBody.orEmpty().contains("client_id=client-id"))
        assertTrue(receivedRequestBody.orEmpty().contains("client_secret=client-secret"))
        assertTrue(receivedRequestBody.orEmpty().contains("scope=documents.read"))
        assertEquals(null, receivedAuthorizationHeader)
    }

    @Test
    fun fetchesOAuthClientCredentialsTokenWithBasicAuthentication() {
        startTokenServer(
            """
            {
              "access_token": "issued-basic-token",
              "token_type": "Bearer",
              "expires_in": 3600
            }
            """.trimIndent(),
        )
        val provider = DocumentIntakeAiAuthorizationProvider(
            aiProperties = oauthProperties(
                clientAuthentication = DocumentIntakeAiOAuthClientAuthentication.BASIC,
            ),
        )

        assertEquals("Bearer issued-basic-token", provider.authorizationHeader())
        assertTrue(receivedAuthorizationHeader.orEmpty().startsWith("Basic "))
        assertFalse(receivedRequestBody.orEmpty().contains("client_secret=client-secret"))
    }

    @Test
    fun failsExplicitlyWhenOAuthConfigIsMissing() {
        val provider = DocumentIntakeAiAuthorizationProvider(
            aiProperties = DocumentIntakeAiProperties(
                authMode = DocumentIntakeAiAuthMode.OAUTH_CLIENT_CREDENTIALS,
            ),
        )

        val exception = assertFailsWith<DocumentIntakeExtractionFailureException> {
            provider.authorizationHeader()
        }

        assertEquals("AI_PROVIDER_UNAVAILABLE", exception.code)
    }

    private fun oauthProperties(
        clientAuthentication: DocumentIntakeAiOAuthClientAuthentication,
    ): DocumentIntakeAiProperties {
        return DocumentIntakeAiProperties(
            authMode = DocumentIntakeAiAuthMode.OAUTH_CLIENT_CREDENTIALS,
            oauth = DocumentIntakeAiOAuthProperties(
                tokenUrl = "http://127.0.0.1:${server?.address?.port}/oauth/token",
                clientId = "client-id",
                clientSecret = "client-secret",
                scope = "documents.read",
                clientAuthentication = clientAuthentication,
            ),
            timeout = Duration.ofSeconds(3),
        )
    }

    private fun startTokenServer(responseBody: String) {
        receivedRequestBody = null
        receivedAuthorizationHeader = null
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/oauth/token") { exchange ->
                receivedAuthorizationHeader = exchange.requestHeaders.getFirst("Authorization")
                receivedRequestBody = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                val bytes = responseBody.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
    }
}
