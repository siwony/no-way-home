package com.nowayhome.housecheck.application

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
@ConditionalOnProperty(
    prefix = "housecheck.document-intake.extraction",
    name = ["provider"],
    havingValue = "openai",
    matchIfMissing = true,
)
class OpenAiDocumentIntakeExtractionAdapter(
    private val aiProperties: DocumentIntakeAiProperties,
    private val documentIntakeAiAuthorizationProvider: DocumentIntakeAiAuthorizationProvider,
    private val documentIntakePdfTextExtractor: DocumentIntakePdfTextExtractor,
    private val documentIntakeAiResultValidator: DocumentIntakeAiResultValidator,
) : DocumentIntakeExtractionPort {
    private val objectMapper = jacksonObjectMapper()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(aiProperties.timeout)
        .build()

    override fun extract(
        documentType: DocumentIntakeDocumentType,
        originalFileName: String,
        contentType: String,
        bytes: ByteArray,
    ): ExtractedDocumentResult {
        val sourceMaterial = buildSourceMaterial(contentType, bytes)
        val authorizationHeader = documentIntakeAiAuthorizationProvider.authorizationHeader()
        val requestBody = buildRequestBody(documentType, originalFileName, contentType, sourceMaterial)
        val responseBody = invokeOpenAi(authorizationHeader, requestBody)
        val payload = parseStructuredPayload(responseBody)
        return documentIntakeAiResultValidator.validate(
            documentType = documentType,
            payload = payload,
            maxSourcePage = sourceMaterial.maxSourcePage,
        )
    }

    private fun buildSourceMaterial(contentType: String, bytes: ByteArray): SourceMaterial {
        return if (contentType == "application/pdf") {
            SourceMaterial.Pdf(
                pages = documentIntakePdfTextExtractor.extract(bytes),
                bytes = bytes,
            )
        } else {
            SourceMaterial.Image(contentType = contentType, bytes = bytes)
        }
    }

    private fun buildRequestBody(
        documentType: DocumentIntakeDocumentType,
        originalFileName: String,
        contentType: String,
        sourceMaterial: SourceMaterial,
    ): String {
        val request = linkedMapOf<String, Any>(
            "model" to aiProperties.model,
            "instructions" to buildInstructions(documentType),
            "input" to listOf(buildInputMessage(documentType, originalFileName, contentType, sourceMaterial)),
            "max_output_tokens" to 4000,
            "text" to mapOf(
                "format" to mapOf(
                    "type" to "json_schema",
                    "name" to "document_intake_extraction",
                    "strict" to true,
                    "schema" to responseSchema(),
                ),
            ),
        )
        return objectMapper.writeValueAsString(request)
    }

    private fun buildInputMessage(
        documentType: DocumentIntakeDocumentType,
        originalFileName: String,
        contentType: String,
        sourceMaterial: SourceMaterial,
    ): Map<String, Any> {
        val content = mutableListOf<Map<String, Any>>(
            mapOf(
                "type" to "input_text",
                "text" to buildUserPrompt(documentType, originalFileName, contentType, sourceMaterial),
            ),
        )
        if (sourceMaterial is SourceMaterial.Image) {
            content += mapOf(
                "type" to "input_image",
                "image_url" to sourceMaterial.toDataUrl(),
                "detail" to "high",
            )
        }
        if (sourceMaterial is SourceMaterial.Pdf) {
            content += mapOf(
                "type" to "input_file",
                "filename" to originalFileName,
                "file_data" to sourceMaterial.toDataUrl(),
            )
        }
        return mapOf(
            "role" to "user",
            "content" to content,
        )
    }

    private fun buildInstructions(documentType: DocumentIntakeDocumentType): String {
        return """
            You extract structured draft fields from Korean housing documents for human review.
            Return only fields supported for ${documentType.name}.
            Never fabricate values. Omit fields you cannot support from the source.
            Keep `sourceText` to a short verbatim evidence snippet copied from the document.
            Use page number 1 for image inputs.
            Exclude resident registration numbers, account numbers, and tenant full identity details.
            `LEASE_SPECIAL_TERMS` is review-only text and should stay concise.
            `LEASE_CONTRACT_TYPE` must be `JEONSE` or `MONTHLY_RENT`.
            Boolean values must be literal true/false based on explicit evidence.
            Confidence must be between 0 and 1.
        """.trimIndent()
    }

    private fun buildUserPrompt(
        documentType: DocumentIntakeDocumentType,
        originalFileName: String,
        contentType: String,
        sourceMaterial: SourceMaterial,
    ): String {
        val fieldGuide = when (documentType) {
            DocumentIntakeDocumentType.REGISTRY -> listOf(
                "REGISTRY_ADDRESS: registry address string",
                "REGISTRY_ISSUED_DATE: ISO date yyyy-MM-dd",
                "REGISTRY_OWNER_NAME: owner name",
                "REGISTRY_HAS_TRUST_REGISTRATION: boolean",
                "REGISTRY_HAS_SEIZURE: boolean",
                "REGISTRY_HAS_PROVISIONAL_SEIZURE: boolean",
                "REGISTRY_HAS_PROVISIONAL_DISPOSITION: boolean",
                "REGISTRY_HAS_AUCTION_PROCEEDING: boolean",
                "REGISTRY_HAS_LEASE_REGISTRATION: boolean",
                "REGISTRY_HAS_MORTGAGE: boolean",
                "REGISTRY_SENIOR_DEBT_AMOUNT: non-negative integer KRW amount",
            )

            DocumentIntakeDocumentType.LEASE_CONTRACT -> listOf(
                "LEASE_ADDRESS_ROAD: road address",
                "LEASE_ADDRESS_LOT: lot address if available",
                "LEASE_CONTRACT_TYPE: JEONSE or MONTHLY_RENT",
                "LEASE_DEPOSIT_AMOUNT: non-negative integer KRW amount",
                "LEASE_MONTHLY_RENT_AMOUNT: non-negative integer KRW amount",
                "LEASE_CONTRACT_DATE: ISO date yyyy-MM-dd",
                "LEASE_OCCUPANCY_DATE: ISO date yyyy-MM-dd",
                "LEASE_LANDLORD_NAME: landlord name",
                "LEASE_SPECIAL_TERMS: short summary of key special term",
            )
        }
        val sourceSummary = when (sourceMaterial) {
            is SourceMaterial.Pdf -> objectMapper.writeValueAsString(
                mapOf(
                    "fileName" to originalFileName,
                    "contentType" to contentType,
                    "pageCount" to sourceMaterial.maxSourcePage,
                    "locallyExtractedTextAvailable" to sourceMaterial.pages.any { it.text.isNotBlank() },
                    "locallyExtractedTextPages" to sourceMaterial.pages
                        .filter { it.text.isNotBlank() }
                        .map { mapOf("page" to it.pageNumber, "text" to it.text.take(6000)) },
                    "pdfAttachedAsInputFile" to true,
                ),
            )

            is SourceMaterial.Image -> objectMapper.writeValueAsString(
                mapOf(
                    "fileName" to originalFileName,
                    "contentType" to contentType,
                    "imageEvidencePage" to 1,
                ),
            )
        }
        return """
            Document type: ${documentType.name}
            Supported fields:
            ${fieldGuide.joinToString(separator = "\n") { "- $it" }}

            Detect only supported warning types when the same document contains conflicting evidence:
            - ADDRESS_MISMATCH
            - LANDLORD_OWNER_MISMATCH
            - DEPOSIT_MISMATCH

            Source material:
            $sourceSummary
        """.trimIndent()
    }

    private fun invokeOpenAi(authorizationHeader: String, requestBody: String): String {
        val request = HttpRequest.newBuilder()
            .uri(resolveResponsesUri(aiProperties.baseUrl))
            .header("Authorization", authorizationHeader)
            .header("Content-Type", "application/json")
            .timeout(aiProperties.timeout)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        } catch (_: HttpTimeoutException) {
            throw providerFailure("AI_REVIEW_FAILED", "문서 처리 중 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.")
        } catch (_: ConnectException) {
            throw providerFailure("AI_REVIEW_FAILED", "문서 처리용 AI에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.")
        } catch (_: Exception) {
            throw providerFailure("AI_REVIEW_FAILED", "문서 처리 중 외부 AI 호출에 실패했습니다. 잠시 후 다시 시도해 주세요.")
        }

        if (response.statusCode() in 200..299) {
            return response.body()
        }

        val errorMessage = parseErrorMessage(response.body())
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw providerFailure(
                "AI_PROVIDER_UNAVAILABLE",
                errorMessage ?: "문서 처리용 AI 인증에 실패했습니다. 설정을 다시 확인해 주세요.",
            )
        }
        throw providerFailure(
            "AI_REVIEW_FAILED",
            errorMessage ?: "문서 처리용 AI가 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        )
    }

    private fun parseStructuredPayload(responseBody: String): DocumentIntakeAiExtractionPayload {
        val responseJson = try {
            objectMapper.readTree(responseBody)
        } catch (_: JsonProcessingException) {
            throw providerFailure("AI_RESPONSE_INVALID", "AI 응답을 해석하지 못했습니다.")
        }
        val outputText = extractOutputText(responseJson)
        if (outputText.isBlank()) {
            throw providerFailure("AI_RESPONSE_INVALID", "AI 응답 본문이 비어 있습니다.")
        }
        return try {
            objectMapper.readValue(outputText, DocumentIntakeAiExtractionPayload::class.java)
        } catch (_: JsonProcessingException) {
            throw providerFailure("AI_RESPONSE_INVALID", "AI 응답 JSON 형식이 올바르지 않습니다.")
        }
    }

    private fun extractOutputText(responseJson: JsonNode): String {
        val directOutputText = responseJson.path("output_text").asText("").trim()
        if (directOutputText.isNotBlank()) {
            return directOutputText
        }

        val contentTexts = mutableListOf<String>()
        responseJson.path("output").forEach { outputNode ->
            outputNode.path("content").forEach { contentNode ->
                val text = contentNode.path("text").asText("").trim()
                if (text.isNotBlank()) {
                    contentTexts += text
                }
            }
        }
        return contentTexts.joinToString(separator = "\n").trim()
    }

    private fun parseErrorMessage(responseBody: String): String? {
        return runCatching {
            objectMapper.readTree(responseBody).path("error").path("message").asText(null)?.trim()
        }.getOrNull()?.takeIf { !it.isNullOrBlank() }
    }

    private fun resolveResponsesUri(baseUrl: String): URI {
        val normalized = baseUrl.trim().trimEnd('/')
        return URI.create("$normalized/responses")
    }

    private fun responseSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "fields" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "additionalProperties" to false,
                        "properties" to mapOf(
                            "fieldKey" to mapOf("type" to "string"),
                            "value" to mapOf("type" to "string"),
                            "sourcePage" to mapOf("type" to "integer"),
                            "sourceText" to mapOf("type" to "string"),
                            "confidence" to mapOf("type" to "number"),
                        ),
                        "required" to listOf("fieldKey", "value", "sourcePage", "sourceText", "confidence"),
                    ),
                ),
                "warnings" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "additionalProperties" to false,
                        "properties" to mapOf(
                            "warningType" to mapOf("type" to "string"),
                            "message" to mapOf("type" to "string"),
                            "relatedFieldKeys" to mapOf(
                                "type" to "array",
                                "items" to mapOf("type" to "string"),
                            ),
                        ),
                        "required" to listOf("warningType", "message", "relatedFieldKeys"),
                    ),
                ),
            ),
            "required" to listOf("fields", "warnings"),
        )
    }

    private fun providerFailure(code: String, message: String): DocumentIntakeExtractionFailureException {
        return DocumentIntakeExtractionFailureException(code = code, message = message)
    }

    private sealed interface SourceMaterial {
        val maxSourcePage: Int

        data class Pdf(
            val pages: List<ExtractedPdfPageText>,
            val bytes: ByteArray,
        ) : SourceMaterial {
            override val maxSourcePage: Int = pages.maxOfOrNull { it.pageNumber } ?: 1

            fun toDataUrl(): String {
                val encoded = java.util.Base64.getEncoder().encodeToString(bytes)
                return "data:application/pdf;base64,$encoded"
            }
        }

        data class Image(
            val contentType: String,
            val bytes: ByteArray,
        ) : SourceMaterial {
            override val maxSourcePage: Int = 1

            fun toDataUrl(): String {
                val encoded = java.util.Base64.getEncoder().encodeToString(bytes)
                return "data:$contentType;base64,$encoded"
            }
        }
    }
}
