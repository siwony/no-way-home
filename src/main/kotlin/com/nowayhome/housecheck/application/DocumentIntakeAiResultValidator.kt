package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeWarningType
import org.springframework.stereotype.Component
import java.math.BigDecimal

data class DocumentIntakeAiFieldPayload(
    val fieldKey: String,
    val value: String,
    val sourcePage: Int,
    val sourceText: String,
    val confidence: BigDecimal,
)

data class DocumentIntakeAiWarningPayload(
    val warningType: String,
    val message: String,
    val relatedFieldKeys: List<String>,
)

data class DocumentIntakeAiExtractionPayload(
    val fields: List<DocumentIntakeAiFieldPayload>,
    val warnings: List<DocumentIntakeAiWarningPayload> = emptyList(),
)

@Component
class DocumentIntakeAiResultValidator(
    private val documentIntakeFieldValueParser: DocumentIntakeFieldValueParser,
) {
    fun validate(
        documentType: DocumentIntakeDocumentType,
        payload: DocumentIntakeAiExtractionPayload,
        maxSourcePage: Int,
    ): ExtractedDocumentResult {
        require(maxSourcePage >= 1) { "maxSourcePage must be positive" }

        val duplicateFieldKeys = payload.fields.groupingBy { it.fieldKey.trim().uppercase() }.eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateFieldKeys.isNotEmpty()) {
            throw invalid("AI 응답에 중복 필드가 포함되어 있습니다.")
        }

        val fields = payload.fields.map { field ->
            val fieldKey = parseFieldKey(field.fieldKey, documentType)
            ExtractedDocumentField(
                fieldKey = fieldKey,
                value = normalizeFieldValue(fieldKey, field.value),
                sourcePage = validateSourcePage(field.sourcePage, maxSourcePage),
                sourceText = validateSourceText(field.sourceText),
                confidence = validateConfidence(field.confidence),
            )
        }

        val warnings = payload.warnings.map { warning ->
            ExtractedDocumentWarning(
                warningType = parseWarningType(warning.warningType),
                message = validateWarningMessage(warning.message),
                relatedFieldKeys = warning.relatedFieldKeys.map(::parseRelatedFieldKey).distinct(),
            )
        }

        return ExtractedDocumentResult(fields = fields, warnings = warnings)
    }

    private fun parseFieldKey(rawValue: String, documentType: DocumentIntakeDocumentType): DocumentIntakeFieldKey {
        val fieldKey = runCatching { DocumentIntakeFieldKey.fromValue(rawValue) }
            .getOrElse { throw invalid("AI 응답에 지원하지 않는 필드가 포함되어 있습니다.") }
        if (fieldKey.documentType != documentType) {
            throw invalid("AI 응답 필드가 업로드한 문서 종류와 일치하지 않습니다.")
        }
        return fieldKey
    }

    private fun normalizeFieldValue(fieldKey: DocumentIntakeFieldKey, rawValue: String): String {
        return runCatching { documentIntakeFieldValueParser.normalize(fieldKey, rawValue) }
            .getOrElse { throw invalid("AI 응답 필드 값 형식이 올바르지 않습니다.") }
    }

    private fun validateSourcePage(sourcePage: Int, maxSourcePage: Int): Int {
        if (sourcePage !in 1..maxSourcePage) {
            throw invalid("AI 응답 sourcePage가 문서 범위를 벗어났습니다.")
        }
        return sourcePage
    }

    private fun validateSourceText(sourceText: String): String {
        val normalized = sourceText.trim()
        if (normalized.isEmpty()) {
            throw invalid("AI 응답 sourceText가 비어 있습니다.")
        }
        if (normalized.length > 2000) {
            throw invalid("AI 응답 sourceText가 너무 깁니다.")
        }
        return normalized
    }

    private fun validateConfidence(confidence: BigDecimal): BigDecimal {
        if (confidence < BigDecimal.ZERO || confidence > BigDecimal.ONE) {
            throw invalid("AI 응답 confidence 범위가 올바르지 않습니다.")
        }
        return confidence
    }

    private fun parseWarningType(rawValue: String): DocumentIntakeWarningType {
        return DocumentIntakeWarningType.entries.firstOrNull { it.name.equals(rawValue.trim(), ignoreCase = true) }
            ?: throw invalid("AI 응답 warningType이 올바르지 않습니다.")
    }

    private fun validateWarningMessage(message: String): String {
        val normalized = message.trim()
        if (normalized.isEmpty() || normalized.length > 255) {
            throw invalid("AI 응답 warning message가 올바르지 않습니다.")
        }
        return normalized
    }

    private fun parseRelatedFieldKey(rawValue: String): DocumentIntakeFieldKey {
        return runCatching { DocumentIntakeFieldKey.fromValue(rawValue) }
            .getOrElse { throw invalid("AI 응답 relatedFieldKeys가 올바르지 않습니다.") }
    }

    private fun invalid(message: String): DocumentIntakeExtractionFailureException {
        return DocumentIntakeExtractionFailureException(
            code = "AI_RESPONSE_INVALID",
            message = message,
        )
    }
}
