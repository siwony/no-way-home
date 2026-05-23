import type {
  BuildingLedgerFindingsFormState,
  ContractFormState,
  MarketPriceFormState,
  RegistryFindingsFormState,
} from "./types";

export type ValidationErrors = Record<string, string>;

function isNegativeNumber(value: string): boolean {
  return value.trim() !== "" && Number(value) < 0;
}

function requireBoolean(value: string, field: string, errors: ValidationErrors) {
  if (value !== "true" && value !== "false") {
    errors[field] = "예 또는 아니오를 선택하세요.";
  }
}

export function validateContract(form: ContractFormState): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!form.addressRoad.trim()) errors.addressRoad = "도로명 주소를 입력하세요.";
  if (!form.depositAmount.trim()) errors.depositAmount = "보증금을 입력하세요.";
  if (isNegativeNumber(form.depositAmount)) errors.depositAmount = "0 이상 금액만 입력하세요.";
  if (!form.monthlyRentAmount.trim()) errors.monthlyRentAmount = "월세를 입력하세요.";
  if (isNegativeNumber(form.monthlyRentAmount)) errors.monthlyRentAmount = "0 이상 금액만 입력하세요.";
  if (!form.contractPlannedDate) errors.contractPlannedDate = "계약 예정일을 입력하세요.";
  if (!form.occupancyPlannedDate) errors.occupancyPlannedDate = "입주 예정일을 입력하세요.";
  if (!form.landlordName.trim()) errors.landlordName = "임대인 이름을 입력하세요.";

  return errors;
}

export function validateUpload(file: File | null): ValidationErrors {
  const errors: ValidationErrors = {};
  if (!file) {
    errors.file = "PDF 파일을 선택하세요.";
    return errors;
  }

  const isPdf = file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");
  if (!isPdf) {
    errors.file = "PDF 파일만 업로드할 수 있습니다.";
  }

  if (file.size <= 0) {
    errors.file = "빈 파일은 업로드할 수 없습니다.";
  }

  return errors;
}

export function validateRegistryFindings(form: RegistryFindingsFormState): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!form.currentOwnerName.trim()) errors.currentOwnerName = "현재 소유자 이름을 입력하세요.";
  requireBoolean(form.ownerMatchesLandlord, "ownerMatchesLandlord", errors);
  requireBoolean(form.hasTrustRegistration, "hasTrustRegistration", errors);
  requireBoolean(form.hasSeizure, "hasSeizure", errors);
  requireBoolean(form.hasProvisionalSeizure, "hasProvisionalSeizure", errors);
  requireBoolean(form.hasProvisionalDisposition, "hasProvisionalDisposition", errors);
  requireBoolean(form.hasAuctionProceeding, "hasAuctionProceeding", errors);
  requireBoolean(form.hasLeaseRegistration, "hasLeaseRegistration", errors);
  requireBoolean(form.hasMortgage, "hasMortgage", errors);

  if (form.hasMortgage === "true") {
    if (!form.seniorDebtAmount.trim()) {
      errors.seniorDebtAmount = "선순위 채권 금액을 입력하세요.";
    } else if (isNegativeNumber(form.seniorDebtAmount)) {
      errors.seniorDebtAmount = "0 이상 금액만 입력하세요.";
    }
  }

  return errors;
}

export function validateBuildingLedgerFindings(form: BuildingLedgerFindingsFormState): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!form.usage.trim()) errors.usage = "공부상 용도를 입력하세요.";
  requireBoolean(form.isResidentialUseConfirmed, "isResidentialUseConfirmed", errors);
  requireBoolean(form.isViolationBuilding, "isViolationBuilding", errors);
  requireBoolean(form.isUnitConfirmed, "isUnitConfirmed", errors);
  requireBoolean(form.isContractAreaConsistent, "isContractAreaConsistent", errors);

  return errors;
}

export function validateMarketPrice(form: MarketPriceFormState): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!form.estimatedMarketValue.trim() && !form.estimatedJeonseValue.trim()) {
    errors.estimatedMarketValue = "추정 매매가 또는 전세 참고 금액 중 하나를 입력하세요.";
    errors.estimatedJeonseValue = "추정 매매가 또는 전세 참고 금액 중 하나를 입력하세요.";
  }

  if (isNegativeNumber(form.estimatedMarketValue)) {
    errors.estimatedMarketValue = "0 이상 금액만 입력하세요.";
  }

  if (isNegativeNumber(form.estimatedJeonseValue)) {
    errors.estimatedJeonseValue = "0 이상 금액만 입력하세요.";
  }

  if (!form.sourceLabel.trim()) errors.sourceLabel = "출처 메모를 입력하세요.";
  if (!form.referenceDate) errors.referenceDate = "기준일을 입력하세요.";

  return errors;
}

export function applyAccessDeniedReset(previousCheckId: string | null, nextUserId: string, currentUserId: string | null) {
  const userChanged = Boolean(previousCheckId && currentUserId && currentUserId !== nextUserId);

  return {
    shouldClearServerData: userChanged,
    accessMode: userChanged ? "boundary_reset" : "none",
  };
}
