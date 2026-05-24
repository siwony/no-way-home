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
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import java.util.Comparator
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentIntakeControllerIntegrationTest {
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
                document_intake_warning,
                document_intake_extracted_field,
                document_intake_document,
                document_intake_session,
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
    fun createsDocumentIntakeSessionBeforeHouseCheckCreation() {
        val response = postNoBody("/api/document-intakes", "owner-a")

        assertEquals(200, response.statusCode())
        val json = parseJson(response.body())
        assertTrue(json.get("sessionId").asText().isNotBlank())
        assertEquals(0, json.get("documents").size())
        assertEquals(0, json.get("fields").size())
        assertEquals(0, json.get("warnings").size())
    }

    @Test
    fun uploadsRegistryAndLeaseDocumentsExtractsFieldsAndEncryptsStoredBytes() {
        val sessionId = createDocumentIntakeSession("owner-a")

        val registryUpload = uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "registry",
            fileName = "registry.pdf",
            contentType = "application/pdf",
            fileBody = "%PDF-1.4 registry fixture",
        )
        assertEquals(200, registryUpload.statusCode())

        val leaseUpload = uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "lease-contract",
            fileName = "lease-contract.png",
            contentType = "image/png",
            fileBody = "png lease fixture",
        )
        assertEquals(200, leaseUpload.statusCode())

        val session = get("/api/document-intakes/$sessionId", "owner-a")
        assertEquals(200, session.statusCode())
        val sessionJson = parseJson(session.body())
        assertEquals(2, sessionJson.get("documents").size())
        assertEquals("REVIEW_REQUIRED", documentStatus(sessionJson, "REGISTRY"))
        assertEquals("REVIEW_REQUIRED", documentStatus(sessionJson, "LEASE_CONTRACT"))

        val ownerField = findField(sessionJson, "REGISTRY_OWNER_NAME")
        assertEquals("REGISTRY", ownerField.get("sourceDocument").asText())
        assertEquals(2, ownerField.get("sourcePage").asInt())
        assertEquals("REVIEW_REQUIRED", ownerField.get("reviewStatus").asText())
        assertTrue(ownerField.get("sourceText").asText().contains("소유자"))
        assertTrue(ownerField.get("confidence").decimalValue().toDouble() > 0.8)

        val storageKey = jdbcTemplate.queryForObject(
            "select storage_key from document_intake_document where session_id = ? and document_type = 'REGISTRY'",
            String::class.java,
            UUID.fromString(sessionId),
        ) ?: error("Expected stored registry intake document")
        val storedBytes = Files.readAllBytes(storageRoot.resolve(storageKey))
        val originalBytes = "%PDF-1.4 registry fixture".toByteArray(StandardCharsets.UTF_8)
        assertFalse(storedBytes.contentEquals(originalBytes))
        assertFalse(containsBytes(storedBytes, "%PDF-1.4".toByteArray(StandardCharsets.UTF_8)))
    }

    @Test
    fun supportsApproveEditExcludeAndBuildsApplicationPayload() {
        val sessionId = createDocumentIntakeSession("owner-a")
        uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "registry",
            fileName = "registry.pdf",
            contentType = "application/pdf",
            fileBody = "%PDF-1.4 registry-address-mismatch owner-mismatch",
        )
        uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "lease-contract",
            fileName = "lease.webp",
            contentType = "image/webp",
            fileBody = "lease-address-mismatch owner-mismatch deposit-mismatch",
        )

        val initialSession = parseJson(get("/api/document-intakes/$sessionId", "owner-a").body())
        assertTrue(hasWarning(initialSession, "ADDRESS_MISMATCH"))
        assertTrue(hasWarning(initialSession, "LANDLORD_OWNER_MISMATCH"))
        assertTrue(hasWarning(initialSession, "DEPOSIT_MISMATCH"))

        initialSession.get("fields").forEach { field ->
            val fieldKey = field.get("fieldKey").asText()
            val requestBody = when (fieldKey) {
                "LEASE_DEPOSIT_AMOUNT" -> """{"action":"EDIT","editedValue":"62000000"}"""
                "LEASE_MONTHLY_RENT_AMOUNT", "LEASE_SPECIAL_TERMS" -> """{"action":"EXCLUDE"}"""
                else -> """{"action":"APPROVE"}"""
            }
            val reviewResponse = putJson(
                path = "/api/document-intakes/$sessionId/fields/$fieldKey",
                ownerId = "owner-a",
                body = requestBody,
            )
            assertEquals(200, reviewResponse.statusCode())
        }

        val reviewedSession = parseJson(get("/api/document-intakes/$sessionId", "owner-a").body())
        assertEquals("APPROVED", documentStatus(reviewedSession, "REGISTRY"))
        assertEquals("APPROVED", documentStatus(reviewedSession, "LEASE_CONTRACT"))
        assertEquals("EDITED", findField(reviewedSession, "LEASE_DEPOSIT_AMOUNT").get("reviewStatus").asText())
        assertEquals("62000000", findField(reviewedSession, "LEASE_DEPOSIT_AMOUNT").get("value").asText())
        assertEquals("EXCLUDED", findField(reviewedSession, "LEASE_MONTHLY_RENT_AMOUNT").get("reviewStatus").asText())

        val payloadResponse = get("/api/document-intakes/$sessionId/application-payload", "owner-a")
        assertEquals(200, payloadResponse.statusCode())
        val payloadJson = parseJson(payloadResponse.body())
        assertEquals(16, payloadJson.get("approvedFieldCount").asInt())
        assertEquals("서울시 강서구 공항대로 9", payloadJson.at("/contractForm/addressRoad").asText())
        assertEquals("합정동 100-1", payloadJson.at("/contractForm/addressLot").asText())
        assertEquals("JEONSE", payloadJson.at("/contractForm/contractType").asText())
        assertEquals(62000000L, payloadJson.at("/contractForm/depositAmount").asLong())
        assertTrue(payloadJson.at("/contractForm/monthlyRentAmount").isNull)
        assertEquals(LocalDate.of(2026, 5, 24).toString(), payloadJson.at("/contractForm/contractPlannedDate").asText())
        assertEquals("다른 임대인", payloadJson.at("/contractForm/landlordName").asText())
        assertEquals("다른 소유자", payloadJson.at("/registryFindingsForm/currentOwnerName").asText())
        assertEquals(false, payloadJson.at("/registryFindingsForm/ownerMatchesLandlord").asBoolean())
        assertEquals(true, payloadJson.at("/registryFindingsForm/hasMortgage").asBoolean())
        assertEquals(12000000L, payloadJson.at("/registryFindingsForm/seniorDebtAmount").asLong())
    }

    @Test
    fun deniesAccessForDifferentOwner() {
        val sessionId = createDocumentIntakeSession("owner-a")
        uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "registry",
            fileName = "registry.pdf",
            contentType = "application/pdf",
            fileBody = "%PDF-1.4 registry fixture",
        )

        val response = get("/api/document-intakes/$sessionId", "owner-b")

        assertEquals(403, response.statusCode())
        assertEquals("ACCESS_DENIED", parseJson(response.body()).get("code").asText())
    }

    @Test
    fun rejectsInvalidFileType() {
        val sessionId = createDocumentIntakeSession("owner-a")

        val response = uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "registry",
            fileName = "registry.png",
            contentType = "image/png",
            fileBody = "not a pdf",
        )

        assertEquals(400, response.statusCode())
        assertEquals("DOCUMENT_INTAKE_INVALID_FILE_TYPE", parseJson(response.body()).get("code").asText())
    }

    @Test
    fun deletesDocumentAndMarksItDeleted() {
        val sessionId = createDocumentIntakeSession("owner-a")
        uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "registry",
            fileName = "registry.pdf",
            contentType = "application/pdf",
            fileBody = "%PDF-1.4 registry fixture",
        )
        val storageKey = jdbcTemplate.queryForObject(
            "select storage_key from document_intake_document where session_id = ? and document_type = 'REGISTRY'",
            String::class.java,
            UUID.fromString(sessionId),
        ) ?: error("Expected stored registry intake document before delete")

        val deleteResponse = delete("/api/document-intakes/$sessionId/documents/registry", "owner-a")

        assertEquals(200, deleteResponse.statusCode())
        val sessionJson = parseJson(deleteResponse.body())
        assertEquals("DELETED", documentStatus(sessionJson, "REGISTRY"))
        assertTrue(findFieldCount(sessionJson, "REGISTRY_") == 0)
        assertFalse(Files.exists(storageRoot.resolve(storageKey)))
    }

    @Test
    fun doesNotMutateExistingHouseCheckBeforeFrontendAppliesPayload() {
        val checkId = createHouseCheck(ownerId = "owner-a", landlordName = "기존 임대인")
        val sessionId = createDocumentIntakeSession("owner-a")

        uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "registry",
            fileName = "registry.pdf",
            contentType = "application/pdf",
            fileBody = "%PDF-1.4 owner-mismatch",
        )
        uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "lease-contract",
            fileName = "lease.pdf",
            contentType = "application/pdf",
            fileBody = "deposit-mismatch owner-mismatch",
        )

        approveEveryField(sessionId, ownerId = "owner-a")

        val payloadResponse = get("/api/document-intakes/$sessionId/application-payload", "owner-a")
        assertEquals(200, payloadResponse.statusCode())

        val houseCheckRow = jdbcTemplate.queryForMap(
            "select address_road, deposit_amount, monthly_rent_amount, landlord_name from house_check_request where id = ?",
            UUID.fromString(checkId),
        )
        assertEquals("서울시 마포구 양화로 1", houseCheckRow["address_road"])
        assertEquals(60000000L, (houseCheckRow["deposit_amount"] as Number).toLong())
        assertEquals(0L, (houseCheckRow["monthly_rent_amount"] as Number).toLong())
        assertTrue((houseCheckRow["landlord_name"] as String).startsWith("enc:"))

        val registryFindingCount = jdbcTemplate.queryForObject(
            "select count(*) from registry_manual_finding where house_check_id = ?",
            Long::class.java,
            UUID.fromString(checkId),
        ) ?: 0L
        assertEquals(0L, registryFindingCount)
    }

    private fun approveEveryField(sessionId: String, ownerId: String) {
        val sessionJson = parseJson(get("/api/document-intakes/$sessionId", ownerId).body())
        sessionJson.get("fields").forEach { field ->
            val fieldKey = field.get("fieldKey").asText()
            val response = putJson(
                path = "/api/document-intakes/$sessionId/fields/$fieldKey",
                ownerId = ownerId,
                body = """{"action":"APPROVE"}""",
            )
            assertEquals(200, response.statusCode())
        }
    }

    private fun createDocumentIntakeSession(ownerId: String): String {
        val response = postNoBody("/api/document-intakes", ownerId)
        assertEquals(200, response.statusCode())
        return parseJson(response.body()).get("sessionId").asText()
    }

    private fun createHouseCheck(ownerId: String, landlordName: String): String {
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

    private fun postNoBody(path: String, ownerId: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(uri(path))
            .header("X-User-Id", ownerId)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun postJson(path: String, ownerId: String, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(uri(path))
            .header("X-User-Id", ownerId)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
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

    private fun delete(path: String, ownerId: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(uri(path))
            .header("X-User-Id", ownerId)
            .DELETE()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun uploadDocument(
        sessionId: String,
        ownerId: String,
        documentType: String,
        fileName: String,
        contentType: String,
        fileBody: String,
    ): HttpResponse<String> {
        val boundary = "----NoWayHomeDocumentIntakeBoundary"
        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
            append("Content-Type: $contentType\r\n\r\n")
            append(fileBody)
            append("\r\n")
            append("--$boundary--\r\n")
        }
        val request = HttpRequest.newBuilder()
            .uri(uri("/api/document-intakes/$sessionId/documents/$documentType"))
            .header("X-User-Id", ownerId)
            .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray(StandardCharsets.UTF_8)))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun documentStatus(json: JsonNode, documentType: String): String {
        return json.get("documents").first { it.get("documentType").asText() == documentType }
            .get("processingStatus").asText()
    }

    private fun findField(json: JsonNode, fieldKey: String): JsonNode {
        return json.get("fields").first { it.get("fieldKey").asText() == fieldKey }
    }

    private fun hasWarning(json: JsonNode, warningType: String): Boolean {
        return json.get("warnings").any { it.get("type").asText() == warningType }
    }

    private fun findFieldCount(json: JsonNode, fieldKeyPrefix: String): Int {
        return json.get("fields").count { it.get("fieldKey").asText().startsWith(fieldKeyPrefix) }
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
        private val storageRoot = Files.createTempDirectory("document-intake-storage-test")

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
