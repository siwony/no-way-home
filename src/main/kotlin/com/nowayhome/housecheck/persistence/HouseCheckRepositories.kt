package com.nowayhome.housecheck.persistence

import com.nowayhome.housecheck.domain.DocumentType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface HouseCheckRequestRepository : JpaRepository<HouseCheckRequestEntity, UUID>

interface HouseCheckDocumentRepository : JpaRepository<HouseCheckDocumentEntity, UUID> {
    fun findByHouseCheckIdAndDocumentType(houseCheckId: UUID, documentType: DocumentType): HouseCheckDocumentEntity?

    fun findAllByHouseCheckId(houseCheckId: UUID): List<HouseCheckDocumentEntity>
}

interface RegistryManualFindingRepository : JpaRepository<RegistryManualFindingEntity, UUID> {
    fun findByHouseCheckId(houseCheckId: UUID): RegistryManualFindingEntity?
}

interface BuildingLedgerManualFindingRepository : JpaRepository<BuildingLedgerManualFindingEntity, UUID> {
    fun findByHouseCheckId(houseCheckId: UUID): BuildingLedgerManualFindingEntity?
}

interface MarketPriceSnapshotRepository : JpaRepository<MarketPriceSnapshotEntity, UUID> {
    fun findByHouseCheckId(houseCheckId: UUID): MarketPriceSnapshotEntity?
}

interface HouseRiskReportRepository : JpaRepository<HouseRiskReportEntity, UUID> {
    fun findByHouseCheckId(houseCheckId: UUID): HouseRiskReportEntity?
}

interface HouseRiskReasonRepository : JpaRepository<HouseRiskReasonEntity, UUID> {
    fun findAllByReportIdOrderByDisplayOrderAsc(reportId: UUID): List<HouseRiskReasonEntity>

    fun deleteAllByReportId(reportId: UUID)
}
