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
