package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import com.nowayhome.housecheck.domain.HouseCheckException
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.util.unit.DataSize
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DocumentIntakeFilePolicyTest {
    private val uploadSizePolicy = DocumentIntakeUploadSizePolicy(DataSize.ofMegabytes(20))
    private val filePolicy = DocumentIntakeFilePolicy(uploadSizePolicy)
    private val maxUploadSizeBytes = 20 * 1024 * 1024

    @Test
    fun rejectsFilesLargerThanConfiguredMaximum() {
        val file = MockMultipartFile(
            "file",
            "registry.pdf",
            "application/pdf",
            ByteArray(maxUploadSizeBytes + 1) { '0'.code.toByte() },
        )

        val exception = assertFailsWith<HouseCheckException> {
            filePolicy.validate(DocumentIntakeDocumentType.REGISTRY, file)
        }

        assertEquals(HouseCheckErrorCode.DOCUMENT_INTAKE_FILE_TOO_LARGE, exception.errorCode)
        assertEquals("업로드 파일은 최대 20MB까지 등록할 수 있습니다.", exception.message)
    }
}
