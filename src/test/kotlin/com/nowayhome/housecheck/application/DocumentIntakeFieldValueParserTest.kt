package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DocumentIntakeFieldValueParserTest {
    private val parser = DocumentIntakeFieldValueParser()

    @Test
    fun normalizesHumanReadableMoneyValuesFromAi() {
        assertEquals(
            "1234000",
            parser.normalize(DocumentIntakeFieldKey.REGISTRY_SENIOR_DEBT_AMOUNT, "금 1,234,000원"),
        )
        assertEquals(
            "120000000",
            parser.normalize(DocumentIntakeFieldKey.LEASE_DEPOSIT_AMOUNT, "1.2억원"),
        )
        assertEquals(
            "150000000",
            parser.normalize(DocumentIntakeFieldKey.LEASE_DEPOSIT_AMOUNT, "1억 5,000만 원"),
        )
        assertEquals(
            "0",
            parser.normalize(DocumentIntakeFieldKey.REGISTRY_SENIOR_DEBT_AMOUNT, "해당 없음"),
        )
    }

    @Test
    fun normalizesHumanReadableBooleanValuesFromAi() {
        assertEquals(
            "true",
            parser.normalize(DocumentIntakeFieldKey.REGISTRY_HAS_MORTGAGE, "설정 있음"),
        )
        assertEquals(
            "false",
            parser.normalize(DocumentIntakeFieldKey.REGISTRY_HAS_SEIZURE, "해당사항 없음"),
        )
        assertEquals(
            "false",
            parser.normalize(DocumentIntakeFieldKey.REGISTRY_HAS_TRUST_REGISTRATION, "미등재"),
        )
    }

    @Test
    fun normalizesHumanReadableDatesFromAi() {
        assertEquals(
            "2026-05-24",
            parser.normalize(DocumentIntakeFieldKey.REGISTRY_ISSUED_DATE, "2026.05.24"),
        )
        assertEquals(
            "2026-05-24",
            parser.normalize(DocumentIntakeFieldKey.LEASE_CONTRACT_DATE, "2026년 5월 24일"),
        )
    }

    @Test
    fun normalizesKoreanLeaseContractTypesFromAi() {
        assertEquals(
            "JEONSE",
            parser.normalize(DocumentIntakeFieldKey.LEASE_CONTRACT_TYPE, "전세"),
        )
        assertEquals(
            "MONTHLY_RENT",
            parser.normalize(DocumentIntakeFieldKey.LEASE_CONTRACT_TYPE, "보증부월세"),
        )
    }
}
