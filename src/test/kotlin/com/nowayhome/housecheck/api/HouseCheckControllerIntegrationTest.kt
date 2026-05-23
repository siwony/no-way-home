package com.nowayhome.housecheck.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Comparator
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HouseCheckControllerIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun cleanDatabase() {
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
        clearStorageRoot()
    }

    @Test
    fun completesPhaseOneFlowAndReturnsReportAndChecklist() {
        val checkId = createHouseCheck(ownerId = "owner-a")

        val registryUpload = uploadDocument(checkId, "owner-a", "registry-file")
        assertEquals(200, registryUpload.statusCode())
        val buildingUpload = uploadDocument(checkId, "owner-a", "building-ledger-file")
        assertEquals(200, buildingUpload.statusCode())

        val registryFindings = putJson(
            path = "/api/house-checks/$checkId/registry-findings",
            ownerId = "owner-a",
            body =
                """
                {
                  "currentOwnerName":"임대인",
                  "ownerMatchesLandlord":true,
                  "hasTrustRegistration":false,
                  "hasSeizure":false,
                  "hasProvisionalSeizure":false,
                  "hasProvisionalDisposition":false,
                  "hasAuctionProceeding":false,
                  "hasLeaseRegistration":false,
                  "hasMortgage":true,
                  "seniorDebtAmount":10000000
                }
                """.trimIndent(),
        )
        assertEquals(200, registryFindings.statusCode())

        val buildingFindings = putJson(
            path = "/api/house-checks/$checkId/building-ledger-findings",
            ownerId = "owner-a",
            body =
                """
                {
                  "usage":"공동주택",
                  "isResidentialUseConfirmed":true,
                  "isViolationBuilding":false,
                  "isUnitConfirmed":true,
                  "isContractAreaConsistent":true,
                  "approvalDate":"2020-01-01",
                  "housingTypeObserved":"APARTMENT"
                }
                """.trimIndent(),
        )
        assertEquals(200, buildingFindings.statusCode())

        val marketPrice = postJson(
            path = "/api/house-checks/$checkId/market-price",
            ownerId = "owner-a",
            body =
                """
                {
                  "estimatedMarketValue":200000000,
                  "sourceLabel":"수동 시세 입력",
                  "referenceDate":"2026-05-24"
                }
                """.trimIndent(),
        )
        assertEquals(200, marketPrice.statusCode())

        val analyze = postJson(
            path = "/api/house-checks/$checkId/analyze",
            ownerId = "owner-a",
            body = "",
        )
        assertEquals(200, analyze.statusCode())
        assertEquals("COMPLETED", parseJson(analyze.body()).get("analysisStatus").asText())

        val report = get("/api/house-checks/$checkId/report", "owner-a")
        assertEquals(200, report.statusCode())
        val reportJson = parseJson(report.body())
        assertEquals("CAUTION", reportJson.get("riskLevel").asText())
        assertEquals(0, reportJson.at("/depositRisk/jeonseRatio/value").decimalValue().compareTo(BigDecimal("30.00")))
        assertEquals(0, reportJson.at("/depositRisk/totalExposureRatio/value").decimalValue().compareTo(BigDecimal("35.00")))
        assertEquals("USER_ENTERED", reportJson.at("/registry/currentOwnerName/sourceType").asText())

        val checklist = get("/api/house-checks/$checkId/checklist", "owner-a")
        assertEquals(200, checklist.statusCode())
        val checklistJson = parseJson(checklist.body())
        assertEquals(3, checklistJson.get("sections").size())
        assertEquals("BEFORE_CONTRACT", checklistJson.at("/sections/0/stage").asText())
    }

    @Test
    fun rejectsNegativeDepositDuringRequestCreation() {
        val response = postJson(
            path = "/api/house-checks",
            ownerId = "owner-a",
            body =
                """
                {
                  "addressRoad":"서울시 마포구",
                  "contractType":"JEONSE",
                  "housingType":"APARTMENT",
                  "depositAmount":-1,
                  "monthlyRentAmount":0,
                  "contractPlannedDate":"2026-05-24",
                  "occupancyPlannedDate":"2026-06-01",
                  "landlordName":"임대인"
                }
                """.trimIndent(),
        )

        assertEquals(400, response.statusCode())
        assertEquals("INVALID_DEPOSIT_AMOUNT", parseJson(response.body()).get("code").asText())
    }

    @Test
    fun deniesChecklistAccessForDifferentOwner() {
        val checkId = createHouseCheck(ownerId = "owner-a")

        val response = get("/api/house-checks/$checkId/checklist", "owner-b")

        assertEquals(403, response.statusCode())
        assertEquals("ACCESS_DENIED", parseJson(response.body()).get("code").asText())
    }

    @Test
    fun supportsJeonseOnlyMarketInputAndEncryptsSensitiveDataAtRest() {
        val landlordName = "qa-landlord-name"
        val fileBody = "%PDF-1.4 qa-plain-pdf"
        val checkId = createHouseCheck(ownerId = "owner-a", landlordName = landlordName)

        val registryUpload = uploadDocument(
            checkId = checkId,
            ownerId = "owner-a",
            suffix = "registry-file",
            fileBody = fileBody,
        )
        assertEquals(200, registryUpload.statusCode())

        val marketPrice = postJson(
            path = "/api/house-checks/$checkId/market-price",
            ownerId = "owner-a",
            body =
                """
                {
                  "estimatedJeonseValue":65000000,
                  "sourceLabel":"전세 참고 시세",
                  "referenceDate":"2026-05-24"
                }
                """.trimIndent(),
        )
        assertEquals(200, marketPrice.statusCode())

        val analyze = postJson(
            path = "/api/house-checks/$checkId/analyze",
            ownerId = "owner-a",
            body = "",
        )
        assertEquals(200, analyze.statusCode())

        val report = get("/api/house-checks/$checkId/report", "owner-a")
        assertEquals(200, report.statusCode())
        val reportJson = parseJson(report.body())
        assertEquals(65_000_000L, reportJson.at("/depositRisk/estimatedJeonseValue/value").longValue())
        assertEquals("USER_ENTERED", reportJson.at("/depositRisk/estimatedJeonseValue/sourceType").asText())
        assertTrue(
            reportJson.at("/depositRisk/estimatedMarketValue/value").isMissingNode ||
                reportJson.at("/depositRisk/estimatedMarketValue/value").isNull,
        )
        assertEquals("NOT_AVAILABLE", reportJson.at("/depositRisk/jeonseRatio/calculationStatus").asText())
        assertTrue(reportJson.at("/depositRisk/jeonseRatio/note").asText().contains("전세가만 저장"))
        assertTrue(reportJson.get("coreReasons").none { it.get("code").asText() == "MARKET_PRICE_REQUIRED" })
        assertTrue(reportJson.get("coreReasons").any { it.get("code").asText() == "MARKET_VALUE_REQUIRED_FOR_RATIO" })

        val rawLandlordColumn = jdbcTemplate.queryForObject(
            "select landlord_name from house_check_request where id = ?",
            String::class.java,
            UUID.fromString(checkId),
        ) ?: error("Expected encrypted landlord_name column")
        assertTrue(rawLandlordColumn != landlordName)
        assertTrue(!rawLandlordColumn.contains(landlordName))

        val storageKey = jdbcTemplate.queryForObject(
            "select storage_key from house_check_document where house_check_id = ? and document_type = 'REGISTRY'",
            String::class.java,
            UUID.fromString(checkId),
        ) ?: error("Expected stored registry document")
        val storedBytes = Files.readAllBytes(storageRoot.resolve(storageKey))
        val originalBytes = fileBody.toByteArray(StandardCharsets.UTF_8)
        assertTrue(!storedBytes.contentEquals(originalBytes))
        assertTrue(!containsBytes(storedBytes, originalBytes))
        assertTrue(!containsBytes(storedBytes, "%PDF-1.4".toByteArray(StandardCharsets.UTF_8)))
    }

    private fun createHouseCheck(ownerId: String, landlordName: String = "임대인"): String {
        val response = postJson(
            path = "/api/house-checks",
            ownerId = ownerId,
            body =
                """
                {
                  "addressRoad":"서울시 마포구 양화로 1",
                  "addressLot":"합정동 100-1",
                  "contractType":"JEONSE",
                  "housingType":"APARTMENT",
                  "depositAmount":60000000,
                  "monthlyRentAmount":0,
                  "contractPlannedDate":"2026-05-24",
                  "occupancyPlannedDate":"2026-06-01",
                  "landlordName":"$landlordName"
                }
                """.trimIndent(),
        )
        assertEquals(200, response.statusCode())
        return parseJson(response.body()).get("checkId").asText()
    }

    private fun postJson(path: String, ownerId: String, body: String): HttpResponse<String> {
        val builder = HttpRequest.newBuilder()
            .uri(uri(path))
            .header("X-User-Id", ownerId)
        if (body.isNotEmpty()) {
            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        }
        val request = builder.POST(HttpRequest.BodyPublishers.ofString(body)).build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun putJson(path: String, ownerId: String, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(uri(path))
            .header("X-User-Id", ownerId)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun get(path: String, ownerId: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(uri(path))
            .header("X-User-Id", ownerId)
            .GET()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun uploadDocument(
        checkId: String,
        ownerId: String,
        suffix: String,
        fileBody: String = "%PDF-1.4 sample",
    ): HttpResponse<String> {
        val boundary = "----NoWayHomeBoundary"
        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"$suffix.pdf\"\r\n")
            append("Content-Type: application/pdf\r\n\r\n")
            append(fileBody)
            append("\r\n")
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"issuedDate\"\r\n\r\n")
            append("2026-05-20\r\n")
            append("--$boundary--\r\n")
        }
        val request = HttpRequest.newBuilder()
            .uri(uri("/api/house-checks/$checkId/$suffix"))
            .header("X-User-Id", ownerId)
            .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray(StandardCharsets.UTF_8)))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun uri(path: String): URI = URI.create("http://localhost:$port$path")

    private fun parseJson(content: String): JsonNode = objectMapper.readTree(content)

    private fun clearStorageRoot() {
        Files.walk(storageRoot).use { paths ->
            paths.sorted(Comparator.reverseOrder())
                .filter { it != storageRoot }
                .forEach { Files.deleteIfExists(it) }
        }
        Files.createDirectories(storageRoot)
    }

    private fun containsBytes(source: ByteArray, target: ByteArray): Boolean {
        if (target.isEmpty() || source.size < target.size) {
            return false
        }
        return source.indices.any { start ->
            if (start + target.size > source.size) {
                false
            } else {
                source.copyOfRange(start, start + target.size).contentEquals(target)
            }
        }
    }

    companion object {
        private val storageRoot = Files.createTempDirectory("housecheck-storage-test")

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
            registry.add("housecheck.storage.root") { storageRoot.toString() }
            registry.add("housecheck.security.encryption.secret") { "housecheck-test-secret" }
        }
    }
}
