import { describe, expect, it } from "vitest";
import { applyAccessDeniedReset, validateContract, validateMarketPrice, validateRegistryFindings } from "./validation";

describe("validation", () => {
  it("requires at least one market price amount", () => {
    const errors = validateMarketPrice({
      estimatedMarketValue: "",
      estimatedJeonseValue: "",
      sourceLabel: "사용자 확인",
      referenceDate: "2026-05-24",
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

  it("clears server data when a different user id is applied to an existing check", () => {
    const result = applyAccessDeniedReset("check-1", "owner-b", "owner-a");

    expect(result.shouldClearServerData).toBe(true);
    expect(result.accessMode).toBe("boundary_reset");
  });
});
