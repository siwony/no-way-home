package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.nowayhome.housecheck.domain.DocumentIntakeFieldKey
import com.nowayhome.housecheck.domain.DocumentIntakeWarningType
import java.math.BigDecimal

data class ExtractedDocumentField(
    val fieldKey: DocumentIntakeFieldKey,
    val value: String,
    val sourcePage: Int?,
    val sourceText: String?,
    val confidence: BigDecimal,
)

data class ExtractedDocumentWarning(
    val warningType: DocumentIntakeWarningType,
    val message: String,
    val relatedFieldKeys: List<DocumentIntakeFieldKey>,
)

data class ExtractedDocumentResult(
    val fields: List<ExtractedDocumentField>,
    val warnings: List<ExtractedDocumentWarning> = emptyList(),
)

interface DocumentIntakeExtractionPort {
    fun extract(
        documentType: DocumentIntakeDocumentType,
        originalFileName: String,
        contentType: String,
        bytes: ByteArray,
    ): ExtractedDocumentResult
}

class DocumentIntakeExtractionFailureException(
    val code: String,
    override val message: String,
) : RuntimeException(message)
