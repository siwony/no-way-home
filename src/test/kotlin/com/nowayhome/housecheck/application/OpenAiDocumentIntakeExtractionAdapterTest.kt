package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.sun.net.httpserver.HttpServer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OpenAiDocumentIntakeExtractionAdapterTest {
    private var server: HttpServer? = null
    private var receivedRequestBody: String? = null

    @AfterEach
    fun stopServer() {
        server?.stop(0)
    }

    @Test
    fun extractsValidatedFieldsFromOpenAiStructuredOutput() {
        startServer(
            """
            {
              "output_text": "{\"fields\":[{\"fieldKey\":\"REGISTRY_OWNER_NAME\",\"value\":\"임대인\",\"sourcePage\":1,\"sourceText\":\"소유자 임대인\",\"confidence\":0.93},{\"fieldKey\":\"REGISTRY_HAS_MORTGAGE\",\"value\":\"true\",\"sourcePage\":1,\"sourceText\":\"근저당권 설정\",\"confidence\":0.89}],\"warnings\":[]}"
            }
            """.trimIndent(),
        )
        val adapter = newAdapter(apiKey = "test-key")

        val result = adapter.extract(
            documentType = DocumentIntakeDocumentType.REGISTRY,
            originalFileName = "registry.pdf",
            contentType = "application/pdf",
            bytes = createPdfFixture(listOf("owner landlord mortgage")),
        )

        assertEquals(2, result.fields.size)
        assertEquals("REGISTRY_OWNER_NAME", result.fields[0].fieldKey.name)
        assertEquals("임대인", result.fields[0].value)
        assertEquals("REGISTRY_HAS_MORTGAGE", result.fields[1].fieldKey.name)
        assertEquals("true", result.fields[1].value)
        assertTrue(result.warnings.isEmpty())
        assertTrue(receivedRequestBody.orEmpty().contains("\"type\":\"input_file\""))
        assertTrue(receivedRequestBody.orEmpty().contains("\"file_data\":\"data:application/pdf;base64,"))
    }

    @Test
    fun extractsValidatedFieldsFromNestedOpenAiOutputContent() {
        startServer(
            """
            {
              "output": [
                {
                  "type": "message",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "{\"fields\":[{\"fieldKey\":\"REGISTRY_OWNER_NAME\",\"value\":\"홍길동\",\"sourcePage\":1,\"sourceText\":\"소유자 홍길동\",\"confidence\":0.91}],\"warnings\":[]}"
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )
        val adapter = newAdapter(apiKey = "test-key")

        val result = adapter.extract(
            documentType = DocumentIntakeDocumentType.REGISTRY,
            originalFileName = "registry.pdf",
            contentType = "application/pdf",
            bytes = createPdfFixture(listOf("owner hong")),
        )

        assertEquals(1, result.fields.size)
        assertEquals("REGISTRY_OWNER_NAME", result.fields[0].fieldKey.name)
        assertEquals("홍길동", result.fields[0].value)
    }

    @Test
    fun sendsBlankTextPdfAsInputFileForModelVisionExtraction() {
        startServer(
            """
            {
              "output_text": "{\"fields\":[{\"fieldKey\":\"REGISTRY_OWNER_NAME\",\"value\":\"김임대\",\"sourcePage\":1,\"sourceText\":\"소유자 김임대\",\"confidence\":0.9}],\"warnings\":[]}"
            }
            """.trimIndent(),
        )
        val adapter = newAdapter(apiKey = "test-key")

        val result = adapter.extract(
            documentType = DocumentIntakeDocumentType.REGISTRY,
            originalFileName = "scanned-registry.pdf",
            contentType = "application/pdf",
            bytes = createBlankPdfFixture(),
        )

        assertEquals("김임대", result.fields.single().value)
        assertTrue(receivedRequestBody.orEmpty().contains("locallyExtractedTextAvailable"))
        assertTrue(receivedRequestBody.orEmpty().contains("\"type\":\"input_file\""))
    }

    @Test
    fun rejectsStructuredOutputThatFailsFieldValidation() {
        startServer(
            """
            {
              "output_text": "{\"fields\":[{\"fieldKey\":\"UNKNOWN_FIELD\",\"value\":\"oops\",\"sourcePage\":1,\"sourceText\":\"근거\",\"confidence\":1.2}],\"warnings\":[]}"
            }
            """.trimIndent(),
        )
        val adapter = newAdapter(apiKey = "test-key")

        val exception = assertFailsWith<DocumentIntakeExtractionFailureException> {
            adapter.extract(
                documentType = DocumentIntakeDocumentType.REGISTRY,
                originalFileName = "registry.pdf",
                contentType = "application/pdf",
                bytes = createPdfFixture(listOf("owner landlord")),
            )
        }

        assertEquals("AI_RESPONSE_INVALID", exception.code)
    }

    @Test
    fun failsExplicitlyWhenApiKeyIsMissing() {
        val adapter = newAdapter(apiKey = "")

        val exception = assertFailsWith<DocumentIntakeExtractionFailureException> {
            adapter.extract(
                documentType = DocumentIntakeDocumentType.REGISTRY,
                originalFileName = "registry.pdf",
                contentType = "application/pdf",
                bytes = createPdfFixture(listOf("owner landlord")),
            )
        }

        assertEquals("AI_PROVIDER_UNAVAILABLE", exception.code)
    }

    private fun newAdapter(apiKey: String): OpenAiDocumentIntakeExtractionAdapter {
        return OpenAiDocumentIntakeExtractionAdapter(
            aiProperties = DocumentIntakeAiProperties(
                apiKey = apiKey,
                model = "gpt-4.1-mini",
                baseUrl = "http://127.0.0.1:${server?.address?.port ?: 1}/v1",
                timeout = Duration.ofSeconds(3),
            ),
            documentIntakePdfTextExtractor = PdfBoxDocumentIntakePdfTextExtractor(),
            documentIntakeAiResultValidator = DocumentIntakeAiResultValidator(DocumentIntakeFieldValueParser()),
        )
    }

    private fun startServer(responseBody: String) {
        receivedRequestBody = null
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/v1/responses") { exchange ->
                receivedRequestBody = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                val bytes = responseBody.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
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

    private fun createBlankPdfFixture(): ByteArray {
        val output = ByteArrayOutputStream()
        PDDocument().use { document ->
            document.addPage(PDPage())
            document.save(output)
        }
        return output.toByteArray()
    }
}
