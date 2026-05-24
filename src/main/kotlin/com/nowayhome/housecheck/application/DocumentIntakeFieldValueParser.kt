package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.ContractType
import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeFieldValueType
import com.nowayhome.housecheck.domain.HouseCheckException
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DocumentIntakeFieldValueParser {
    fun normalize(fieldKey: DocumentIntakeFieldKey, value: String?): String {
        val normalizedValue = value?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_INVALID_FIELD_VALUE)
        return when (fieldKey.valueType) {
            DocumentIntakeFieldValueType.STRING -> normalizeString(fieldKey, normalizedValue)
            DocumentIntakeFieldValueType.LONG -> normalizedValue.toLongOrNull()
                ?.takeIf { it >= 0 }
                ?.toString()
                ?: throw HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_INVALID_FIELD_VALUE)

            DocumentIntakeFieldValueType.BOOLEAN -> when (normalizedValue.lowercase()) {
                "true" -> "true"
                "false" -> "false"
                else -> throw HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_INVALID_FIELD_VALUE)
            }

            DocumentIntakeFieldValueType.DATE -> runCatching { LocalDate.parse(normalizedValue).toString() }
                .getOrElse { throw HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_INVALID_FIELD_VALUE) }
        }
    }

    private fun normalizeString(fieldKey: DocumentIntakeFieldKey, value: String): String {
        return if (fieldKey == DocumentIntakeFieldKey.LEASE_CONTRACT_TYPE) {
            ContractType.fromApiValue(value).name
        } else {
            value
        }
    }
}
