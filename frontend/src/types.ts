export type AnalysisStatus = "NOT_RUN" | "RUNNING" | "COMPLETED" | "FAILED";
export type FileStatus = "MISSING" | "UPLOADED";
export type FindingStatus = "NOT_STARTED" | "COMPLETED";
export type MarketPriceStatus = "MISSING" | "SAVED";
export type ReportAvailability = "NOT_READY" | "AVAILABLE";
export type RiskLevel = "SAFE" | "CAUTION" | "DANGER" | "CRITICAL";
export type CalculationStatus = "AVAILABLE" | "NOT_AVAILABLE";
export type ReportValueSourceType = "USER_ENTERED" | "UPLOADED_FILE_METADATA" | "CALCULATED";
export type ChecklistStage = "BEFORE_CONTRACT" | "RIGHT_BEFORE_CONTRACT" | "AFTER_CONTRACT";

export type SectionStatusResponse = {
  checkId: string;
  registryFileStatus: FileStatus;
  registryFindingStatus: FindingStatus;
  buildingLedgerFileStatus: FileStatus;
  buildingLedgerFindingStatus: FindingStatus;
  marketPriceStatus: MarketPriceStatus;
  analysisStatus: AnalysisStatus;
  reportAvailability: ReportAvailability;
};

export type FieldErrorResponse = {
  field: string;
  reason: string;
};

export type ApiErrorResponse = {
  code: string;
  message: string;
  fieldErrors?: FieldErrorResponse[];
};

export type ValueField<T> = {
  value: T | null;
  sourceType: ReportValueSourceType;
  note?: string | null;
};

export type DecimalFieldResponse = ValueField<string | number> & {
  calculationStatus: CalculationStatus;
};

export type RiskReasonResponse = {
  code: string;
  riskLevel: RiskLevel;
  title: string;
  detail: string;
  sourceType: ReportValueSourceType;
};

export type RegistryReportSectionResponse = {
  summary: string;
  issuedDate?: ValueField<string> | null;
  currentOwnerName?: ValueField<string> | null;
  ownerMatchesLandlord?: ValueField<boolean> | null;
  seniorDebtAmount?: ValueField<number> | null;
};

export type BuildingLedgerReportSectionResponse = {
  summary: string;
  issuedDate?: ValueField<string> | null;
  usage?: ValueField<string> | null;
  isResidentialUseConfirmed?: ValueField<boolean> | null;
  isViolationBuilding?: ValueField<boolean> | null;
};

export type DepositRiskSectionResponse = {
  summary: string;
  estimatedMarketValue?: ValueField<number> | null;
  estimatedJeonseValue?: ValueField<number> | null;
  sourceLabel?: ValueField<string> | null;
  referenceDate?: ValueField<string> | null;
  jeonseRatio: DecimalFieldResponse;
  totalExposureRatio: DecimalFieldResponse;
};

export type RecoverySimulationResponse = {
  estimatedAuctionValue?: ValueField<number> | null;
  recoverableDepositAmount?: ValueField<number> | null;
  shortfallAmount?: ValueField<number> | null;
  calculationStatus: CalculationStatus;
  note?: string | null;
};

export type HouseRiskReportResponse = {
  checkId: string;
  generatedAt: string;
  riskLevel: RiskLevel;
  summary: string;
  sectionStatus: SectionStatusResponse;
  coreReasons: RiskReasonResponse[];
  registry: RegistryReportSectionResponse;
  buildingLedger: BuildingLedgerReportSectionResponse;
  depositRisk: DepositRiskSectionResponse;
  recoverySimulation: RecoverySimulationResponse;
  additionalChecks: string[];
};

export type ChecklistSectionResponse = {
  stage: ChecklistStage;
  title: string;
  items: string[];
};

export type HouseChecklistResponse = {
  checkId: string;
  analysisStatus: AnalysisStatus;
  sections: ChecklistSectionResponse[];
};

export type ContractType = "JEONSE" | "MONTHLY_RENT";
export type HousingType =
  | "APARTMENT"
  | "OFFICETEL"
  | "VILLA"
  | "MULTI_HOUSEHOLD"
  | "MULTI_FAMILY"
  | "UNKNOWN";

export type ContractFormState = {
  addressRoad: string;
  addressLot: string;
  contractType: ContractType;
  housingType: HousingType;
  depositAmount: string;
  monthlyRentAmount: string;
  contractPlannedDate: string;
  occupancyPlannedDate: string;
  landlordName: string;
};

export type UploadFormState = {
  issuedDate: string;
  selectedFileName: string;
};

export type RegistryFindingsFormState = {
  currentOwnerName: string;
  ownerMatchesLandlord: string;
  hasTrustRegistration: string;
  hasSeizure: string;
  hasProvisionalSeizure: string;
  hasProvisionalDisposition: string;
  hasAuctionProceeding: string;
  hasLeaseRegistration: string;
  hasMortgage: string;
  seniorDebtAmount: string;
};

export type BuildingLedgerFindingsFormState = {
  usage: string;
  isResidentialUseConfirmed: string;
  isViolationBuilding: string;
  isUnitConfirmed: string;
  isContractAreaConsistent: string;
  approvalDate: string;
  housingTypeObserved: string;
};

export type MarketPriceFormState = {
  estimatedMarketValue: string;
  estimatedJeonseValue: string;
  sourceLabel: string;
  referenceDate: string;
};

export type DocumentIntakeDocumentType = "REGISTRY" | "LEASE_CONTRACT";
export type DocumentIntakeProcessingStatus = "UPLOADED" | "EXTRACTING" | "REVIEW_REQUIRED" | "APPROVED" | "FAILED" | "DELETED";
export type DocumentIntakeFieldReviewStatus = "REVIEW_REQUIRED" | "APPROVED" | "EDITED" | "EXCLUDED";
export type DocumentIntakeWarningType = "ADDRESS_MISMATCH" | "LANDLORD_OWNER_MISMATCH" | "DEPOSIT_MISMATCH";
export type DocumentIntakeReviewAction = "APPROVE" | "EDIT" | "EXCLUDE";
export type DocumentIntakeFieldKey =
  | "LEASE_ADDRESS_ROAD"
  | "LEASE_ADDRESS_LOT"
  | "LEASE_CONTRACT_TYPE"
  | "LEASE_DEPOSIT_AMOUNT"
  | "LEASE_MONTHLY_RENT_AMOUNT"
  | "LEASE_CONTRACT_DATE"
  | "LEASE_OCCUPANCY_DATE"
  | "LEASE_LANDLORD_NAME"
  | "LEASE_SPECIAL_TERMS"
  | "REGISTRY_ADDRESS"
  | "REGISTRY_ISSUED_DATE"
  | "REGISTRY_OWNER_NAME"
  | "REGISTRY_HAS_TRUST_REGISTRATION"
  | "REGISTRY_HAS_SEIZURE"
  | "REGISTRY_HAS_PROVISIONAL_SEIZURE"
  | "REGISTRY_HAS_PROVISIONAL_DISPOSITION"
  | "REGISTRY_HAS_AUCTION_PROCEEDING"
  | "REGISTRY_HAS_LEASE_REGISTRATION"
  | "REGISTRY_HAS_MORTGAGE"
  | "REGISTRY_SENIOR_DEBT_AMOUNT";

export type DocumentIntakeDocumentFailureResponse = {
  code: string;
  message: string;
};

export type DocumentIntakeDocumentResponse = {
  documentType: DocumentIntakeDocumentType;
  processingStatus: DocumentIntakeProcessingStatus;
  fileName: string | null;
  mimeType: string | null;
  uploadedAt: string | null;
  processedAt: string | null;
  failure: DocumentIntakeDocumentFailureResponse | null;
};

export type DocumentIntakeFieldResponse = {
  fieldKey: DocumentIntakeFieldKey;
  value: string | null;
  sourceDocument: DocumentIntakeDocumentType;
  sourcePage: number | null;
  sourceText: string | null;
  confidence: number;
  reviewStatus: DocumentIntakeFieldReviewStatus;
};

export type DocumentIntakeWarningResponse = {
  type: DocumentIntakeWarningType;
  message: string;
  relatedFields: DocumentIntakeFieldKey[];
};

export type DocumentIntakeSessionResponse = {
  sessionId: string;
  documents: DocumentIntakeDocumentResponse[];
  fields: DocumentIntakeFieldResponse[];
  warnings: DocumentIntakeWarningResponse[];
  createdAt: string;
  updatedAt: string;
  expiresAt: string;
};

export type DocumentIntakeContractFormPayload = {
  addressRoad: string | null;
  addressLot: string | null;
  contractType: ContractType | null;
  depositAmount: number | null;
  monthlyRentAmount: number | null;
  contractPlannedDate: string | null;
  occupancyPlannedDate: string | null;
  landlordName: string | null;
};

export type DocumentIntakeRegistryFindingsPayload = {
  currentOwnerName: string | null;
  ownerMatchesLandlord: boolean | null;
  hasTrustRegistration: boolean | null;
  hasSeizure: boolean | null;
  hasProvisionalSeizure: boolean | null;
  hasProvisionalDisposition: boolean | null;
  hasAuctionProceeding: boolean | null;
  hasLeaseRegistration: boolean | null;
  hasMortgage: boolean | null;
  seniorDebtAmount: number | null;
};

export type DocumentIntakeApplicationPayloadResponse = {
  sessionId: string;
  approvedFieldCount: number;
  contractForm: DocumentIntakeContractFormPayload;
  registryFindingsForm: DocumentIntakeRegistryFindingsPayload;
};
