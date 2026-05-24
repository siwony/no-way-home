package com.nowayhome.housecheck.api

import com.nowayhome.housecheck.application.CreateHouseCheckRequest
import com.nowayhome.housecheck.application.HouseCheckCommandService
import com.nowayhome.housecheck.application.HouseCheckQueryService
import com.nowayhome.housecheck.application.MarketPriceLookupService
import com.nowayhome.housecheck.application.SaveBuildingLedgerFindingsRequest
import com.nowayhome.housecheck.application.SaveMarketPriceRequest
import com.nowayhome.housecheck.application.SaveRegistryFindingsRequest
import com.nowayhome.housecheck.application.SectionStatusResponse
import com.nowayhome.housecheck.domain.DocumentType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/house-checks")
class HouseCheckController(
    private val houseCheckCommandService: HouseCheckCommandService,
    private val houseCheckQueryService: HouseCheckQueryService,
    private val marketPriceLookupService: MarketPriceLookupService,
) {
    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
        @Valid @RequestBody request: CreateHouseCheckRequest,
    ): SectionStatusResponse {
        return houseCheckCommandService.create(ownerId, request)
    }

    @PostMapping("/{checkId}/registry-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadRegistryFile(
        @PathVariable checkId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("issuedDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) issuedDate: LocalDate?,
    ): SectionStatusResponse {
        return houseCheckCommandService.uploadDocument(checkId, ownerId, DocumentType.REGISTRY, file, issuedDate)
    }

    @PostMapping("/{checkId}/building-ledger-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadBuildingLedgerFile(
        @PathVariable checkId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("issuedDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) issuedDate: LocalDate?,
    ): SectionStatusResponse {
        return houseCheckCommandService.uploadDocument(checkId, ownerId, DocumentType.BUILDING_LEDGER, file, issuedDate)
    }

    @PutMapping("/{checkId}/registry-findings")
    fun saveRegistryFindings(
        @PathVariable checkId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
        @Valid @RequestBody request: SaveRegistryFindingsRequest,
    ): SectionStatusResponse {
        return houseCheckCommandService.saveRegistryFindings(checkId, ownerId, request)
    }

    @PutMapping("/{checkId}/building-ledger-findings")
    fun saveBuildingLedgerFindings(
        @PathVariable checkId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
        @Valid @RequestBody request: SaveBuildingLedgerFindingsRequest,
    ): SectionStatusResponse {
        return houseCheckCommandService.saveBuildingLedgerFindings(checkId, ownerId, request)
    }

    @PostMapping("/{checkId}/market-price")
    fun saveMarketPrice(
        @PathVariable checkId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
        @Valid @RequestBody request: SaveMarketPriceRequest,
    ): SectionStatusResponse {
        return houseCheckCommandService.saveMarketPrice(checkId, ownerId, request)
    }

    @PostMapping("/{checkId}/market-price/lookup")
    fun lookupMarketPrice(
        @PathVariable checkId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
    ) = marketPriceLookupService.lookup(checkId, ownerId)

    @PostMapping("/{checkId}/analyze")
    fun analyze(
        @PathVariable checkId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
    ): SectionStatusResponse {
        return houseCheckCommandService.analyze(checkId, ownerId)
    }

    @GetMapping("/{checkId}/report")
    fun getReport(
        @PathVariable checkId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
    ) = houseCheckQueryService.getReport(checkId, ownerId)

    @GetMapping("/{checkId}/checklist")
    fun getChecklist(
        @PathVariable checkId: UUID,
        @RequestHeader("X-User-Id") @NotBlank ownerId: String,
    ) = houseCheckQueryService.getChecklist(checkId, ownerId)
}
