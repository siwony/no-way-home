import { describe, expect, it } from "vitest";
import { buildDocumentApplyPreview } from "./documentIntake";
import type { ContractFormState, DocumentIntakeApplicationPayloadResponse, DocumentIntakeSessionResponse, RegistryFindingsFormState } from "./types";

const contractForm: ContractFormState = {
  addressRoad: "서울시 마포구 양화로 1",
  addressLot: "",
  contractType: "JEONSE",
  housingType: "APARTMENT",
  depositAmount: "60000000",
  monthlyRentAmount: "0",
  contractPlannedDate: "",
  occupancyPlannedDate: "",
  landlordName: "기존 임대인",
};

const registryFindingsForm: RegistryFindingsFormState = {
  currentOwnerName: "",
  ownerMatchesLandlord: "",
  hasTrustRegistration: "",
  hasSeizure: "",
  hasProvisionalSeizure: "",
  hasProvisionalDisposition: "",
  hasAuctionProceeding: "",
  hasLeaseRegistration: "",
  hasMortgage: "",
  seniorDebtAmount: "0",
};

const session: DocumentIntakeSessionResponse = {
  sessionId: "session-1",
  documents: [
    {
      documentType: "REGISTRY",
      processingStatus: "REVIEW_REQUIRED",
      fileName: "registry.pdf",
      mimeType: "application/pdf",
      uploadedAt: "2026-05-24T10:00:00Z",
      processedAt: "2026-05-24T10:00:01Z",
      failure: null,
    },
    {
      documentType: "LEASE_CONTRACT",
      processingStatus: "REVIEW_REQUIRED",
      fileName: "lease.pdf",
      mimeType: "application/pdf",
      uploadedAt: "2026-05-24T10:00:00Z",
      processedAt: "2026-05-24T10:00:01Z",
      failure: null,
    },
  ],
  fields: [
    {
      fieldKey: "LEASE_ADDRESS_ROAD",
      value: "서울시 강서구 공항대로 9",
      sourceDocument: "LEASE_CONTRACT",
      sourcePage: 1,
      sourceText: "소재지 서울시 강서구 공항대로 9",
      confidence: 0.92,
      reviewStatus: "APPROVED",
    },
    {
      fieldKey: "LEASE_LANDLORD_NAME",
      value: "다른 임대인",
      sourceDocument: "LEASE_CONTRACT",
      sourcePage: 4,
      sourceText: "임대인 다른 임대인",
      confidence: 0.83,
      reviewStatus: "APPROVED",
    },
    {
      fieldKey: "REGISTRY_OWNER_NAME",
      value: "다른 소유자",
      sourceDocument: "REGISTRY",
      sourcePage: 2,
      sourceText: "소유자 다른 소유자",
      confidence: 0.91,
      reviewStatus: "APPROVED",
    },
  ],
  warnings: [],
  createdAt: "2026-05-24T10:00:00Z",
  updatedAt: "2026-05-24T10:00:02Z",
  expiresAt: "2026-05-25T10:00:00Z",
};

const payload: DocumentIntakeApplicationPayloadResponse = {
  sessionId: "session-1",
  approvedFieldCount: 3,
  contractForm: {
    addressRoad: "서울시 강서구 공항대로 9",
    addressLot: null,
    contractType: null,
    depositAmount: null,
    monthlyRentAmount: null,
    contractPlannedDate: null,
    occupancyPlannedDate: null,
    landlordName: "다른 임대인",
  },
  registryFindingsForm: {
    currentOwnerName: "다른 소유자",
    ownerMatchesLandlord: false,
    hasTrustRegistration: null,
    hasSeizure: null,
    hasProvisionalSeizure: null,
    hasProvisionalDisposition: null,
    hasAuctionProceeding: null,
    hasLeaseRegistration: null,
    hasMortgage: null,
    seniorDebtAmount: null,
  },
};

describe("document intake preview", () => {
  it("builds overwrite summaries and source notes", () => {
    const preview = buildDocumentApplyPreview(session, payload, contractForm, registryFindingsForm);

    expect(preview.items).toHaveLength(4);
    expect(preview.conflictCount).toBe(2);
    expect(preview.blankCount).toBe(2);

    const addressItem = preview.items.find((item) => item.id === "contract.addressRoad");
    expect(addressItem?.conflict).toBe(true);
    expect(addressItem?.sourceNote).toContain("임대차 계약서 1페이지");

    const currentOwnerItem = preview.items.find((item) => item.id === "registry.currentOwnerName");
    expect(currentOwnerItem?.conflict).toBe(false);

    const ownerMatchItem = preview.items.find((item) => item.id === "registry.ownerMatchesLandlord");
    expect(ownerMatchItem?.sourceNote).toContain("문서 비교");
  });
});
