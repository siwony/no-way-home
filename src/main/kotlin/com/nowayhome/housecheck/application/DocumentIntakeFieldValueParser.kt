package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.ContractType
import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeFieldValueType
import com.nowayhome.housecheck.domain.HouseCheckException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField

@Component
class DocumentIntakeFieldValueParser {
    fun normalize(fieldKey: DocumentIntakeFieldKey, value: String?): String {
        val normalizedValue = value?.trim()?.takeIf { it.isNotEmpty() } ?: invalidFieldValue()
        return try {
            when (fieldKey.valueType) {
                DocumentIntakeFieldValueType.STRING -> normalizeString(fieldKey, normalizedValue)
                DocumentIntakeFieldValueType.LONG -> normalizeLong(normalizedValue)

                DocumentIntakeFieldValueType.BOOLEAN -> normalizeBoolean(normalizedValue)

                DocumentIntakeFieldValueType.DATE -> normalizeDate(normalizedValue)
            }
        } catch (_: HouseCheckException) {
            throw invalidFieldValue()
        }
    }

    private fun normalizeString(fieldKey: DocumentIntakeFieldKey, value: String): String {
        return if (fieldKey == DocumentIntakeFieldKey.LEASE_CONTRACT_TYPE) {
            when (value.trim().uppercase()) {
                "전세" -> ContractType.JEONSE.name
                "월세", "반전세", "보증부월세" -> ContractType.MONTHLY_RENT.name
                else -> ContractType.fromApiValue(value).name
            }
        } else {
            value
        }
    }

    private fun normalizeLong(value: String): String {
        val normalized = value.trim()
            .replace(",", "")
            .replace(" ", "")
            .replace("금", "")
            .replace("₩", "")
            .replace("KRW", "", ignoreCase = true)
            .replace("원", "")

        if (listOf("없음", "없다", "무", "해당없음", "해당사항없음").any { normalized.contains(it) }) {
            return "0"
        }

        val amountFromKoreanUnits = parseKoreanMoneyAmount(normalized)
        if (amountFromKoreanUnits != null) {
            return amountFromKoreanUnits.toString()
        }

        val digitsOnly = normalized.filter { it.isDigit() }
        return digitsOnly.takeIf { it.isNotBlank() }
            ?.toLongOrNull()
            ?.takeIf { it >= 0 }
            ?.toString()
            ?: invalidFieldValue()
    }

    private fun parseKoreanMoneyAmount(value: String): Long? {
        if (!value.contains("억") && !value.contains("만")) {
            return null
        }

        var total = BigDecimal.ZERO
        val eokMatch = Regex("""(\d+(?:\.\d+)?)억""").find(value)
        if (eokMatch != null) {
            total += BigDecimal(eokMatch.groupValues[1]).multiply(BigDecimal("100000000"))
        }

        val manMatch = Regex("""(\d+(?:\.\d+)?)만""").find(value)
        if (manMatch != null) {
            total += BigDecimal(manMatch.groupValues[1]).multiply(BigDecimal("10000"))
        }

        return total.takeIf { it >= BigDecimal.ZERO && it > BigDecimal.ZERO }?.toLong()
    }

    private fun normalizeBoolean(value: String): String {
        val normalized = value.trim().lowercase()
            .replace(" ", "")
            .replace(".", "")
            .replace(":", "")
        val falseTokens = listOf(
            "false",
            "no",
            "n",
            "0",
            "아니오",
            "아님",
            "없음",
            "없다",
            "무",
            "비해당",
            "부존재",
            "미설정",
            "미등재",
            "해당없음",
            "해당사항없음",
        )
        val trueTokens = listOf(
            "true",
            "yes",
            "y",
            "1",
            "예",
            "있음",
            "있다",
            "유",
            "해당",
            "존재",
            "설정",
            "등재",
            "진행중",
        )
        if (falseTokens.any { normalized == it || normalized.contains(it) }) {
            return "false"
        }
        if (trueTokens.any { normalized == it || normalized.contains(it) }) {
            return "true"
        }
        return invalidFieldValue()
    }

    private fun normalizeDate(value: String): String {
        val normalized = value.trim()
            .replace("년", "-")
            .replace("월", "-")
            .replace("일", "")
            .replace(".", "-")
            .replace("/", "-")
            .replace(Regex("""\s+"""), "")
            .trim('-')
        return DATE_FORMATTERS.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(normalized, formatter).toString() }.getOrNull()
        } ?: invalidFieldValue()
    }

    private fun invalidFieldValue(): Nothing {
        throw HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_INVALID_FIELD_VALUE)
    }

    companion object {
        private val FLEXIBLE_DASH_DATE_FORMATTER = DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT)

        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            FLEXIBLE_DASH_DATE_FORMATTER,
        )
    }
}
