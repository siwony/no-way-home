package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.HouseCheckException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize

private const val defaultDocumentIntakeMaxUploadSize = "20MB"

@Component
class DocumentIntakeUploadSizePolicy(
    @param:Value("\${housecheck.document-intake.max-upload-size:$defaultDocumentIntakeMaxUploadSize}") private val maxUploadSize: DataSize,
) {
    fun validate(fileSize: Long) {
        if (fileSize > maxUploadSize.toBytes()) {
            throw HouseCheckException(
                errorCode = HouseCheckErrorCode.DOCUMENT_INTAKE_FILE_TOO_LARGE,
                message = maxUploadSizeExceededMessage(),
            )
        }
    }

    fun maxUploadSizeExceededMessage(): String {
        return "업로드 파일은 최대 ${maxUploadSize.toMegabytes()}MB까지 등록할 수 있습니다."
    }
}
