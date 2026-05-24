package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DocumentIntakeAiResultValidatorTest {
    private val validator = DocumentIntakeAiResultValidator(DocumentIntakeFieldValueParser())

    @Test
    fun keepsValidFieldsAndSkipsInvalidAiFieldValues() {
        val result = validator.validate(
            documentType = DocumentIntakeDocumentType.REGISTRY,
            payload = DocumentIntakeAiExtractionPayload(
                fields = listOf(
                    field("REGISTRY_OWNER_NAME", "홍길동"),
                    field("REGISTRY_HAS_MORTGAGE", "판단 불가"),
                    field("REGISTRY_SENIOR_DEBT_AMOUNT", "금 1,234,000원"),
                ),
                warnings = listOf(
                    DocumentIntakeAiWarningPayload(
                        warningType = "UNKNOWN_WARNING",
                        message = "unknown",
                        relatedFieldKeys = emptyList(),
                    ),
                ),
            ),
            maxSourcePage = 1,
        )

        assertEquals(2, result.fields.size)
        assertEquals("REGISTRY_OWNER_NAME", result.fields[0].fieldKey.name)
        assertEquals("REGISTRY_SENIOR_DEBT_AMOUNT", result.fields[1].fieldKey.name)
        assertEquals("1234000", result.fields[1].value)
        assertEquals(0, result.warnings.size)
    }

    @Test
    fun failsWhenNoFieldsCanBeSaved() {
        val exception = assertFailsWith<DocumentIntakeExtractionFailureException> {
            validator.validate(
                documentType = DocumentIntakeDocumentType.REGISTRY,
                payload = DocumentIntakeAiExtractionPayload(
                    fields = listOf(field("REGISTRY_HAS_MORTGAGE", "판단 불가")),
                ),
                maxSourcePage = 1,
            )
        }

        assertEquals("AI_RESPONSE_INVALID", exception.code)
    }

    private fun field(
        fieldKey: String,
        value: String,
    ): DocumentIntakeAiFieldPayload {
        return DocumentIntakeAiFieldPayload(
            fieldKey = fieldKey,
            value = value,
            sourcePage = 1,
            sourceText = "근거",
            confidence = BigDecimal("0.8"),
        )
    }
}
