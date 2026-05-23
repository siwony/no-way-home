import type {
  ApiErrorResponse,
  BuildingLedgerFindingsFormState,
  ContractFormState,
  HouseChecklistResponse,
  HouseRiskReportResponse,
  MarketPriceFormState,
  RegistryFindingsFormState,
  SectionStatusResponse,
} from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

export class ApiError extends Error {
  status: number;
  code: string;
  fieldErrors: ApiErrorResponse["fieldErrors"];

  constructor(status: number, payload: ApiErrorResponse) {
    super(payload.message);
    this.name = "ApiError";
    this.status = status;
    this.code = payload.code;
    this.fieldErrors = payload.fieldErrors || [];
  }
}

type RequestInitWithJson = RequestInit & {
  body?: BodyInit | null;
};

async function request<T>(path: string, userId: string, init: RequestInitWithJson = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "X-User-Id": userId,
      ...(init.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      ...init.headers,
    },
  });

  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as ApiErrorResponse | null;
    if (payload && typeof payload.code === "string") {
      throw new ApiError(response.status, payload);
    }

    throw new Error(`HTTP ${response.status}`);
  }

  return (await response.json()) as T;
}

function toNumber(value: string): number | null {
  if (!value.trim()) {
    return null;
  }

  return Number(value);
}

function toBoolean(value: string): boolean {
  return value === "true";
}

export const houseCheckApi = {
  create(userId: string, form: ContractFormState) {
    return request<SectionStatusResponse>("/house-checks", userId, {
      method: "POST",
      body: JSON.stringify({
        addressRoad: form.addressRoad.trim(),
        addressLot: form.addressLot.trim() || null,
        contractType: form.contractType,
        housingType: form.housingType,
        depositAmount: Number(form.depositAmount),
        monthlyRentAmount: Number(form.monthlyRentAmount || "0"),
        contractPlannedDate: form.contractPlannedDate,
        occupancyPlannedDate: form.occupancyPlannedDate,
        landlordName: form.landlordName.trim(),
      }),
    });
  },

  uploadDocument(
    userId: string,
    checkId: string,
    kind: "registry-file" | "building-ledger-file",
    file: File,
    issuedDate: string,
  ) {
    const formData = new FormData();
    formData.append("file", file);
    if (issuedDate) {
      formData.append("issuedDate", issuedDate);
    }

    return request<SectionStatusResponse>(`/house-checks/${checkId}/${kind}`, userId, {
      method: "POST",
      body: formData,
    });
  },

  saveRegistryFindings(userId: string, checkId: string, form: RegistryFindingsFormState) {
    return request<SectionStatusResponse>(`/house-checks/${checkId}/registry-findings`, userId, {
      method: "PUT",
      body: JSON.stringify({
        currentOwnerName: form.currentOwnerName.trim(),
        ownerMatchesLandlord: toBoolean(form.ownerMatchesLandlord),
        hasTrustRegistration: toBoolean(form.hasTrustRegistration),
        hasSeizure: toBoolean(form.hasSeizure),
        hasProvisionalSeizure: toBoolean(form.hasProvisionalSeizure),
        hasProvisionalDisposition: toBoolean(form.hasProvisionalDisposition),
        hasAuctionProceeding: toBoolean(form.hasAuctionProceeding),
        hasLeaseRegistration: toBoolean(form.hasLeaseRegistration),
        hasMortgage: toBoolean(form.hasMortgage),
        seniorDebtAmount: Number(form.seniorDebtAmount || "0"),
      }),
    });
  },

  saveBuildingLedgerFindings(userId: string, checkId: string, form: BuildingLedgerFindingsFormState) {
    return request<SectionStatusResponse>(`/house-checks/${checkId}/building-ledger-findings`, userId, {
      method: "PUT",
      body: JSON.stringify({
        usage: form.usage.trim(),
        isResidentialUseConfirmed: toBoolean(form.isResidentialUseConfirmed),
        isViolationBuilding: toBoolean(form.isViolationBuilding),
        isUnitConfirmed: toBoolean(form.isUnitConfirmed),
        isContractAreaConsistent: toBoolean(form.isContractAreaConsistent),
        approvalDate: form.approvalDate || null,
        housingTypeObserved: form.housingTypeObserved || null,
      }),
    });
  },

  saveMarketPrice(userId: string, checkId: string, form: MarketPriceFormState) {
    return request<SectionStatusResponse>(`/house-checks/${checkId}/market-price`, userId, {
      method: "POST",
      body: JSON.stringify({
        estimatedMarketValue: toNumber(form.estimatedMarketValue),
        estimatedJeonseValue: toNumber(form.estimatedJeonseValue),
        sourceLabel: form.sourceLabel.trim(),
        referenceDate: form.referenceDate,
      }),
    });
  },

  analyze(userId: string, checkId: string) {
    return request<SectionStatusResponse>(`/house-checks/${checkId}/analyze`, userId, {
      method: "POST",
    });
  },

  getReport(userId: string, checkId: string) {
    return request<HouseRiskReportResponse>(`/house-checks/${checkId}/report`, userId);
  },

  getChecklist(userId: string, checkId: string) {
    return request<HouseChecklistResponse>(`/house-checks/${checkId}/checklist`, userId);
  },
};
