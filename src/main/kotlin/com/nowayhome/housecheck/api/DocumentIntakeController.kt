package com.nowayhome.housecheck.api

import com.nowayhome.housecheck.application.DocumentIntakeCommandService
import com.nowayhome.housecheck.application.DocumentIntakeQueryService
import com.nowayhome.housecheck.application.ReviewDocumentIntakeFieldRequest
import com.nowayhome.housecheck.domain.DocumentIntakeDocumentType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/document-intakes")
class DocumentIntakeController(
    private val documentIntakeCommandService: DocumentIntakeCommandService,
    private val documentIntakeQueryService: DocumentIntakeQueryService,
) {
    @PostMapping
    fun createSession(
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
    ) = documentIntakeCommandService.createSession(ownerId)

    @PostMapping("/{sessionId}/documents/{documentType}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadDocument(
        @PathVariable sessionId: UUID,
        @PathVariable documentType: String,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
        @RequestPart("file") file: MultipartFile,
    ) = documentIntakeCommandService.uploadDocument(
        sessionId = sessionId,
        ownerId = ownerId,
        documentType = DocumentIntakeDocumentType.fromPathValue(documentType),
        file = file,
    )

    @GetMapping("/{sessionId}")
    fun getSession(
        @PathVariable sessionId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
    ) = documentIntakeQueryService.getSession(sessionId, ownerId)

    @PutMapping("/{sessionId}/fields/{fieldKey}")
    fun reviewField(
        @PathVariable sessionId: UUID,
        @PathVariable fieldKey: String,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
        @Valid @RequestBody request: ReviewDocumentIntakeFieldRequest,
    ) = documentIntakeCommandService.reviewField(sessionId, ownerId, fieldKey, request)

    @GetMapping("/{sessionId}/application-payload")
    fun getApplicationPayload(
        @PathVariable sessionId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
    ) = documentIntakeQueryService.getApplicationPayload(sessionId, ownerId)

    @DeleteMapping("/{sessionId}/documents/{documentType}")
    fun deleteDocument(
        @PathVariable sessionId: UUID,
        @PathVariable documentType: String,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
    ) = documentIntakeCommandService.deleteDocument(
        sessionId = sessionId,
        ownerId = ownerId,
        documentType = DocumentIntakeDocumentType.fromPathValue(documentType),
    )
}
