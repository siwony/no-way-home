package com.nowayhome.housecheck.application

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DocumentIntakePdfTextExtractorTest {
    private val extractor = PdfBoxDocumentIntakePdfTextExtractor()

    @Test
    fun extractsPerPageTextFromPdfBytes() {
        val bytes = createPdfFixture(listOf("Page one owner", "Page two address"))

        val pages = extractor.extract(bytes)

        assertEquals(2, pages.size)
        assertEquals(1, pages[0].pageNumber)
        assertTrue(pages[0].text.contains("Page one owner"))
        assertEquals(2, pages[1].pageNumber)
        assertTrue(pages[1].text.contains("Page two address"))
    }

    @Test
    fun failsExplicitlyForInvalidPdfBytes() {
        val exception = assertFailsWith<DocumentIntakeExtractionFailureException> {
            extractor.extract("not a pdf".encodeToByteArray())
        }

        assertEquals("PDF_PARSE_FAILED", exception.code)
    }

    @Test
    fun keepsBlankPageMetadataForVisionFallback() {
        val bytes = createBlankPdfFixture()

        val pages = extractor.extract(bytes)

        assertEquals(1, pages.size)
        assertEquals(1, pages[0].pageNumber)
        assertEquals("", pages[0].text)
    }

    private fun createPdfFixture(pageTexts: List<String>): ByteArray {
        val output = ByteArrayOutputStream()
        PDDocument().use { document ->
            val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
            pageTexts.forEach { text ->
                val page = PDPage()
                document.addPage(page)
                PDPageContentStream(document, page).use { content ->
                    content.beginText()
                    content.setFont(font, 12f)
                    content.newLineAtOffset(72f, 720f)
                    content.showText(text)
                    content.endText()
                }
            }
            document.save(output)
        }
        return output.toByteArray()
    }

    private fun createBlankPdfFixture(): ByteArray {
        val output = ByteArrayOutputStream()
        PDDocument().use { document ->
            document.addPage(PDPage())
            document.save(output)
        }
        return output.toByteArray()
    }
}
