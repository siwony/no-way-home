package com.nowayhome.housecheck.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Duration
import java.util.Comparator
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentIntakeControllerOpenAiUnavailableIntegrationTest {
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
    fun returnsFailedDocumentStateWhenDefaultOpenAiProviderHasNoApiKey() {
        val sessionId = createDocumentIntakeSession("owner-a")

        val response = uploadDocument(
            sessionId = sessionId,
            ownerId = "owner-a",
            documentType = "registry",
            fileName = "registry.pdf",
            contentType = "application/pdf",
            fileBody = createPdfFixture(listOf("owner landlord", "registry address")),
        )

        assertEquals(200, response.statusCode())
        val json = objectMapper.readTree(response.body())
        val document = json.get("documents").first()
        assertEquals("FAILED", document.get("processingStatus").asText())
        assertEquals("AI_PROVIDER_UNAVAILABLE", document.get("failure").get("code").asText())
        assertTrue(document.get("failure").get("message").asText().contains("AI"))
        assertEquals(0, json.get("fields").size())
        assertEquals(0, json.get("warnings").size())
    }

    private fun createDocumentIntakeSession(ownerId: String): String {
        val request = HttpRequest.newBuilder()
            .uri(uri("/api/document-intakes"))
            .header("X-User-Id", ownerId)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode())
        return objectMapper.readTree(response.body()).get("sessionId").asText()
    }

    private fun uploadDocument(
        sessionId: String,
        ownerId: String,
        documentType: String,
        fileName: String,
        contentType: String,
        fileBody: ByteArray,
    ): HttpResponse<String> {
        val boundary = "----NoWayHomeDocumentIntakeBoundaryOpenAiUnavailable"
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\n".toByteArray(StandardCharsets.UTF_8))
            write("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n".toByteArray(StandardCharsets.UTF_8))
            write("Content-Type: $contentType\r\n\r\n".toByteArray(StandardCharsets.UTF_8))
            write(fileBody)
            write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
        }.toByteArray()
        val request = HttpRequest.newBuilder()
            .uri(uri("/api/document-intakes/$sessionId/documents/$documentType"))
            .header("X-User-Id", ownerId)
            .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun uri(path: String): URI = URI.create("http://localhost:$port$path")

    private fun clearStorageRoot() {
        Files.walk(storageRoot).use { paths ->
            paths.sorted(Comparator.reverseOrder())
                .filter { it != storageRoot }
                .forEach { Files.deleteIfExists(it) }
        }
        Files.createDirectories(storageRoot)
    }

    private fun createPdfFixture(pageTexts: List<String>): ByteArray {
        val output = ByteArrayOutputStream()
        PDDocument().use { document ->
            val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
            pageTexts.forEach { text ->
                val page = PDPage()
                document.addPage(page)
                PDPageContentStream(document, page).use { content ->
                    content.beginText()
                    content.setFont(font, 12f)
                    content.newLineAtOffset(72f, 720f)
                    content.showText(text)
                    content.endText()
                }
            }
            document.save(output)
        }
        return output.toByteArray()
    }

    companion object {
        private val storageRoot = Files.createTempDirectory("document-intake-openai-unavailable-test")

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
            registry.add("housecheck.document-intake.ai.auth-mode") { "api-key" }
            registry.add("housecheck.document-intake.ai.api-key") { "" }
            registry.add("housecheck.document-intake.ai.model") { "gpt-4.1-mini" }
            registry.add("housecheck.document-intake.ai.base-url") { "http://127.0.0.1:1/v1" }
            registry.add("housecheck.document-intake.ai.timeout") { Duration.ofSeconds(1) }
        }
    }
}
