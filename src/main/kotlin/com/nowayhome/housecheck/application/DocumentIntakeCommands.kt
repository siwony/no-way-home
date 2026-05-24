package com.nowayhome.housecheck.application

import jakarta.validation.constraints.NotBlank

data class ReviewDocumentIntakeFieldRequest(
    @field:NotBlank
    val action: String,
    val editedValue: String? = null,
)

enum class DocumentIntakeReviewAction {
    APPROVE,
    EDIT,
    EXCLUDE,
    ;

    companion object {
        fun fromValue(value: String): DocumentIntakeReviewAction {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw com.nowayhome.housecheck.domain.HouseCheckException(HouseCheckErrorCode.DOCUMENT_INTAKE_INVALID_REVIEW_ACTION)
        }
    }
}
