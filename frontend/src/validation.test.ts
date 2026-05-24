import { ApiError } from "./api";
import { describe, expect, it } from "vitest";
import {
  applyAccessDeniedReset,
  deriveDocumentIntakeUploadFailureMessage,
  DOCUMENT_INTAKE_UPLOAD_MAX_BYTES,
  deriveNeutralGlobalMessage,
  validateContract,
  validateDocumentIntakeUpload,
  validateMarketPrice,
  validateRegistryFindings,
} from "./validation";

function createFile(name: string, type: string, size: number) {
  const file = new File(["stub"], name, { type });
  Object.defineProperty(file, "size", { value: size });
  return file;
}

describe("validation", () => {
  it("requires at least one market price amount", () => {
    const errors = validateMarketPrice({
      estimatedMarketValue: "",
      estimatedJeonseValue: "",
      sourceLabel: "사용자 확인",
      referenceDate: "2026-05-24",
      sourceKind: "USER_ENTERED",
      sampleCount: "",
      lawdCode: "",
      dealYmdFrom: "",
      dealYmdTo: "",
    });

    expect(errors.estimatedMarketValue).toContain("하나");
    expect(errors.estimatedJeonseValue).toContain("하나");
  });

  it("validates contract required fields and negative amounts", () => {
    const errors = validateContract({
      addressRoad: "",
      addressLot: "",
      contractType: "JEONSE",
      housingType: "APARTMENT",
      depositAmount: "-1",
      monthlyRentAmount: "",
      contractPlannedDate: "",
      occupancyPlannedDate: "",
      landlordName: "",
    });

    expect(errors.addressRoad).toBeDefined();
    expect(errors.depositAmount).toContain("0 이상");
    expect(errors.monthlyRentAmount).toBeDefined();
    expect(errors.landlordName).toBeDefined();
  });

  it("requires senior debt amount when mortgage exists", () => {
    const errors = validateRegistryFindings({
      currentOwnerName: "임대인",
      ownerMatchesLandlord: "true",
      hasTrustRegistration: "false",
      hasSeizure: "false",
      hasProvisionalSeizure: "false",
      hasProvisionalDisposition: "false",
      hasAuctionProceeding: "false",
      hasLeaseRegistration: "false",
      hasMortgage: "true",
      seniorDebtAmount: "",
    });

    expect(errors.seniorDebtAmount).toContain("선순위 채권");
  });

  it("validates registry auto-fill uploads as pdf only", () => {
    const errors = validateDocumentIntakeUpload(
      "registry",
      new File(["fake"], "registry.png", { type: "image/png" }),
    );

    expect(errors.file).toContain("PDF");
  });

  it("allows supported lease contract image uploads", () => {
    const errors = validateDocumentIntakeUpload(
      "lease-contract",
      new File(["fake"], "lease.webp", { type: "image/webp" }),
    );

    expect(errors.file).toBeUndefined();
  });

  it("allows production-sized registry and lease files under the document intake limit", () => {
    const registryErrors = validateDocumentIntakeUpload(
      "registry",
      createFile("registry.pdf", "application/pdf", 11 * 1024 * 1024),
    );
    const leaseErrors = validateDocumentIntakeUpload(
      "lease-contract",
      createFile("lease.pdf", "application/pdf", Math.round(9.7 * 1024 * 1024)),
    );

    expect(registryErrors.file).toBeUndefined();
    expect(leaseErrors.file).toBeUndefined();
  });

  it("rejects document intake uploads larger than 20 MiB before upload", () => {
    const errors = validateDocumentIntakeUpload(
      "registry",
      createFile("registry.pdf", "application/pdf", DOCUMENT_INTAKE_UPLOAD_MAX_BYTES + 1),
    );

    expect(errors.file).toContain("20MB");
    expect(errors.file).toContain("이하");
  });

  it("keeps the server message for document intake ApiError uploads", () => {
    const error = new ApiError(413, {
      code: "DOCUMENT_INTAKE_FILE_TOO_LARGE",
      message: "문서 자동 입력은 20MB 이하 파일만 업로드할 수 있습니다.",
    });

    expect(deriveDocumentIntakeUploadFailureMessage(error)).toBe(error.message);
  });

  it("maps bare HTTP 413 upload failures to the size guidance message", () => {
    expect(deriveDocumentIntakeUploadFailureMessage(new Error("HTTP 413"))).toContain("20MB");
  });

  it("clears server data when a different user id is applied to an existing check", () => {
    const result = applyAccessDeniedReset("check-1", "owner-b", "owner-a");

    expect(result.shouldClearServerData).toBe(true);
    expect(result.accessMode).toBe("boundary_reset");
  });

  it("derives the neutral banner message from restored session state", () => {
    expect(deriveNeutralGlobalMessage("", null)).toBe("먼저 User ID를 적용하면 진단을 시작할 수 있습니다.");
    expect(deriveNeutralGlobalMessage("owner-a", null)).toBe("User ID가 적용되었습니다. 진단을 시작할 수 있습니다.");
    expect(deriveNeutralGlobalMessage("owner-a", "check-1")).toBe("현재 User ID로 이 checkId를 다시 확인할 수 있습니다.");
  });
});
