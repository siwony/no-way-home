package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.nowayhome.housecheck.domain.HouseCheckException
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

data class ValidatedDocumentIntakeUpload(
    val originalFileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

@Component
class DocumentIntakeFilePolicy {
    fun validate(documentType: DocumentIntakeDocumentType, file: MultipartFile): ValidatedDocumentIntakeUpload {
        if (file.isEmpty) {
            throw HouseCheckException(HouseCheckErrorCode.VALIDATION_ERROR)
        }

        val originalFileName = file.originalFilename?.trim().orEmpty().ifBlank { "uploaded.bin" }
        val extension = originalFileName.substringAfterLast('.', "").lowercase()
        val mimeType = file.contentType?.trim()?.lowercase().orEmpty()
        val isValid = when (documentType) {
            DocumentIntakeDocumentType.REGISTRY -> mimeType == "application/pdf" && extension == "pdf"
            DocumentIntakeDocumentType.LEASE_CONTRACT -> {
                (mimeType == "application/pdf" && extension == "pdf") ||
                    (mimeType == "image/jpeg" && (extension == "jpg" || extension == "jpeg")) ||
                    (mimeType == "image/png" && extension == "png") ||
                    (mimeType == "image/webp" && extension == "webp")
            }
        }

        if (!isValid) {
            throw HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_INVALID_FILE_TYPE)
        }

        return ValidatedDocumentIntakeUpload(
            originalFileName = originalFileName,
            mimeType = mimeType,
            bytes = file.bytes,
        )
    }
}
