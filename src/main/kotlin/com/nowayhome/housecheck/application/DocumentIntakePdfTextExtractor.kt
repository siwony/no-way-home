package com.nowayhome.housecheck.application

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Component
import java.io.IOException

data class ExtractedPdfPageText(
    val pageNumber: Int,
    val text: String,
)

interface DocumentIntakePdfTextExtractor {
    fun extract(bytes: ByteArray): List<ExtractedPdfPageText>
}

@Component
class PdfBoxDocumentIntakePdfTextExtractor : DocumentIntakePdfTextExtractor {
    override fun extract(bytes: ByteArray): List<ExtractedPdfPageText> {
        try {
            Loader.loadPDF(bytes).use { document ->
                val stripper = PDFTextStripper()
                val pages = (1..document.numberOfPages).map { pageNumber ->
                    stripper.startPage = pageNumber
                    stripper.endPage = pageNumber
                    ExtractedPdfPageText(
                        pageNumber = pageNumber,
                        text = stripper.getText(document)
                            .replace("\u0000", "")
                            .trim(),
                    )
                }
                if (pages.isEmpty()) {
                    throw DocumentIntakeExtractionFailureException(
                        code = "PDF_TEXT_EXTRACTION_EMPTY",
                        message = "PDF 페이지를 찾지 못했습니다. 파일을 다시 확인해 주세요.",
                    )
                }
                return pages
            }
        } catch (_: InvalidPasswordException) {
            throw DocumentIntakeExtractionFailureException(
                code = "PDF_ENCRYPTED",
                message = "암호화된 PDF는 바로 읽을 수 없습니다. 비밀번호 없이 열리는 파일로 다시 업로드해 주세요.",
            )
        } catch (exception: DocumentIntakeExtractionFailureException) {
            throw exception
        } catch (_: IOException) {
            throw DocumentIntakeExtractionFailureException(
                code = "PDF_PARSE_FAILED",
                message = "PDF 텍스트를 읽지 못했습니다. 파일을 다시 확인해 주세요.",
            )
        }
    }
}
