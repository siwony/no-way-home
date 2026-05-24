import type {
  ContractFormState,
  DocumentIntakeApplicationPayloadResponse,
  DocumentIntakeDocumentType,
  DocumentIntakeFieldKey,
  DocumentIntakeFieldResponse,
  DocumentIntakeSessionResponse,
  RegistryFindingsFormState,
} from "./types";

export type DocumentApplyTarget =
  | { form: "contract"; field: keyof ContractFormState }
  | { form: "registry"; field: keyof RegistryFindingsFormState };

export type DocumentApplyPreviewItem = {
  id: string;
  label: string;
  target: DocumentApplyTarget;
  currentValue: string;
  incomingValue: string;
  conflict: boolean;
  sameAsCurrent: boolean;
  applyByDefault: boolean;
  fieldKeys: DocumentIntakeFieldKey[];
  sourceNote: string;
};

export type DocumentApplyPreview = {
  sessionId: string;
  approvedFieldCount: number;
  items: DocumentApplyPreviewItem[];
  conflictCount: number;
  blankCount: number;
  sameCount: number;
};

type PreviewConfig = {
  id: string;
  label: string;
  target: DocumentApplyTarget;
  readPayloadValue: (payload: DocumentIntakeApplicationPayloadResponse) => string | null;
  fieldKeys: DocumentIntakeFieldKey[];
  customSourceNote?: (fields: DocumentIntakeFieldResponse[]) => string;
};

const documentTypeLabels: Record<DocumentIntakeDocumentType, string> = {
  REGISTRY: "등기부등본",
  LEASE_CONTRACT: "임대차 계약서",
};

export const documentFieldLabels: Record<DocumentIntakeFieldKey, string> = {
  LEASE_ADDRESS_ROAD: "도로명 주소",
  LEASE_ADDRESS_LOT: "지번 주소",
  LEASE_CONTRACT_TYPE: "계약 유형",
  LEASE_DEPOSIT_AMOUNT: "보증금",
  LEASE_MONTHLY_RENT_AMOUNT: "월세",
  LEASE_CONTRACT_DATE: "계약 예정일",
  LEASE_OCCUPANCY_DATE: "입주 예정일",
  LEASE_LANDLORD_NAME: "임대인 이름",
  LEASE_SPECIAL_TERMS: "특약 핵심 문장 후보",
  REGISTRY_ADDRESS: "등기 주소",
  REGISTRY_ISSUED_DATE: "등기부등본 발급일",
  REGISTRY_OWNER_NAME: "현재 소유자 이름",
  REGISTRY_HAS_TRUST_REGISTRATION: "신탁등기 여부",
  REGISTRY_HAS_SEIZURE: "압류 여부",
  REGISTRY_HAS_PROVISIONAL_SEIZURE: "가압류 여부",
  REGISTRY_HAS_PROVISIONAL_DISPOSITION: "가처분 여부",
  REGISTRY_HAS_AUCTION_PROCEEDING: "경매개시결정 여부",
  REGISTRY_HAS_LEASE_REGISTRATION: "임차권등기 여부",
  REGISTRY_HAS_MORTGAGE: "근저당 여부",
  REGISTRY_SENIOR_DEBT_AMOUNT: "선순위 채권 금액",
};

const previewConfigs: PreviewConfig[] = [
  {
    id: "contract.addressRoad",
    label: "도로명 주소",
    target: { form: "contract", field: "addressRoad" },
    readPayloadValue: (payload) => payload.contractForm.addressRoad,
    fieldKeys: ["LEASE_ADDRESS_ROAD"],
  },
  {
    id: "contract.addressLot",
    label: "지번 주소",
    target: { form: "contract", field: "addressLot" },
    readPayloadValue: (payload) => payload.contractForm.addressLot,
    fieldKeys: ["LEASE_ADDRESS_LOT"],
  },
  {
    id: "contract.contractType",
    label: "계약 유형",
    target: { form: "contract", field: "contractType" },
    readPayloadValue: (payload) => payload.contractForm.contractType,
    fieldKeys: ["LEASE_CONTRACT_TYPE"],
  },
  {
    id: "contract.depositAmount",
    label: "보증금",
    target: { form: "contract", field: "depositAmount" },
    readPayloadValue: (payload) =>
      payload.contractForm.depositAmount === null ? null : String(payload.contractForm.depositAmount),
    fieldKeys: ["LEASE_DEPOSIT_AMOUNT"],
  },
  {
    id: "contract.monthlyRentAmount",
    label: "월세",
    target: { form: "contract", field: "monthlyRentAmount" },
    readPayloadValue: (payload) =>
      payload.contractForm.monthlyRentAmount === null ? null : String(payload.contractForm.monthlyRentAmount),
    fieldKeys: ["LEASE_MONTHLY_RENT_AMOUNT"],
  },
  {
    id: "contract.contractPlannedDate",
    label: "계약 예정일",
    target: { form: "contract", field: "contractPlannedDate" },
    readPayloadValue: (payload) => payload.contractForm.contractPlannedDate,
    fieldKeys: ["LEASE_CONTRACT_DATE"],
  },
  {
    id: "contract.occupancyPlannedDate",
    label: "입주 예정일",
    target: { form: "contract", field: "occupancyPlannedDate" },
    readPayloadValue: (payload) => payload.contractForm.occupancyPlannedDate,
    fieldKeys: ["LEASE_OCCUPANCY_DATE"],
  },
  {
    id: "contract.landlordName",
    label: "임대인 이름",
    target: { form: "contract", field: "landlordName" },
    readPayloadValue: (payload) => payload.contractForm.landlordName,
    fieldKeys: ["LEASE_LANDLORD_NAME"],
  },
  {
    id: "registry.currentOwnerName",
    label: "현재 소유자 이름",
    target: { form: "registry", field: "currentOwnerName" },
    readPayloadValue: (payload) => payload.registryFindingsForm.currentOwnerName,
    fieldKeys: ["REGISTRY_OWNER_NAME"],
  },
  {
    id: "registry.ownerMatchesLandlord",
    label: "임대인과 소유자 일치 여부",
    target: { form: "registry", field: "ownerMatchesLandlord" },
    readPayloadValue: (payload) =>
      payload.registryFindingsForm.ownerMatchesLandlord === null ? null : String(payload.registryFindingsForm.ownerMatchesLandlord),
    fieldKeys: ["REGISTRY_OWNER_NAME", "LEASE_LANDLORD_NAME"],
    customSourceNote: (fields) => {
      const ownerField = fields.find((field) => field.fieldKey === "REGISTRY_OWNER_NAME");
      const landlordField = fields.find((field) => field.fieldKey === "LEASE_LANDLORD_NAME");
      const notes = [ownerField, landlordField]
        .filter((field): field is DocumentIntakeFieldResponse => Boolean(field))
        .map((field) => formatFieldSource(field));
      return notes.length > 0 ? `문서 비교 · ${notes.join(" + ")}` : "문서 비교 결과";
    },
  },
  {
    id: "registry.hasTrustRegistration",
    label: "신탁등기 여부",
    target: { form: "registry", field: "hasTrustRegistration" },
    readPayloadValue: (payload) =>
      payload.registryFindingsForm.hasTrustRegistration === null ? null : String(payload.registryFindingsForm.hasTrustRegistration),
    fieldKeys: ["REGISTRY_HAS_TRUST_REGISTRATION"],
  },
  {
    id: "registry.hasSeizure",
    label: "압류 여부",
    target: { form: "registry", field: "hasSeizure" },
    readPayloadValue: (payload) =>
      payload.registryFindingsForm.hasSeizure === null ? null : String(payload.registryFindingsForm.hasSeizure),
    fieldKeys: ["REGISTRY_HAS_SEIZURE"],
  },
  {
    id: "registry.hasProvisionalSeizure",
    label: "가압류 여부",
    target: { form: "registry", field: "hasProvisionalSeizure" },
    readPayloadValue: (payload) =>
      payload.registryFindingsForm.hasProvisionalSeizure === null ? null : String(payload.registryFindingsForm.hasProvisionalSeizure),
    fieldKeys: ["REGISTRY_HAS_PROVISIONAL_SEIZURE"],
  },
  {
    id: "registry.hasProvisionalDisposition",
    label: "가처분 여부",
    target: { form: "registry", field: "hasProvisionalDisposition" },
    readPayloadValue: (payload) =>
      payload.registryFindingsForm.hasProvisionalDisposition === null ? null : String(payload.registryFindingsForm.hasProvisionalDisposition),
    fieldKeys: ["REGISTRY_HAS_PROVISIONAL_DISPOSITION"],
  },
  {
    id: "registry.hasAuctionProceeding",
    label: "경매개시결정 여부",
    target: { form: "registry", field: "hasAuctionProceeding" },
    readPayloadValue: (payload) =>
      payload.registryFindingsForm.hasAuctionProceeding === null ? null : String(payload.registryFindingsForm.hasAuctionProceeding),
    fieldKeys: ["REGISTRY_HAS_AUCTION_PROCEEDING"],
  },
  {
    id: "registry.hasLeaseRegistration",
    label: "임차권등기 여부",
    target: { form: "registry", field: "hasLeaseRegistration" },
    readPayloadValue: (payload) =>
      payload.registryFindingsForm.hasLeaseRegistration === null ? null : String(payload.registryFindingsForm.hasLeaseRegistration),
    fieldKeys: ["REGISTRY_HAS_LEASE_REGISTRATION"],
  },
  {
    id: "registry.hasMortgage",
    label: "근저당 여부",
    target: { form: "registry", field: "hasMortgage" },
    readPayloadValue: (payload) =>
      payload.registryFindingsForm.hasMortgage === null ? null : String(payload.registryFindingsForm.hasMortgage),
    fieldKeys: ["REGISTRY_HAS_MORTGAGE"],
  },
  {
    id: "registry.seniorDebtAmount",
    label: "선순위 채권 금액",
    target: { form: "registry", field: "seniorDebtAmount" },
    readPayloadValue: (payload) =>
      payload.registryFindingsForm.seniorDebtAmount === null ? null : String(payload.registryFindingsForm.seniorDebtAmount),
    fieldKeys: ["REGISTRY_SENIOR_DEBT_AMOUNT"],
  },
];

function formatFieldSource(field: DocumentIntakeFieldResponse) {
  const documentLabel = documentTypeLabels[field.sourceDocument];
  const pageLabel = field.sourcePage ? ` ${field.sourcePage}페이지` : "";
  return `${documentLabel}${pageLabel}`;
}

function getCurrentValue(target: DocumentApplyTarget, contractForm: ContractFormState, registryFindingsForm: RegistryFindingsFormState) {
  return target.form === "contract" ? contractForm[target.field] : registryFindingsForm[target.field];
}

function buildSourceNote(config: PreviewConfig, fields: DocumentIntakeFieldResponse[]) {
  if (config.customSourceNote) {
    return config.customSourceNote(fields);
  }

  const sourceField = config.fieldKeys
    .map((fieldKey) => fields.find((field) => field.fieldKey === fieldKey))
    .find((field): field is DocumentIntakeFieldResponse => Boolean(field));

  return sourceField ? formatFieldSource(sourceField) : "승인된 문서 값";
}

export function buildDocumentApplyPreview(
  session: DocumentIntakeSessionResponse,
  payload: DocumentIntakeApplicationPayloadResponse,
  contractForm: ContractFormState,
  registryFindingsForm: RegistryFindingsFormState,
): DocumentApplyPreview {
  const items = previewConfigs
    .map((config) => {
      const incomingValue = config.readPayloadValue(payload);
      if (incomingValue === null) {
        return null;
      }

      const currentValue = getCurrentValue(config.target, contractForm, registryFindingsForm);
      const sameAsCurrent = currentValue === incomingValue;
      const conflict = currentValue.trim().length > 0 && !sameAsCurrent;

      return {
        id: config.id,
        label: config.label,
        target: config.target,
        currentValue,
        incomingValue,
        conflict,
        sameAsCurrent,
        applyByDefault: !conflict,
        fieldKeys: config.fieldKeys,
        sourceNote: buildSourceNote(config, session.fields),
      } satisfies DocumentApplyPreviewItem;
    })
    .filter((item): item is DocumentApplyPreviewItem => Boolean(item));

  return {
    sessionId: session.sessionId,
    approvedFieldCount: payload.approvedFieldCount,
    items,
    conflictCount: items.filter((item) => item.conflict).length,
    blankCount: items.filter((item) => !item.conflict && !item.sameAsCurrent && item.currentValue.trim().length === 0).length,
    sameCount: items.filter((item) => item.sameAsCurrent).length,
  };
}
