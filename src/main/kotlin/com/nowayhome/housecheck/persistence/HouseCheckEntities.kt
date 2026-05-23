package com.nowayhome.housecheck.persistence

import com.nowayhome.housecheck.domain.AnalysisStatus
import com.nowayhome.housecheck.domain.ContractType
import com.nowayhome.housecheck.domain.DocumentType
import com.nowayhome.housecheck.domain.HousingType
import com.nowayhome.housecheck.domain.MarketPriceSourceKind
import com.nowayhome.housecheck.domain.RiskLevel
import com.nowayhome.housecheck.security.EncryptedStringAttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "house_check_request")
class HouseCheckRequestEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "owner_id", nullable = false, length = 120)
    var ownerId: String = "",
    @Column(name = "address_road", nullable = false)
    var addressRoad: String = "",
    @Column(name = "address_lot")
    var addressLot: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 40)
    var contractType: ContractType = ContractType.JEONSE,
    @Enumerated(EnumType.STRING)
    @Column(name = "housing_type", nullable = false, length = 40)
    var housingType: HousingType = HousingType.UNKNOWN,
    @Column(name = "deposit_amount", nullable = false)
    var depositAmount: Long = 0,
    @Column(name = "monthly_rent_amount", nullable = false)
    var monthlyRentAmount: Long = 0,
    @Column(name = "contract_planned_date", nullable = false)
    var contractPlannedDate: LocalDate = LocalDate.now(),
    @Column(name = "occupancy_planned_date", nullable = false)
    var occupancyPlannedDate: LocalDate = LocalDate.now(),
    @Convert(converter = EncryptedStringAttributeConverter::class)
    @Column(name = "landlord_name", nullable = false)
    var landlordName: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 30)
    var analysisStatus: AnalysisStatus = AnalysisStatus.NOT_RUN,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(
    name = "house_check_document",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_house_check_document_type", columnNames = ["house_check_id", "document_type"]),
        UniqueConstraint(name = "uk_house_check_document_storage_key", columnNames = ["storage_key"]),
    ],
)
class HouseCheckDocumentEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "house_check_id", nullable = false)
    var houseCheckId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    var documentType: DocumentType = DocumentType.REGISTRY,
    @Column(name = "original_file_name", nullable = false)
    var originalFileName: String = "",
    @Column(name = "mime_type", nullable = false, length = 120)
    var mimeType: String = "",
    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0,
    @Column(name = "storage_key", nullable = false)
    var storageKey: String = "",
    @Column(name = "issued_date")
    var issuedDate: LocalDate? = null,
    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "registry_manual_finding")
class RegistryManualFindingEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "house_check_id", nullable = false, unique = true)
    var houseCheckId: UUID = UUID.randomUUID(),
    @Column(name = "current_owner_name", nullable = false)
    var currentOwnerName: String = "",
    @Column(name = "owner_matches_landlord", nullable = false)
    var ownerMatchesLandlord: Boolean = false,
    @Column(name = "has_trust_registration", nullable = false)
    var hasTrustRegistration: Boolean = false,
    @Column(name = "has_seizure", nullable = false)
    var hasSeizure: Boolean = false,
    @Column(name = "has_provisional_seizure", nullable = false)
    var hasProvisionalSeizure: Boolean = false,
    @Column(name = "has_provisional_disposition", nullable = false)
    var hasProvisionalDisposition: Boolean = false,
    @Column(name = "has_auction_proceeding", nullable = false)
    var hasAuctionProceeding: Boolean = false,
    @Column(name = "has_lease_registration", nullable = false)
    var hasLeaseRegistration: Boolean = false,
    @Column(name = "has_mortgage", nullable = false)
    var hasMortgage: Boolean = false,
    @Column(name = "senior_debt_amount")
    var seniorDebtAmount: Long? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "building_ledger_manual_finding")
class BuildingLedgerManualFindingEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "house_check_id", nullable = false, unique = true)
    var houseCheckId: UUID = UUID.randomUUID(),
    @Column(name = "usage", nullable = false)
    var usage: String = "",
    @Column(name = "is_residential_use_confirmed", nullable = false)
    var isResidentialUseConfirmed: Boolean = false,
    @Column(name = "is_violation_building", nullable = false)
    var isViolationBuilding: Boolean = false,
    @Column(name = "is_unit_confirmed", nullable = false)
    var isUnitConfirmed: Boolean = false,
    @Column(name = "is_contract_area_consistent", nullable = false)
    var isContractAreaConsistent: Boolean = false,
    @Column(name = "approval_date")
    var approvalDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "housing_type_observed", length = 40)
    var housingTypeObserved: HousingType? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "market_price_snapshot")
class MarketPriceSnapshotEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "house_check_id", nullable = false, unique = true)
    var houseCheckId: UUID = UUID.randomUUID(),
    @Column(name = "estimated_market_value")
    var estimatedMarketValue: Long? = null,
    @Column(name = "estimated_jeonse_value")
    var estimatedJeonseValue: Long? = null,
    @Column(name = "source_label", nullable = false)
    var sourceLabel: String = "",
    @Column(name = "reference_date", nullable = false)
    var referenceDate: LocalDate = LocalDate.now(),
    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 40)
    var sourceKind: MarketPriceSourceKind = MarketPriceSourceKind.USER_ENTERED,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "house_risk_report")
class HouseRiskReportEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "house_check_id", nullable = false, unique = true)
    var houseCheckId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    var riskLevel: RiskLevel = RiskLevel.CAUTION,
    @Column(name = "summary", nullable = false, columnDefinition = "text")
    var summary: String = "",
    @Column(name = "registry_summary", nullable = false, columnDefinition = "text")
    var registrySummary: String = "",
    @Column(name = "building_summary", nullable = false, columnDefinition = "text")
    var buildingSummary: String = "",
    @Column(name = "deposit_summary", nullable = false, columnDefinition = "text")
    var depositSummary: String = "",
    @Column(name = "jeonse_ratio", precision = 7, scale = 2)
    var jeonseRatio: BigDecimal? = null,
    @Column(name = "total_exposure_ratio", precision = 7, scale = 2)
    var totalExposureRatio: BigDecimal? = null,
    @Column(name = "valuation_status", nullable = false, length = 30)
    var valuationStatus: String = "",
    @Column(name = "valuation_note", columnDefinition = "text")
    var valuationNote: String? = null,
    @Column(name = "recovery_status", nullable = false, length = 30)
    var recoveryStatus: String = "",
    @Column(name = "recovery_note", columnDefinition = "text")
    var recoveryNote: String? = null,
    @Column(name = "estimated_auction_value")
    var estimatedAuctionValue: Long? = null,
    @Column(name = "recoverable_deposit_amount")
    var recoverableDepositAmount: Long? = null,
    @Column(name = "shortfall_amount")
    var shortfallAmount: Long? = null,
    @Column(name = "generated_at", nullable = false)
    var generatedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "house_risk_reason")
class HouseRiskReasonEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "report_id", nullable = false)
    var reportId: UUID = UUID.randomUUID(),
    @Column(name = "reason_code", nullable = false, length = 80)
    var reasonCode: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    var riskLevel: RiskLevel = RiskLevel.CAUTION,
    @Column(name = "title", nullable = false)
    var title: String = "",
    @Column(name = "detail", nullable = false, columnDefinition = "text")
    var detail: String = "",
    @Column(name = "source_type", nullable = false, length = 40)
    var sourceType: String = "",
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,
)
