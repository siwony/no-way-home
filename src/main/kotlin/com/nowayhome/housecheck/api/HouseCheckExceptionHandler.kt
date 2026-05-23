package com.nowayhome.housecheck.api

import com.nowayhome.housecheck.application.ErrorResponse
import com.nowayhome.housecheck.application.FieldErrorResponse
import com.nowayhome.housecheck.application.HouseCheckErrorCode
import com.nowayhome.housecheck.domain.HouseCheckException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException

@RestControllerAdvice
class HouseCheckExceptionHandler {
    @ExceptionHandler(HouseCheckException::class)
    fun handleHouseCheckException(exception: HouseCheckException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(statusFor(exception.errorCode))
            .body(ErrorResponse(exception.errorCode, exception.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                code = HouseCheckErrorCode.VALIDATION_ERROR,
                message = HouseCheckErrorCode.VALIDATION_ERROR.defaultMessage,
                fieldErrors = exception.bindingResult.fieldErrors.map {
                    FieldErrorResponse(field = it.field, reason = it.defaultMessage ?: "invalid")
                },
            ),
        )
    }

    @ExceptionHandler(ConstraintViolationException::class, HandlerMethodValidationException::class)
    fun handleConstraintViolation(exception: Exception): ResponseEntity<ErrorResponse> {
        val fields = when (exception) {
            is ConstraintViolationException -> exception.constraintViolations.map {
                FieldErrorResponse(field = it.propertyPath.toString(), reason = it.message)
            }
            is HandlerMethodValidationException -> exception.parameterValidationResults.flatMap { result ->
                result.resolvableErrors.map {
                    FieldErrorResponse(field = result.methodParameter.parameterName ?: "request", reason = it.defaultMessage ?: "invalid")
                }
            }
            else -> emptyList()
        }
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                code = HouseCheckErrorCode.VALIDATION_ERROR,
                message = HouseCheckErrorCode.VALIDATION_ERROR.defaultMessage,
                fieldErrors = fields,
            ),
        )
    }

    private fun statusFor(errorCode: HouseCheckErrorCode): HttpStatus {
        return when (errorCode) {
            HouseCheckErrorCode.HOUSE_CHECK_NOT_FOUND -> HttpStatus.NOT_FOUND
            HouseCheckErrorCode.ACCESS_DENIED -> HttpStatus.FORBIDDEN
            HouseCheckErrorCode.ANALYSIS_NOT_READY -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
    }
}
