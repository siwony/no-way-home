import { ApiError } from "./api";
import type {
  BuildingLedgerFindingsFormState,
  ContractFormState,
  MarketPriceFormState,
  RegistryFindingsFormState,
} from "./types";

export type ValidationErrors = Record<string, string>;
export const DOCUMENT_INTAKE_UPLOAD_MAX_BYTES = 20 * 1024 * 1024;
export const DOCUMENT_INTAKE_UPLOAD_MAX_LABEL = "20MB(20MiB)";

function isNegativeNumber(value: string): boolean {
  return value.trim() !== "" && Number(value) < 0;
}

function documentIntakeUploadSizeMessage() {
  return `파일 용량이 너무 큽니다. 문서 자동 입력은 ${DOCUMENT_INTAKE_UPLOAD_MAX_LABEL} 이하 파일만 업로드할 수 있습니다.`;
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

export function validateDocumentIntakeUpload(documentType: "registry" | "lease-contract", file: File | null): ValidationErrors {
  const errors: ValidationErrors = {};
  if (!file) {
    errors.file = "업로드할 파일을 먼저 선택해 주세요.";
    return errors;
  }

  const lowerName = file.name.toLowerCase();
  const isPdf = file.type === "application/pdf" || lowerName.endsWith(".pdf");
  const isImage =
    file.type === "image/jpeg" ||
    file.type === "image/png" ||
    file.type === "image/webp" ||
    lowerName.endsWith(".jpg") ||
    lowerName.endsWith(".jpeg") ||
    lowerName.endsWith(".png") ||
    lowerName.endsWith(".webp");

  if (documentType === "registry" && !isPdf) {
    errors.file = "지원하지 않는 형식입니다. 등기부등본은 PDF만 등록할 수 있습니다.";
  }

  if (documentType === "lease-contract" && !isPdf && !isImage) {
    errors.file = "지원하지 않는 형식입니다. 임대차 계약서는 PDF, JPEG, PNG, WebP만 등록할 수 있습니다.";
  }

  if (file.size <= 0) {
    errors.file = "빈 파일은 업로드할 수 없습니다.";
  } else if (file.size > DOCUMENT_INTAKE_UPLOAD_MAX_BYTES) {
    errors.file = documentIntakeUploadSizeMessage();
  }

  return errors;
}

export function deriveDocumentIntakeUploadFailureMessage(error: unknown) {
  if (error instanceof ApiError) {
    return error.message;
  }

  if (error instanceof Error && error.message.includes("HTTP 413")) {
    return documentIntakeUploadSizeMessage();
  }

  return "문서를 업로드하지 못했습니다. 파일 형식과 상태를 다시 확인하세요.";
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

export function deriveNeutralGlobalMessage(userId: string, checkId: string | null) {
  if (!userId.trim()) {
    return "먼저 User ID를 적용하면 진단을 시작할 수 있습니다.";
  }

  return checkId ? "현재 User ID로 이 checkId를 다시 확인할 수 있습니다." : "User ID가 적용되었습니다. 진단을 시작할 수 있습니다.";
}
