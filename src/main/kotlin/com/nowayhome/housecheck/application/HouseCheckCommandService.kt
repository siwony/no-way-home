package com.nowayhome.housecheck.application

import com.nowayhome.housecheck.domain.AnalysisStatus
import com.nowayhome.housecheck.domain.ContractType
import com.nowayhome.housecheck.domain.DocumentType
import com.nowayhome.housecheck.domain.HouseCheckException
import com.nowayhome.housecheck.domain.HousingType
import com.nowayhome.housecheck.persistence.BuildingLedgerManualFindingEntity
import com.nowayhome.housecheck.persistence.BuildingLedgerManualFindingRepository
import com.nowayhome.housecheck.persistence.HouseCheckDocumentEntity
import com.nowayhome.housecheck.persistence.HouseCheckDocumentRepository
import com.nowayhome.housecheck.persistence.HouseCheckRequestEntity
import com.nowayhome.housecheck.persistence.HouseCheckRequestRepository
import com.nowayhome.housecheck.persistence.MarketPriceSnapshotEntity
import com.nowayhome.housecheck.persistence.MarketPriceSnapshotRepository
import com.nowayhome.housecheck.persistence.RegistryManualFindingEntity
import com.nowayhome.housecheck.persistence.RegistryManualFindingRepository
import com.nowayhome.housecheck.storage.HouseCheckDocumentStorage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime
import java.util.UUID

@Service
class HouseCheckCommandService(
    private val houseCheckRequestRepository: HouseCheckRequestRepository,
    private val houseCheckDocumentRepository: HouseCheckDocumentRepository,
    private val registryManualFindingRepository: RegistryManualFindingRepository,
    private val buildingLedgerManualFindingRepository: BuildingLedgerManualFindingRepository,
    private val marketPriceSnapshotRepository: MarketPriceSnapshotRepository,
    private val houseCheckDocumentStorage: HouseCheckDocumentStorage,
    private val houseCheckAccessGuard: HouseCheckAccessGuard,
    private val houseRiskAnalysisService: HouseRiskAnalysisService,
) {
    @Transactional
    fun create(ownerId: String, request: CreateHouseCheckRequest): SectionStatusResponse {
        val depositAmount = request.depositAmount ?: throw HouseCheckException(HouseCheckErrorCode.VALIDATION_ERROR)
        val monthlyRentAmount = request.monthlyRentAmount ?: throw HouseCheckException(HouseCheckErrorCode.VALIDATION_ERROR)
        validateAmount(depositAmount, HouseCheckErrorCode.INVALID_DEPOSIT_AMOUNT)
        validateAmount(monthlyRentAmount, HouseCheckErrorCode.INVALID_MONTHLY_RENT_AMOUNT)

        val now = OffsetDateTime.now()
        val entity = houseCheckRequestRepository.save(
            HouseCheckRequestEntity(
                ownerId = ownerId,
                addressRoad = request.addressRoad.trim(),
                addressLot = request.addressLot?.trim()?.takeIf { it.isNotEmpty() },
                contractType = ContractType.fromApiValue(request.contractType),
                housingType = HousingType.fromApiValue(request.housingType),
                depositAmount = depositAmount,
                monthlyRentAmount = monthlyRentAmount,
                contractPlannedDate = request.contractPlannedDate ?: throw HouseCheckException(HouseCheckErrorCode.VALIDATION_ERROR),
                occupancyPlannedDate = request.occupancyPlannedDate ?: throw HouseCheckException(HouseCheckErrorCode.VALIDATION_ERROR),
                landlordName = request.landlordName.trim(),
                analysisStatus = AnalysisStatus.NOT_RUN,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return buildSectionStatus(entity)
    }

    @Transactional
    fun uploadDocument(
        checkId: UUID,
        ownerId: String,
        documentType: DocumentType,
        file: MultipartFile,
        issuedDate: java.time.LocalDate?,
    ): SectionStatusResponse {
        val houseCheck = houseCheckAccessGuard.getOwnedHouseCheck(checkId, ownerId)
        validatePdf(file)

        val existing = houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(checkId, documentType)
        val storedDocument = houseCheckDocumentStorage.store(checkId, documentType, file)
        existing?.storageKey?.let(houseCheckDocumentStorage::delete)

        val entity = existing ?: HouseCheckDocumentEntity(
            houseCheckId = checkId,
            documentType = documentType,
        )
        entity.originalFileName = file.originalFilename?.trim().orEmpty().ifBlank { "uploaded.pdf" }
        entity.mimeType = file.contentType?.trim().orEmpty().ifBlank { "application/pdf" }
        entity.fileSize = file.size
        entity.storageKey = storedDocument.storageKey
        entity.issuedDate = issuedDate
        entity.uploadedAt = OffsetDateTime.now()
        houseCheckDocumentRepository.save(entity)

        houseCheck.analysisStatus = AnalysisStatus.NOT_RUN
        houseCheck.updatedAt = OffsetDateTime.now()
        houseCheckRequestRepository.save(houseCheck)

        return buildSectionStatus(houseCheck)
    }

    @Transactional
    fun saveRegistryFindings(checkId: UUID, ownerId: String, request: SaveRegistryFindingsRequest): SectionStatusResponse {
        val houseCheck = houseCheckAccessGuard.getOwnedHouseCheck(checkId, ownerId)
        requireDocument(checkId, DocumentType.REGISTRY, HouseCheckErrorCode.REGISTRY_FILE_REQUIRED)
        request.seniorDebtAmount?.let { validateAmount(it, HouseCheckErrorCode.INVALID_MARKET_PRICE_AMOUNT) }

        val now = OffsetDateTime.now()
        val entity = registryManualFindingRepository.findByHouseCheckId(checkId) ?: RegistryManualFindingEntity(
            houseCheckId = checkId,
            createdAt = now,
        )
        entity.currentOwnerName = request.currentOwnerName.trim()
        entity.ownerMatchesLandlord = request.ownerMatchesLandlord
        entity.hasTrustRegistration = request.hasTrustRegistration
        entity.hasSeizure = request.hasSeizure
        entity.hasProvisionalSeizure = request.hasProvisionalSeizure
        entity.hasProvisionalDisposition = request.hasProvisionalDisposition
        entity.hasAuctionProceeding = request.hasAuctionProceeding
        entity.hasLeaseRegistration = request.hasLeaseRegistration
        entity.hasMortgage = request.hasMortgage
        entity.seniorDebtAmount = request.seniorDebtAmount
        entity.updatedAt = now
        registryManualFindingRepository.save(entity)

        houseCheck.analysisStatus = AnalysisStatus.NOT_RUN
        houseCheck.updatedAt = now
        houseCheckRequestRepository.save(houseCheck)
        return buildSectionStatus(houseCheck)
    }

    @Transactional
    fun saveBuildingLedgerFindings(checkId: UUID, ownerId: String, request: SaveBuildingLedgerFindingsRequest): SectionStatusResponse {
        val houseCheck = houseCheckAccessGuard.getOwnedHouseCheck(checkId, ownerId)
        requireDocument(checkId, DocumentType.BUILDING_LEDGER, HouseCheckErrorCode.BUILDING_LEDGER_FILE_REQUIRED)

        val now = OffsetDateTime.now()
        val entity = buildingLedgerManualFindingRepository.findByHouseCheckId(checkId) ?: BuildingLedgerManualFindingEntity(
            houseCheckId = checkId,
            createdAt = now,
        )
        entity.usage = request.usage.trim()
        entity.isResidentialUseConfirmed = request.isResidentialUseConfirmed
        entity.isViolationBuilding = request.isViolationBuilding
        entity.isUnitConfirmed = request.isUnitConfirmed
        entity.isContractAreaConsistent = request.isContractAreaConsistent
        entity.approvalDate = request.approvalDate
        entity.housingTypeObserved = request.toHousingTypeOrNull()
        entity.updatedAt = now
        buildingLedgerManualFindingRepository.save(entity)

        houseCheck.analysisStatus = AnalysisStatus.NOT_RUN
        houseCheck.updatedAt = now
        houseCheckRequestRepository.save(houseCheck)
        return buildSectionStatus(houseCheck)
    }

    @Transactional
    fun saveMarketPrice(checkId: UUID, ownerId: String, request: SaveMarketPriceRequest): SectionStatusResponse {
        val houseCheck = houseCheckAccessGuard.getOwnedHouseCheck(checkId, ownerId)
        request.validateAmounts()
        request.estimatedMarketValue?.let { validateAmount(it, HouseCheckErrorCode.INVALID_MARKET_PRICE_AMOUNT) }
        request.estimatedJeonseValue?.let { validateAmount(it, HouseCheckErrorCode.INVALID_MARKET_PRICE_AMOUNT) }
        request.sampleCount?.let { validateAmount(it.toLong(), HouseCheckErrorCode.INVALID_MARKET_PRICE_AMOUNT) }

        val now = OffsetDateTime.now()
        val entity = marketPriceSnapshotRepository.findByHouseCheckId(checkId) ?: MarketPriceSnapshotEntity(
            houseCheckId = checkId,
            createdAt = now,
        )
        entity.estimatedMarketValue = request.estimatedMarketValue
        entity.estimatedJeonseValue = request.estimatedJeonseValue
        entity.sourceLabel = request.sourceLabel.trim()
        entity.referenceDate = request.referenceDate ?: throw HouseCheckException(HouseCheckErrorCode.VALIDATION_ERROR)
        entity.sourceKind = request.sourceKindOrDefault()
        entity.sampleCount = request.sampleCount
        entity.lawdCode = request.lawdCode?.trim()?.takeIf { it.isNotEmpty() }
        entity.dealYmdFrom = request.dealYmdFrom?.trim()?.takeIf { it.isNotEmpty() }
        entity.dealYmdTo = request.dealYmdTo?.trim()?.takeIf { it.isNotEmpty() }
        entity.updatedAt = now
        marketPriceSnapshotRepository.save(entity)

        houseCheck.analysisStatus = AnalysisStatus.NOT_RUN
        houseCheck.updatedAt = now
        houseCheckRequestRepository.save(houseCheck)
        return buildSectionStatus(houseCheck)
    }

    @Transactional
    fun analyze(checkId: UUID, ownerId: String): SectionStatusResponse {
        val houseCheck = houseCheckAccessGuard.getOwnedHouseCheck(checkId, ownerId)
        houseRiskAnalysisService.analyze(houseCheck)
        return buildSectionStatus(houseCheckAccessGuard.getOwnedHouseCheck(checkId, ownerId))
    }

    fun buildSectionStatus(houseCheck: HouseCheckRequestEntity): SectionStatusResponse {
        val registryFile = houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(houseCheck.id, DocumentType.REGISTRY)
        val buildingFile = houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(houseCheck.id, DocumentType.BUILDING_LEDGER)
        val registryFinding = registryManualFindingRepository.findByHouseCheckId(houseCheck.id)
        val buildingFinding = buildingLedgerManualFindingRepository.findByHouseCheckId(houseCheck.id)
        val marketPrice = marketPriceSnapshotRepository.findByHouseCheckId(houseCheck.id)

        return SectionStatusResponse(
            checkId = houseCheck.id,
            registryFileStatus = if (registryFile == null) FileStatus.MISSING else FileStatus.UPLOADED,
            registryFindingStatus = if (registryFinding == null) FindingStatus.NOT_STARTED else FindingStatus.COMPLETED,
            buildingLedgerFileStatus = if (buildingFile == null) FileStatus.MISSING else FileStatus.UPLOADED,
            buildingLedgerFindingStatus = if (buildingFinding == null) FindingStatus.NOT_STARTED else FindingStatus.COMPLETED,
            marketPriceStatus = if (marketPrice == null) MarketPriceStatus.MISSING else MarketPriceStatus.SAVED,
            analysisStatus = houseCheck.analysisStatus,
            reportAvailability = if (houseCheck.analysisStatus == AnalysisStatus.COMPLETED) {
                ReportAvailability.AVAILABLE
            } else {
                ReportAvailability.NOT_READY
            },
        )
    }

    private fun requireDocument(checkId: UUID, documentType: DocumentType, errorCode: HouseCheckErrorCode) {
        if (houseCheckDocumentRepository.findByHouseCheckIdAndDocumentType(checkId, documentType) == null) {
            throw HouseCheckException(errorCode)
        }
    }

    private fun validatePdf(file: MultipartFile) {
        val contentType = file.contentType?.lowercase()
        val isPdfName = file.originalFilename?.lowercase()?.endsWith(".pdf") == true
        if (file.isEmpty || contentType != "application/pdf" || !isPdfName) {
            throw HouseCheckException(HouseCheckErrorCode.INVALID_FILE_TYPE)
        }
    }

    private fun validateAmount(amount: Long, errorCode: HouseCheckErrorCode) {
        if (amount < 0) {
            throw HouseCheckException(errorCode)
        }
    }
}
