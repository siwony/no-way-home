package com.nowayhome.housecheck.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HouseCheckMarketPriceLookupIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun cleanDatabase() {
        requestLog.clear()
        jdbcTemplate.execute(
            """
            truncate table
                house_risk_reason,
                house_risk_report,
                market_price_snapshot,
                building_ledger_manual_finding,
                registry_manual_finding,
                house_check_document,
                house_check_request
            cascade
            """.trimIndent(),
        )
    }

    @Test
    fun looksUpPublicMarketPriceThroughXmlOnlyApisAndSavesSelectedResult() {
        val checkId = createHouseCheck()

        val lookup = postJson(
            path = "/api/house-checks/$checkId/market-price/lookup",
            ownerId = "owner-a",
            body = "",
        )

        assertEquals(200, lookup.statusCode())
        val lookupJson = parseJson(lookup.body())
        assertEquals(220_000_000L, lookupJson.get("estimatedMarketValue").longValue())
        assertEquals(120_000_000L, lookupJson.get("estimatedJeonseValue").longValue())
        assertEquals("MLIT_REAL_TRANSACTION", lookupJson.get("sourceKind").asText())
        assertEquals("AVAILABLE", lookupJson.get("confidence").asText())
        assertEquals("11440", lookupJson.get("lawdCode").asText())
        assertEquals("202604", lookupJson.get("dealYmdFrom").asText())
        assertEquals("202605", lookupJson.get("dealYmdTo").asText())

        assertTrue(requestLog.any { it.path == "/juso" && it.query.contains("resultType=xml") })
        assertTrue(requestLog.any { it.path == "/legal" && it.query.contains("type=xml") })
        assertTrue(requestLog.any { it.path.contains("RTMSDataSvcAptTrade") })
        assertTrue(requestLog.any { it.path.contains("RTMSDataSvcAptRent") })
        assertFalse(requestLog.any { it.query.contains("json", ignoreCase = true) })
        assertTrue(requestLog.all { it.accept.contains("xml", ignoreCase = true) })

        val save = postJson(
            path = "/api/house-checks/$checkId/market-price",
            ownerId = "owner-a",
            body =
                """
                {
                  "estimatedMarketValue": ${lookupJson.get("estimatedMarketValue").longValue()},
                  "estimatedJeonseValue": ${lookupJson.get("estimatedJeonseValue").longValue()},
                  "sourceLabel": ${objectMapper.writeValueAsString(lookupJson.get("sourceLabel").asText())},
                  "referenceDate": "${lookupJson.get("referenceDate").asText()}",
                  "sourceKind": "MLIT_REAL_TRANSACTION",
                  "sampleCount": ${lookupJson.get("sampleCount").intValue()},
                  "lawdCode": "${lookupJson.get("lawdCode").asText()}",
                  "dealYmdFrom": "${lookupJson.get("dealYmdFrom").asText()}",
                  "dealYmdTo": "${lookupJson.get("dealYmdTo").asText()}"
                }
                """.trimIndent(),
        )
        assertEquals(200, save.statusCode())

        val saved = jdbcTemplate.queryForMap("select * from market_price_snapshot where house_check_id = ?::uuid", checkId)
        assertEquals("MLIT_REAL_TRANSACTION", saved["source_kind"])
        assertEquals("11440", saved["lawd_cd"])
        assertEquals("202604", saved["deal_ymd_from"])
        assertEquals("202605", saved["deal_ymd_to"])
    }

    private fun createHouseCheck(): String {
        val response = postJson(
            path = "/api/house-checks",
            ownerId = "owner-a",
            body =
                """
                {
                  "addressRoad":"서울특별시 마포구 양화로 1 테스트아파트",
                  "addressLot":"합정동 100-1",
                  "contractType":"JEONSE",
                  "housingType":"APARTMENT",
                  "depositAmount":60000000,
                  "monthlyRentAmount":0,
                  "contractPlannedDate":"2026-05-24",
                  "occupancyPlannedDate":"2026-06-01",
                  "landlordName":"임대인"
                }
                """.trimIndent(),
        )
        assertEquals(200, response.statusCode())
        return parseJson(response.body()).get("checkId").asText()
    }

    private fun postJson(path: String, ownerId: String, body: String): HttpResponse<String> {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("X-User-Id", ownerId)
        if (body.isNotEmpty()) {
            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        }
        return httpClient.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun parseJson(content: String): JsonNode = objectMapper.readTree(content)

    companion object {
        private val marketApiServer: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
        private val requestLog = Collections.synchronizedList(mutableListOf<LoggedRequest>())
        private val marketApiBaseUrl: String

        init {
            marketApiServer.createContext("/") { exchange ->
                requestLog += LoggedRequest(
                    path = exchange.requestURI.path,
                    query = exchange.requestURI.rawQuery.orEmpty(),
                    accept = exchange.requestHeaders.getFirst("Accept").orEmpty(),
                )
                exchange.respond(responseBodyFor(exchange))
            }
            marketApiServer.start()
            marketApiBaseUrl = "http://localhost:${marketApiServer.address.port}"
        }

        @JvmStatic
        @AfterAll
        fun stopMarketApiServer() {
            marketApiServer.stop(0)
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                System.getenv("HOUSECHECK_TEST_DATASOURCE_URL") ?: "jdbc:postgresql://localhost:5432/no_way_home"
            }
            registry.add("spring.datasource.username") {
                System.getenv("HOUSECHECK_TEST_DATASOURCE_USERNAME") ?: "no_way_home"
            }
            registry.add("spring.datasource.password") {
                System.getenv("HOUSECHECK_TEST_DATASOURCE_PASSWORD") ?: "no_way_home"
            }
            registry.add("housecheck.security.encryption.secret") { "housecheck-test-secret" }
            registry.add("housecheck.market-price.provider") { "mlit" }
            registry.add("housecheck.market-price.molit-openapi-service-key") { "mock-key%2Bencoded" }
            registry.add("housecheck.market-price.juso-confirm-key") { "juso-key" }
            registry.add("housecheck.market-price.history-months") { "2" }
            registry.add("housecheck.market-price.min-sample-count") { "3" }
            registry.add("housecheck.market-price.juso-search-url") { "$marketApiBaseUrl/juso" }
            registry.add("housecheck.market-price.legal-region-code-url") { "$marketApiBaseUrl/legal" }
            registry.add("housecheck.market-price.mlit-base-url") { marketApiBaseUrl }
        }

        private fun responseBodyFor(exchange: HttpExchange): String {
            val path = exchange.requestURI.path
            return when {
                path == "/juso" -> jusoXml()
                path == "/legal" -> legalCodeXml()
                path.contains("AptTrade") -> saleXml()
                path.contains("AptRent") -> rentXml()
                else -> """<response><header><resultCode>000</resultCode></header><body><items/></body></response>"""
            }
        }

        private fun HttpExchange.respond(body: String) {
            val bytes = body.toByteArray(Charsets.UTF_8)
            responseHeaders.add("Content-Type", "application/xml; charset=utf-8")
            sendResponseHeaders(200, bytes.size.toLong())
            responseBody.use { it.write(bytes) }
        }

        private fun jusoXml(): String =
            """
            <results>
              <common><errorCode>0</errorCode></common>
              <juso>
                <roadAddr>서울특별시 마포구 양화로 1 테스트아파트</roadAddr>
                <jibunAddr>서울특별시 마포구 합정동 100-1</jibunAddr>
                <admCd>1144012000</admCd>
                <siNm>서울특별시</siNm>
                <sggNm>마포구</sggNm>
                <emdNm>합정동</emdNm>
                <bdNm>테스트아파트</bdNm>
              </juso>
            </results>
            """.trimIndent()

        private fun legalCodeXml(): String =
            """
            <StanReginCd>
              <row>
                <region_cd>1144000000</region_cd>
                <locatadd_nm>서울특별시 마포구</locatadd_nm>
                <umd_cd>000</umd_cd>
                <ri_cd>00</ri_cd>
              </row>
            </StanReginCd>
            """.trimIndent()

        private fun saleXml(): String =
            """
            <response>
              <header><resultCode>000</resultCode><resultMsg>OK</resultMsg></header>
              <body><items>
                <item><aptNm>테스트아파트</aptNm><dealAmount>20,000</dealAmount><dealYear>2026</dealYear><dealMonth>5</dealMonth><dealDay>1</dealDay></item>
                <item><aptNm>테스트아파트</aptNm><dealAmount>22,000</dealAmount><dealYear>2026</dealYear><dealMonth>5</dealMonth><dealDay>2</dealDay></item>
                <item><aptNm>테스트아파트</aptNm><dealAmount>24,000</dealAmount><dealYear>2026</dealYear><dealMonth>5</dealMonth><dealDay>3</dealDay></item>
              </items></body>
            </response>
            """.trimIndent()

        private fun rentXml(): String =
            """
            <response>
              <header><resultCode>000</resultCode><resultMsg>OK</resultMsg></header>
              <body><items>
                <item><aptNm>테스트아파트</aptNm><deposit>10,000</deposit><monthlyRent>0</monthlyRent><dealYear>2026</dealYear><dealMonth>5</dealMonth><dealDay>4</dealDay></item>
                <item><aptNm>테스트아파트</aptNm><deposit>12,000</deposit><monthlyRent>0</monthlyRent><dealYear>2026</dealYear><dealMonth>5</dealMonth><dealDay>5</dealDay></item>
                <item><aptNm>테스트아파트</aptNm><deposit>14,000</deposit><monthlyRent>0</monthlyRent><dealYear>2026</dealYear><dealMonth>5</dealMonth><dealDay>6</dealDay></item>
              </items></body>
            </response>
            """.trimIndent()
    }
}

data class LoggedRequest(
    val path: String,
    val query: String,
    val accept: String,
)
