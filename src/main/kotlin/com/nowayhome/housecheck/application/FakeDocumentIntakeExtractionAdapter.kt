package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeWarningType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@Component
@ConditionalOnProperty(
    prefix = "housecheck.document-intake.extraction",
    name = ["provider"],
    havingValue = "fake",
)
class FakeDocumentIntakeExtractionAdapter : DocumentIntakeExtractionPort {
    override fun extract(
        documentType: DocumentIntakeDocumentType,
        originalFileName: String,
        contentType: String,
        bytes: ByteArray,
    ): ExtractedDocumentResult {
        val scenario = String(bytes, StandardCharsets.UTF_8).lowercase()
        if (scenario.contains("extract-fail")) {
            throw DocumentIntakeExtractionFailureException(
                code = "FAKE_EXTRACTION_FAILURE",
                message = "문서 처리에 실패했습니다. 파일을 다시 확인해 주세요.",
            )
        }
        return when (documentType) {
            DocumentIntakeDocumentType.REGISTRY -> registryResult(scenario)
            DocumentIntakeDocumentType.LEASE_CONTRACT -> leaseResult(scenario)
        }
    }

    private fun registryResult(scenario: String): ExtractedDocumentResult {
        val address = if (scenario.contains("registry-address-mismatch")) {
            "서울시 서대문구 통일로 10"
        } else {
            "서울시 마포구 양화로 1"
        }
        val ownerName = if (scenario.contains("owner-mismatch")) "다른 소유자" else "임대인"
        return ExtractedDocumentResult(
            fields = listOf(
                field(DocumentIntakeFieldKey.REGISTRY_ADDRESS, address, 1, "건물주소 $address", "0.96"),
                field(DocumentIntakeFieldKey.REGISTRY_ISSUED_DATE, "2026-05-20", 1, "발급일자 2026-05-20", "0.95"),
                field(DocumentIntakeFieldKey.REGISTRY_OWNER_NAME, ownerName, 2, "소유자 $ownerName", "0.93"),
                field(DocumentIntakeFieldKey.REGISTRY_HAS_TRUST_REGISTRATION, "false", 2, "신탁사항 없음", "0.90"),
                field(DocumentIntakeFieldKey.REGISTRY_HAS_SEIZURE, "false", 2, "압류 없음", "0.92"),
                field(DocumentIntakeFieldKey.REGISTRY_HAS_PROVISIONAL_SEIZURE, "false", 2, "가압류 없음", "0.92"),
                field(DocumentIntakeFieldKey.REGISTRY_HAS_PROVISIONAL_DISPOSITION, "false", 2, "가처분 없음", "0.92"),
                field(DocumentIntakeFieldKey.REGISTRY_HAS_AUCTION_PROCEEDING, "false", 2, "경매개시결정 없음", "0.92"),
                field(DocumentIntakeFieldKey.REGISTRY_HAS_LEASE_REGISTRATION, "false", 3, "임차권등기 없음", "0.88"),
                field(DocumentIntakeFieldKey.REGISTRY_HAS_MORTGAGE, "true", 3, "근저당권 설정", "0.89"),
                field(DocumentIntakeFieldKey.REGISTRY_SENIOR_DEBT_AMOUNT, "12000000", 3, "채권최고액 12,000,000원", "0.86"),
            ),
        )
    }

    private fun leaseResult(scenario: String): ExtractedDocumentResult {
        val depositAmount = if (scenario.contains("deposit-mismatch")) "65000000" else "60000000"
        val addressRoad = if (scenario.contains("lease-address-mismatch")) "서울시 강서구 공항대로 9" else "서울시 마포구 양화로 1"
        val landlordName = if (scenario.contains("owner-mismatch")) "다른 임대인" else "임대인"
        val warnings = buildList {
            if (scenario.contains("deposit-mismatch")) {
                add(
                    ExtractedDocumentWarning(
                        warningType = DocumentIntakeWarningType.DEPOSIT_MISMATCH,
                        message = "계약서 보증금 표기가 서로 다를 수 있어 확인이 필요합니다.",
                        relatedFieldKeys = listOf(DocumentIntakeFieldKey.LEASE_DEPOSIT_AMOUNT),
                    ),
                )
            }
        }
        return ExtractedDocumentResult(
            fields = listOf(
                field(DocumentIntakeFieldKey.LEASE_ADDRESS_ROAD, addressRoad, 1, "소재지 $addressRoad", "0.95"),
                field(DocumentIntakeFieldKey.LEASE_ADDRESS_LOT, "합정동 100-1", 1, "지번 합정동 100-1", "0.84"),
                field(DocumentIntakeFieldKey.LEASE_CONTRACT_TYPE, "JEONSE", 1, "계약종류 전세", "0.87"),
                field(DocumentIntakeFieldKey.LEASE_DEPOSIT_AMOUNT, depositAmount, 1, "보증금 ${depositAmount}원", "0.92"),
                field(DocumentIntakeFieldKey.LEASE_MONTHLY_RENT_AMOUNT, "0", 1, "월세 0원", "0.93"),
                field(DocumentIntakeFieldKey.LEASE_CONTRACT_DATE, LocalDate.of(2026, 5, 24).toString(), 1, "계약일 2026-05-24", "0.90"),
                field(DocumentIntakeFieldKey.LEASE_OCCUPANCY_DATE, LocalDate.of(2026, 6, 1).toString(), 2, "입주예정일 2026-06-01", "0.88"),
                field(DocumentIntakeFieldKey.LEASE_LANDLORD_NAME, landlordName, 1, "임대인 $landlordName", "0.91"),
                field(DocumentIntakeFieldKey.LEASE_SPECIAL_TERMS, "관리비 정산 방식 확인 필요", 3, "특약 관리비 정산 방식 확인", "0.61"),
            ),
            warnings = warnings,
        )
    }

    private fun field(
        fieldKey: DocumentIntakeFieldKey,
        value: String,
        sourcePage: Int,
        sourceText: String,
        confidence: String,
    ): ExtractedDocumentField {
        return ExtractedDocumentField(
            fieldKey = fieldKey,
            value = value,
            sourcePage = sourcePage,
            sourceText = sourceText,
            confidence = BigDecimal(confidence),
        )
    }
}
