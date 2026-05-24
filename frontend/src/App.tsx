import { useEffect, useMemo, useState, type FormEvent } from "react";
import { ApiError, documentIntakeApi, houseCheckApi } from "./api";
import { buildDocumentApplyPreview, documentFieldLabels, type DocumentApplyPreview } from "./documentIntake";
import {
  buildProgressItems,
  formatBoolean,
  formatCalculation,
  formatDate,
  formatDocumentProcessingStatus,
  formatDocumentReviewStatus,
  formatDocumentType,
  formatNumber,
  formatRiskLevel,
  formatSourceType,
} from "./format";
import type {
  BuildingLedgerFindingsFormState,
  ContractFormState,
  DocumentIntakeDocumentResponse,
  DocumentIntakeFieldResponse,
  DocumentIntakeFieldReviewStatus,
  DocumentIntakeProcessingStatus,
  DocumentIntakeReviewAction,
  DocumentIntakeSessionResponse,
  DocumentIntakeWarningResponse,
  HouseChecklistResponse,
  HouseRiskReportResponse,
  MarketPriceFormState,
  MarketPriceLookupResponse,
  RegistryFindingsFormState,
  SectionStatusResponse,
} from "./types";
import {
  applyAccessDeniedReset,
  deriveDocumentIntakeUploadFailureMessage,
  DOCUMENT_INTAKE_UPLOAD_MAX_LABEL,
  deriveNeutralGlobalMessage,
  validateBuildingLedgerFindings,
  validateContract,
  validateDocumentIntakeUpload,
  validateMarketPrice,
  validateRegistryFindings,
  validateUpload,
  type ValidationErrors,
} from "./validation";

type AsyncState = "idle" | "loading" | "success" | "error";
type ResultState = "idle" | "loading" | "ready" | "not_ready" | "error";
type ResultTab = "report" | "checklist";
type ReviewTab = "all" | "registry" | "lease-contract" | "warnings";
type AccessMode = "none" | "boundary_reset" | "denied";
type DocumentUploadSlot = "registry" | "lease-contract";
type SourceStatus = "AUTO_APPLIED" | "USER_EDITED";

type FieldSourceNote = {
  sessionId: string;
  status: SourceStatus;
  note: string;
  previousValue: string;
};

type ContractFieldSources = Partial<Record<keyof ContractFormState, FieldSourceNote>>;
type RegistryFieldSources = Partial<Record<keyof RegistryFindingsFormState, FieldSourceNote>>;

const sessionKeys = {
  userId: "house-risk-agent-prompts.user-id",
  checkId: "house-risk-agent-prompts.check-id",
  documentIntakeSessionId: "house-risk-agent-prompts.document-intake-session-id",
};

const contractTypeOptions = [
  ["JEONSE", "전세"],
  ["MONTHLY_RENT", "월세"],
] as const;

const housingTypeOptions = [
  ["APARTMENT", "아파트"],
  ["OFFICETEL", "오피스텔"],
  ["VILLA", "빌라"],
  ["MULTI_HOUSEHOLD", "다세대"],
  ["MULTI_FAMILY", "다가구"],
  ["UNKNOWN", "기타 / 미확인"],
] as const;

const booleanFieldLabels: Partial<Record<keyof RegistryFindingsFormState, string>> = {
  ownerMatchesLandlord: "임대인과 소유자 일치 여부",
  hasTrustRegistration: "신탁등기 여부",
  hasSeizure: "압류 여부",
  hasProvisionalSeizure: "가압류 여부",
  hasProvisionalDisposition: "가처분 여부",
  hasAuctionProceeding: "경매개시결정 여부",
  hasLeaseRegistration: "임차권등기 여부",
  hasMortgage: "근저당 여부",
};

const initialContractForm: ContractFormState = {
  addressRoad: "",
  addressLot: "",
  contractType: "JEONSE",
  housingType: "APARTMENT",
  depositAmount: "",
  monthlyRentAmount: "0",
  contractPlannedDate: "",
  occupancyPlannedDate: "",
  landlordName: "",
};

const initialRegistryFindings: RegistryFindingsFormState = {
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

const initialBuildingLedgerFindings: BuildingLedgerFindingsFormState = {
  usage: "",
  isResidentialUseConfirmed: "",
  isViolationBuilding: "",
  isUnitConfirmed: "",
  isContractAreaConsistent: "",
  approvalDate: "",
  housingTypeObserved: "",
};

const initialMarketPrice: MarketPriceFormState = {
  estimatedMarketValue: "",
  estimatedJeonseValue: "",
  sourceLabel: "",
  referenceDate: "",
  sourceKind: "USER_ENTERED",
  sampleCount: "",
  lawdCode: "",
  dealYmdFrom: "",
  dealYmdTo: "",
};

const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
});

function readSessionValue(key: string) {
  return window.sessionStorage.getItem(key) || "";
}

function readCheckId() {
  return window.sessionStorage.getItem(sessionKeys.checkId);
}

function readDocumentIntakeSessionId() {
  return window.sessionStorage.getItem(sessionKeys.documentIntakeSessionId);
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "미기록";
  }

  return dateTimeFormatter.format(new Date(value));
}

function formatFieldValue(field: DocumentIntakeFieldResponse) {
  if (field.value === null || field.value === "") {
    return "추출값 없음";
  }

  if (field.fieldKey.endsWith("_AMOUNT")) {
    return `${Number(field.value).toLocaleString("ko-KR")}원`;
  }

  if (field.value === "true" || field.value === "false") {
    return field.value === "true" ? "예" : "아니오";
  }

  if (field.fieldKey === "LEASE_CONTRACT_TYPE") {
    return field.value === "JEONSE" ? "전세" : field.value === "MONTHLY_RENT" ? "월세" : field.value;
  }

  return field.value;
}

function formatConfidence(confidence: number) {
  if (confidence >= 0.85) {
    return { label: `높음 ${Math.round(confidence * 100)}%`, tone: "ready" as const };
  }

  if (confidence >= 0.6) {
    return { label: `보통 ${Math.round(confidence * 100)}%`, tone: "muted" as const };
  }

  return { label: `낮음 ${Math.round(confidence * 100)}%`, tone: "warning" as const };
}

function processingTone(status: DocumentIntakeProcessingStatus) {
  if (status === "APPROVED") return "ready";
  if (status === "FAILED" || status === "DELETED") return "critical";
  if (status === "REVIEW_REQUIRED" || status === "EXTRACTING") return "warning";
  return "muted";
}

function reviewTone(status: DocumentIntakeFieldReviewStatus) {
  if (status === "APPROVED" || status === "EDITED") return "ready";
  if (status === "EXCLUDED") return "critical";
  return "warning";
}

function warningTone(type: DocumentIntakeWarningResponse["type"]) {
  return type === "DEPOSIT_MISMATCH" ? "warning" : "critical";
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p className="field-error">{message}</p>;
}

function SourceNote({
  note,
}: {
  note?: FieldSourceNote;
}) {
  if (!note) return null;

  return (
    <div className="source-note">
      <span className="status-pill" data-tone={note.status === "USER_EDITED" ? "warning" : "ready"}>
        {note.status === "USER_EDITED" ? "사용자 수정됨" : "자동 입력 반영"}
      </span>
      <span>{note.note}</span>
    </div>
  );
}

function App() {
  const [draftUserId, setDraftUserId] = useState(() => readSessionValue(sessionKeys.userId));
  const [activeUserId, setActiveUserId] = useState(() => readSessionValue(sessionKeys.userId));
  const [checkId, setCheckId] = useState<string | null>(() => readCheckId());
  const [documentIntakeSessionId, setDocumentIntakeSessionId] = useState<string | null>(() => readDocumentIntakeSessionId());
  const [sectionStatus, setSectionStatus] = useState<SectionStatusResponse | null>(null);
  const [report, setReport] = useState<HouseRiskReportResponse | null>(null);
  const [checklist, setChecklist] = useState<HouseChecklistResponse | null>(null);
  const [documentSession, setDocumentSession] = useState<DocumentIntakeSessionResponse | null>(null);
  const [documentSessionState, setDocumentSessionState] = useState<AsyncState>("idle");
  const [documentSessionMessage, setDocumentSessionMessage] = useState("문서 검토 세션을 시작하면 업로드할 수 있습니다.");
  const [activeTab, setActiveTab] = useState<ResultTab>("report");
  const [reviewTab, setReviewTab] = useState<ReviewTab>("all");
  const [resultState, setResultState] = useState<ResultState>("idle");
  const [resultMessage, setResultMessage] = useState("아직 분석을 실행하지 않았습니다.");
  const [accessMode, setAccessMode] = useState<AccessMode>("none");
  const [globalMessage, setGlobalMessage] = useState(() =>
    deriveNeutralGlobalMessage(readSessionValue(sessionKeys.userId), readCheckId()),
  );

  const [contractForm, setContractForm] = useState(initialContractForm);
  const [registryFile, setRegistryFile] = useState<File | null>(null);
  const [buildingFile, setBuildingFile] = useState<File | null>(null);
  const [registryUploadIssuedDate, setRegistryUploadIssuedDate] = useState("");
  const [buildingUploadIssuedDate, setBuildingUploadIssuedDate] = useState("");
  const [registryUploadName, setRegistryUploadName] = useState("");
  const [buildingUploadName, setBuildingUploadName] = useState("");
  const [registryFindingsForm, setRegistryFindingsForm] = useState(initialRegistryFindings);
  const [buildingLedgerForm, setBuildingLedgerForm] = useState(initialBuildingLedgerFindings);
  const [marketPriceForm, setMarketPriceForm] = useState(initialMarketPrice);
  const [marketPriceLookup, setMarketPriceLookup] = useState<MarketPriceLookupResponse | null>(null);
  const [marketPriceLookupMessage, setMarketPriceLookupMessage] = useState("");
  const [contractFieldSources, setContractFieldSources] = useState<ContractFieldSources>({});
  const [registryFieldSources, setRegistryFieldSources] = useState<RegistryFieldSources>({});

  const [documentFiles, setDocumentFiles] = useState<Record<DocumentUploadSlot, File | null>>({
    registry: null,
    "lease-contract": null,
  });
  const [documentUploadStates, setDocumentUploadStates] = useState<Record<DocumentUploadSlot, AsyncState>>({
    registry: "idle",
    "lease-contract": "idle",
  });
  const [documentUploadErrors, setDocumentUploadErrors] = useState<Record<DocumentUploadSlot, ValidationErrors>>({
    registry: {},
    "lease-contract": {},
  });
  const [fieldActionStates, setFieldActionStates] = useState<Record<string, AsyncState>>({});
  const [editingFieldKey, setEditingFieldKey] = useState<string | null>(null);
  const [editingFieldValue, setEditingFieldValue] = useState("");
  const [focusedFieldKey, setFocusedFieldKey] = useState<string | null>(null);
  const [payloadPreviewState, setPayloadPreviewState] = useState<AsyncState>("idle");
  const [payloadPreviewMessage, setPayloadPreviewMessage] = useState("");
  const [applyPreview, setApplyPreview] = useState<DocumentApplyPreview | null>(null);
  const [applySelection, setApplySelection] = useState<Record<string, boolean>>({});

  const [contractErrors, setContractErrors] = useState<ValidationErrors>({});
  const [registryUploadErrors, setRegistryUploadErrors] = useState<ValidationErrors>({});
  const [buildingUploadErrors, setBuildingUploadErrors] = useState<ValidationErrors>({});
  const [registryFindingsErrors, setRegistryFindingsErrors] = useState<ValidationErrors>({});
  const [buildingLedgerErrors, setBuildingLedgerErrors] = useState<ValidationErrors>({});
  const [marketPriceErrors, setMarketPriceErrors] = useState<ValidationErrors>({});

  const [createState, setCreateState] = useState<AsyncState>("idle");
  const [registryUploadState, setRegistryUploadState] = useState<AsyncState>("idle");
  const [buildingUploadState, setBuildingUploadState] = useState<AsyncState>("idle");
  const [registryFindingState, setRegistryFindingState] = useState<AsyncState>("idle");
  const [buildingFindingState, setBuildingFindingState] = useState<AsyncState>("idle");
  const [marketPriceState, setMarketPriceState] = useState<AsyncState>("idle");
  const [marketPriceLookupState, setMarketPriceLookupState] = useState<AsyncState>("idle");
  const [analyzeState, setAnalyzeState] = useState<AsyncState>("idle");

  const hasAppliedDocumentFields = Object.keys(contractFieldSources).length + Object.keys(registryFieldSources).length > 0;
  const progressItems = useMemo(
    () => buildProgressItems(sectionStatus, documentSession, hasAppliedDocumentFields),
    [sectionStatus, documentSession, hasAppliedDocumentFields],
  );
  const userIdApplied = activeUserId.trim().length > 0;
  const pendingUserId = draftUserId.trim();
  const checkReady = Boolean(checkId);
  const documentReady = Boolean(documentIntakeSessionId);

  const documentSummary = useMemo(() => {
    const fields = documentSession?.fields ?? [];
    return {
      reviewRequiredCount: fields.filter((field) => field.reviewStatus === "REVIEW_REQUIRED").length,
      approvedCount: fields.filter((field) => field.reviewStatus === "APPROVED" || field.reviewStatus === "EDITED").length,
      excludedCount: fields.filter((field) => field.reviewStatus === "EXCLUDED").length,
      lowConfidenceCount: fields.filter((field) => field.confidence < 0.6).length,
      warningCount: documentSession?.warnings.length ?? 0,
    };
  }, [documentSession]);

  const visibleReviewFields = useMemo(() => {
    const fields = [...(documentSession?.fields ?? [])].sort((left, right) => {
      if (left.reviewStatus !== right.reviewStatus) {
        return left.reviewStatus.localeCompare(right.reviewStatus);
      }

      return left.fieldKey.localeCompare(right.fieldKey);
    });

    if (reviewTab === "registry") {
      return fields.filter((field) => field.sourceDocument === "REGISTRY");
    }

    if (reviewTab === "lease-contract") {
      return fields.filter((field) => field.sourceDocument === "LEASE_CONTRACT");
    }

    if (reviewTab === "warnings") {
      const relatedFieldKeys = new Set((documentSession?.warnings ?? []).flatMap((warning) => warning.relatedFields));
      return fields.filter((field) => relatedFieldKeys.has(field.fieldKey));
    }

    return fields;
  }, [documentSession, reviewTab]);

  const approvedPayloadReady = documentSummary.approvedCount > 0;

  const getDocument = (documentType: DocumentIntakeDocumentResponse["documentType"]) =>
    documentSession?.documents.find((document) => document.documentType === documentType) || null;

  const syncStoredIds = (nextUserId: string, nextCheckId: string | null, nextDocumentSessionId: string | null) => {
    window.sessionStorage.setItem(sessionKeys.userId, nextUserId);

    if (nextCheckId) {
      window.sessionStorage.setItem(sessionKeys.checkId, nextCheckId);
    } else {
      window.sessionStorage.removeItem(sessionKeys.checkId);
    }

    if (nextDocumentSessionId) {
      window.sessionStorage.setItem(sessionKeys.documentIntakeSessionId, nextDocumentSessionId);
    } else {
      window.sessionStorage.removeItem(sessionKeys.documentIntakeSessionId);
    }
  };

  const clearMessages = () => {
    setContractErrors({});
    setRegistryUploadErrors({});
    setBuildingUploadErrors({});
    setRegistryFindingsErrors({});
    setBuildingLedgerErrors({});
    setMarketPriceErrors({});
    setMarketPriceLookupMessage("");
    setDocumentUploadErrors({
      registry: {},
      "lease-contract": {},
    });
  };

  const resetServerData = (nextCheckId: string | null) => {
    setCheckId(nextCheckId);
    setSectionStatus(null);
    setReport(null);
    setChecklist(null);
    setResultState("idle");
    setResultMessage(nextCheckId ? "현재 User ID 기준으로 다시 결과를 불러와야 합니다." : "아직 분석을 실행하지 않았습니다.");
    setRegistryUploadName("");
    setBuildingUploadName("");
    setRegistryFile(null);
    setBuildingFile(null);
    setRegistryFindingsForm(initialRegistryFindings);
    setBuildingLedgerForm(initialBuildingLedgerFindings);
    setMarketPriceForm(initialMarketPrice);
    setMarketPriceLookup(null);
    setMarketPriceLookupState("idle");
    setMarketPriceLookupMessage("");
    setContractForm(initialContractForm);
    setContractFieldSources({});
    setRegistryFieldSources({});
  };

  const clearDocumentIntakeState = (clearStoredSessionId: boolean) => {
    setDocumentSession(null);
    setDocumentSessionState("idle");
    setDocumentSessionMessage("문서 검토 세션을 시작하면 업로드할 수 있습니다.");
    setDocumentFiles({
      registry: null,
      "lease-contract": null,
    });
    setDocumentUploadStates({
      registry: "idle",
      "lease-contract": "idle",
    });
    setDocumentUploadErrors({
      registry: {},
      "lease-contract": {},
    });
    setFieldActionStates({});
    setEditingFieldKey(null);
    setEditingFieldValue("");
    setReviewTab("all");
    setFocusedFieldKey(null);
    setPayloadPreviewState("idle");
    setPayloadPreviewMessage("");
    setApplyPreview(null);
    setApplySelection({});

    if (clearStoredSessionId) {
      setDocumentIntakeSessionId(null);
      window.sessionStorage.removeItem(sessionKeys.documentIntakeSessionId);
    }
  };

  const clearDocumentDerivedData = (sessionId?: string | null) => {
    const nextContractForm: ContractFormState = { ...contractForm };
    const nextRegistryForm: RegistryFindingsFormState = { ...registryFindingsForm };
    const nextContractSources = { ...contractFieldSources };
    const nextRegistrySources = { ...registryFieldSources };

    (Object.keys(contractFieldSources) as Array<keyof ContractFormState>).forEach((field) => {
      const source = contractFieldSources[field];
      if (!source || (sessionId && source.sessionId !== sessionId)) {
        return;
      }

      if (source.status === "AUTO_APPLIED") {
        (nextContractForm as Record<keyof ContractFormState, string>)[field] = source.previousValue;
      }

      delete nextContractSources[field];
    });

    (Object.keys(registryFieldSources) as Array<keyof RegistryFindingsFormState>).forEach((field) => {
      const source = registryFieldSources[field];
      if (!source || (sessionId && source.sessionId !== sessionId)) {
        return;
      }

      if (source.status === "AUTO_APPLIED") {
        (nextRegistryForm as Record<keyof RegistryFindingsFormState, string>)[field] = source.previousValue;
      }

      delete nextRegistrySources[field];
    });

    setContractForm(nextContractForm);
    setRegistryFindingsForm(nextRegistryForm);
    setContractFieldSources(nextContractSources);
    setRegistryFieldSources(nextRegistrySources);
  };

  const handleHouseCheckAccessDenied = (message: string) => {
    setAccessMode("denied");
    setSectionStatus(null);
    setReport(null);
    setChecklist(null);
    setResultState("error");
    setResultMessage(message);
    setGlobalMessage("현재 User ID로는 이 진단 요청에 접근할 수 없습니다. 다른 User ID로 다시 적용하거나 새 진단을 시작하세요.");
    setRegistryUploadName("");
    setBuildingUploadName("");
  };

  const handleDocumentAccessDenied = (message: string) => {
    clearDocumentDerivedData(documentIntakeSessionId);
    clearDocumentIntakeState(true);
    setAccessMode("denied");
    setDocumentSessionMessage(message);
    setGlobalMessage("현재 User ID로는 이 문서와 추출 결과에 접근할 수 없습니다. 다른 User ID로 다시 적용하거나 새 진단을 시작하세요.");
  };

  const handleKnownError = (
    error: unknown,
    setErrors?: (errors: ValidationErrors) => void,
    scope: "house-check" | "document-intake" = "house-check",
  ) => {
    if (error instanceof ApiError) {
      if (error.code === "ACCESS_DENIED") {
        if (scope === "document-intake") {
          handleDocumentAccessDenied(error.message);
        } else {
          handleHouseCheckAccessDenied(error.message);
        }
        return;
      }

      if (scope === "house-check" && error.code === "ANALYSIS_NOT_READY") {
        setResultState("not_ready");
        setResultMessage("분석 결과를 준비 중입니다. 잠시 후 다시 불러오세요.");
        return;
      }

      if (setErrors && error.fieldErrors?.length) {
        const nextErrors = error.fieldErrors.reduce<ValidationErrors>((acc, fieldError) => {
          acc[fieldError.field.split(".").pop() || fieldError.field] = fieldError.reason;
          return acc;
        }, {});
        setErrors(nextErrors);
      }

      throw error;
    }

    throw error;
  };

  const withSectionSuccess = (status: SectionStatusResponse, message: string) => {
    setSectionStatus(status);
    setCheckId(status.checkId);
    syncStoredIds(activeUserId, status.checkId, documentIntakeSessionId);
    setAccessMode("none");
    setGlobalMessage(message);
  };

  const updateDocumentSession = (session: DocumentIntakeSessionResponse, message: string) => {
    setDocumentSession(session);
    setDocumentSessionState("success");
    setDocumentIntakeSessionId(session.sessionId);
    setDocumentSessionMessage(message);
    setAccessMode("none");
    syncStoredIds(activeUserId, checkId, session.sessionId);
    setApplyPreview(null);
    setApplySelection({});
    setPayloadPreviewState("idle");
  };

  const updateContractField = <K extends keyof ContractFormState>(field: K, value: ContractFormState[K]) => {
    setContractForm((previous) => ({
      ...previous,
      [field]: value,
    }));
    setContractFieldSources((previous) => {
      const existing = previous[field];
      if (!existing) {
        return previous;
      }

      return {
        ...previous,
        [field]: {
          ...existing,
          status: "USER_EDITED",
        },
      };
    });
  };

  const updateRegistryField = <K extends keyof RegistryFindingsFormState>(field: K, value: RegistryFindingsFormState[K]) => {
    setRegistryFindingsForm((previous) => ({
      ...previous,
      [field]: value,
    }));
    setRegistryFieldSources((previous) => {
      const existing = previous[field];
      if (!existing) {
        return previous;
      }

      return {
        ...previous,
        [field]: {
          ...existing,
          status: "USER_EDITED",
        },
      };
    });
  };

  const refreshResults = async () => {
    if (!activeUserId || !checkId) return;

    setResultState("loading");
    setResultMessage("결과를 불러오는 중입니다.");

    try {
      const [nextReport, nextChecklist] = await Promise.all([
        houseCheckApi.getReport(activeUserId, checkId),
        houseCheckApi.getChecklist(activeUserId, checkId),
      ]);
      setReport(nextReport);
      setChecklist(nextChecklist);
      setSectionStatus(nextReport.sectionStatus);
      setResultState("ready");
      setResultMessage("현재 확인된 자료 기준 결과를 불러왔습니다.");
      setAccessMode("none");
    } catch (error) {
      try {
        handleKnownError(error);
      } catch (innerError) {
        if (innerError instanceof ApiError) {
          setResultState("error");
          setResultMessage(innerError.message);
          return;
        }

        setResultState("error");
        setResultMessage("결과를 불러오지 못했습니다. 잠시 후 다시 시도하세요.");
      }
    }
  };

  useEffect(() => {
    if (!activeUserId || !documentIntakeSessionId) {
      return;
    }

    if (documentSession?.sessionId === documentIntakeSessionId) {
      return;
    }

    let cancelled = false;
    setDocumentSessionState("loading");
    setDocumentSessionMessage("이전 문서 검토 세션을 불러오는 중입니다.");

    void documentIntakeApi
      .getSession(activeUserId, documentIntakeSessionId)
      .then((session) => {
        if (cancelled) return;
        setDocumentSession(session);
        setDocumentSessionState("success");
        setDocumentSessionMessage("이전 문서 검토 세션을 다시 불러왔습니다.");
      })
      .catch((error) => {
        if (cancelled) return;
        try {
          handleKnownError(error, undefined, "document-intake");
        } catch (innerError) {
          setDocumentSessionState("error");
          setDocumentSessionMessage(innerError instanceof ApiError ? innerError.message : "문서 검토 세션을 다시 불러오지 못했습니다.");
        }
      });

    return () => {
      cancelled = true;
    };
  }, [activeUserId, documentIntakeSessionId, documentSession?.sessionId]);

  const onApplyUserId = () => {
    const nextUserId = draftUserId.trim();
    if (!nextUserId) {
      setGlobalMessage("User ID를 입력해야 진단을 시작할 수 있습니다.");
      return;
    }

    const boundaryReset = applyAccessDeniedReset(checkId, nextUserId, activeUserId);
    const documentBoundaryReset = Boolean(documentIntakeSessionId && activeUserId && activeUserId !== nextUserId);
    setActiveUserId(nextUserId);
    window.sessionStorage.setItem(sessionKeys.userId, nextUserId);
    clearMessages();

    if (boundaryReset.shouldClearServerData || documentBoundaryReset) {
      resetServerData(checkId);
      clearDocumentDerivedData(documentIntakeSessionId);
      clearDocumentIntakeState(true);
      setAccessMode("boundary_reset");
      setGlobalMessage("User ID가 바뀌어 기존 서버 데이터와 문서 초안을 화면에서 제거했습니다. 같은 checkId 접근은 다시 확인해야 합니다.");
      syncStoredIds(nextUserId, checkId, null);
      return;
    }

    setAccessMode("none");
    setGlobalMessage(deriveNeutralGlobalMessage(nextUserId, checkId));
    syncStoredIds(nextUserId, checkId, documentIntakeSessionId);
  };

  const onStartFresh = () => {
    clearMessages();
    clearDocumentDerivedData(documentIntakeSessionId);
    clearDocumentIntakeState(true);
    resetServerData(null);
    setAccessMode("none");
    setGlobalMessage(userIdApplied ? "새 진단을 시작할 수 있습니다." : deriveNeutralGlobalMessage(activeUserId, null));
    syncStoredIds(activeUserId, null, null);
  };

  const onCreateDocumentSession = async () => {
    if (!activeUserId) {
      setGlobalMessage("User ID를 먼저 적용하세요.");
      return;
    }

    setDocumentSessionState("loading");
    setDocumentSessionMessage("문서 검토 세션을 시작하는 중입니다.");

    try {
      const session = await documentIntakeApi.createSession(activeUserId);
      updateDocumentSession(session, "문서 검토 세션이 준비되었습니다. 등기부등본과 임대차 계약서를 등록해 주세요.");
    } catch (error) {
      try {
        handleKnownError(error, undefined, "document-intake");
      } catch (innerError) {
        setDocumentSessionState("error");
        setDocumentSessionMessage(innerError instanceof ApiError ? innerError.message : "문서 검토 세션을 만들지 못했습니다.");
      }
    }
  };

  const onRefreshDocumentSession = async () => {
    if (!activeUserId || !documentIntakeSessionId) return;

    setDocumentSessionState("loading");
    setDocumentSessionMessage("문서 추출 상태를 다시 불러오는 중입니다.");

    try {
      const session = await documentIntakeApi.getSession(activeUserId, documentIntakeSessionId);
      updateDocumentSession(session, "문서 추출 상태를 다시 불러왔습니다.");
    } catch (error) {
      try {
        handleKnownError(error, undefined, "document-intake");
      } catch (innerError) {
        setDocumentSessionState("error");
        setDocumentSessionMessage(innerError instanceof ApiError ? innerError.message : "문서 추출 상태를 다시 불러오지 못했습니다.");
      }
    }
  };

  const onUploadDocument = async (slot: DocumentUploadSlot) => {
    if (!activeUserId || !documentIntakeSessionId) {
      setDocumentSessionMessage("문서 검토 세션을 먼저 시작하세요.");
      return;
    }

    const file = documentFiles[slot];
    const errors = validateDocumentIntakeUpload(slot, file);
    if (Object.keys(errors).length > 0) {
      setDocumentUploadErrors((previous) => ({
        ...previous,
        [slot]: errors,
      }));
      return;
    }

    setDocumentUploadErrors((previous) => ({
      ...previous,
      [slot]: {},
    }));
    setDocumentUploadStates((previous) => ({
      ...previous,
      [slot]: "loading",
    }));

    try {
      const session = await documentIntakeApi.uploadDocument(activeUserId, documentIntakeSessionId, slot, file!);
      updateDocumentSession(
        session,
        slot === "registry"
          ? "등기부등본이 등록되었습니다. 자동 입력 초안을 검토해 주세요."
          : "임대차 계약서가 등록되었습니다. 자동 입력 초안을 검토해 주세요.",
      );
      setDocumentUploadStates((previous) => ({
        ...previous,
        [slot]: "success",
      }));
    } catch (error) {
      try {
        handleKnownError(
          error,
          (nextErrors) =>
            setDocumentUploadErrors((previous) => ({
              ...previous,
              [slot]: nextErrors,
            })),
          "document-intake",
        );
      } catch (innerError) {
        setDocumentUploadStates((previous) => ({
          ...previous,
          [slot]: "error",
        }));
        setDocumentUploadErrors((previous) => ({
          ...previous,
          [slot]: {
            form: deriveDocumentIntakeUploadFailureMessage(innerError),
          },
        }));
      }
    }
  };

  const onDeleteDocument = async (slot: DocumentUploadSlot) => {
    if (!activeUserId || !documentIntakeSessionId) return;

    setDocumentUploadStates((previous) => ({
      ...previous,
      [slot]: "loading",
    }));

    try {
      const session = await documentIntakeApi.deleteDocument(activeUserId, documentIntakeSessionId, slot);
      updateDocumentSession(session, slot === "registry" ? "등기부등본을 삭제했습니다." : "임대차 계약서를 삭제했습니다.");
      setDocumentUploadStates((previous) => ({
        ...previous,
        [slot]: "success",
      }));
      setDocumentFiles((previous) => ({
        ...previous,
        [slot]: null,
      }));
    } catch (error) {
      try {
        handleKnownError(error, undefined, "document-intake");
      } catch (innerError) {
        setDocumentUploadStates((previous) => ({
          ...previous,
          [slot]: "error",
        }));
        setDocumentUploadErrors((previous) => ({
          ...previous,
          [slot]: {
            form: innerError instanceof ApiError ? innerError.message : "문서를 삭제하지 못했습니다. 잠시 후 다시 시도하세요.",
          },
        }));
      }
    }
  };

  const onStartEditField = (field: DocumentIntakeFieldResponse) => {
    setEditingFieldKey(field.fieldKey);
    setEditingFieldValue(field.value ?? "");
  };

  const onReviewField = async (fieldKey: string, action: DocumentIntakeReviewAction, editedValue?: string) => {
    if (!activeUserId || !documentIntakeSessionId) return;

    setFieldActionStates((previous) => ({
      ...previous,
      [fieldKey]: "loading",
    }));

    try {
      const session = await documentIntakeApi.reviewField(activeUserId, documentIntakeSessionId, fieldKey, action, editedValue);
      updateDocumentSession(
        session,
        action === "APPROVE"
          ? "필드를 승인했습니다."
          : action === "EDIT"
            ? "수정한 값을 승인 초안에 반영했습니다."
            : "필드를 제외했습니다.",
      );
      setFieldActionStates((previous) => ({
        ...previous,
        [fieldKey]: "success",
      }));
      if (editingFieldKey === fieldKey) {
        setEditingFieldKey(null);
        setEditingFieldValue("");
      }
    } catch (error) {
      try {
        handleKnownError(error, undefined, "document-intake");
      } catch (innerError) {
        setFieldActionStates((previous) => ({
          ...previous,
          [fieldKey]: "error",
        }));
        setDocumentSessionMessage(innerError instanceof ApiError ? innerError.message : "필드 검토 상태를 저장하지 못했습니다.");
      }
    }
  };

  const onPrepareApplyApprovedFields = async () => {
    if (!activeUserId || !documentIntakeSessionId || !documentSession) return;

    if (!approvedPayloadReady) {
      setPayloadPreviewState("error");
      setPayloadPreviewMessage("승인한 필드가 없습니다. 반영할 항목을 먼저 확인해 주세요.");
      return;
    }

    setPayloadPreviewState("loading");
    setPayloadPreviewMessage("승인된 필드를 반영 전 비교하는 중입니다.");

    try {
      const payload = await documentIntakeApi.getApplicationPayload(activeUserId, documentIntakeSessionId);
      const preview = buildDocumentApplyPreview(documentSession, payload, contractForm, registryFindingsForm);

      if (preview.items.length === 0) {
        setPayloadPreviewState("error");
        setPayloadPreviewMessage("이번 문서에서는 바로 반영할 필드를 찾지 못했습니다. 필요한 값은 수기로 입력할 수 있습니다.");
        setApplyPreview(null);
        return;
      }

      setApplyPreview(preview);
      setApplySelection(
        Object.fromEntries(preview.items.map((item) => [item.id, item.applyByDefault])),
      );
      setPayloadPreviewState("success");
      setPayloadPreviewMessage("현재 입력값과 승인된 문서 초안을 비교했습니다. 충돌 항목은 교체 여부를 직접 선택해 주세요.");
    } catch (error) {
      try {
        handleKnownError(error, undefined, "document-intake");
      } catch (innerError) {
        setPayloadPreviewState("error");
        setPayloadPreviewMessage(innerError instanceof ApiError ? innerError.message : "승인된 필드를 비교하지 못했습니다.");
      }
    }
  };

  const onApplyApprovedFields = () => {
    if (!applyPreview) {
      return;
    }

    const nextContractForm: ContractFormState = { ...contractForm };
    const nextRegistryForm: RegistryFindingsFormState = { ...registryFindingsForm };
    const nextContractSources = { ...contractFieldSources };
    const nextRegistrySources = { ...registryFieldSources };
    const selectedItems = applyPreview.items.filter((item) => applySelection[item.id]);

    if (selectedItems.length === 0) {
      setPayloadPreviewState("error");
      setPayloadPreviewMessage("반영할 필드를 선택하지 않았습니다. 충돌 항목은 현재 값 유지 또는 교체 중 하나를 선택해 주세요.");
      return;
    }

    selectedItems.forEach((item) => {
      if (item.target.form === "contract") {
        const field = item.target.field;
        nextContractSources[field] = {
          sessionId: applyPreview.sessionId,
          status: "AUTO_APPLIED",
          note: item.sourceNote,
          previousValue: nextContractForm[field],
        };
        (nextContractForm as Record<keyof ContractFormState, string>)[field] = item.incomingValue;
        return;
      }

      const field = item.target.field;
      nextRegistrySources[field] = {
        sessionId: applyPreview.sessionId,
        status: "AUTO_APPLIED",
        note: item.sourceNote,
        previousValue: nextRegistryForm[field],
      };
      (nextRegistryForm as Record<keyof RegistryFindingsFormState, string>)[field] = item.incomingValue;
    });

    setContractForm(nextContractForm);
    setRegistryFindingsForm(nextRegistryForm);
    setContractFieldSources(nextContractSources);
    setRegistryFieldSources(nextRegistrySources);
    setPayloadPreviewState("success");
    setPayloadPreviewMessage(
      checkReady
        ? "승인한 필드를 화면 입력값에 반영했습니다. 등기 확인은 저장 버튼으로 현재 진단에 반영하세요. 계약 기본 정보는 새 진단 시작 전 반영하는 흐름을 권장합니다."
        : "승인한 필드를 계약 기본 정보와 등기 확인 초안에 반영했습니다. 필요하면 계속 수정할 수 있습니다.",
    );
    setApplyPreview(null);
  };

  const onCreate = async (event: FormEvent) => {
    event.preventDefault();
    clearMessages();
    const errors = validateContract(contractForm);
    if (Object.keys(errors).length > 0) {
      setContractErrors(errors);
      return;
    }
    if (!activeUserId) {
      setGlobalMessage("User ID를 먼저 적용하세요.");
      return;
    }

    setCreateState("loading");
    try {
      const status = await houseCheckApi.create(activeUserId, contractForm);
      withSectionSuccess(status, "진단 요청이 생성되었습니다. 아래 단계들을 순서대로 채워 주세요.");
      setCreateState("success");
      setResultState("idle");
      setResultMessage("아직 분석을 실행하지 않았습니다.");
    } catch (error) {
      try {
        handleKnownError(error, setContractErrors);
      } catch (innerError) {
        setCreateState("error");
        if (innerError instanceof ApiError) {
          setGlobalMessage(innerError.message);
        } else {
          setGlobalMessage("진단 요청을 생성하지 못했습니다. 입력값을 다시 확인하세요.");
        }
      }
      return;
    }

    setCreateState("success");
  };

  const onUpload = async (kind: "registry" | "building") => {
    if (!activeUserId || !checkId) return;

    const file = kind === "registry" ? registryFile : buildingFile;
    const errors = validateUpload(file);
    if (Object.keys(errors).length > 0) {
      if (kind === "registry") setRegistryUploadErrors(errors);
      else setBuildingUploadErrors(errors);
      return;
    }

    const setState = kind === "registry" ? setRegistryUploadState : setBuildingUploadState;
    const setErrors = kind === "registry" ? setRegistryUploadErrors : setBuildingUploadErrors;
    const issuedDate = kind === "registry" ? registryUploadIssuedDate : buildingUploadIssuedDate;
    setState("loading");
    setErrors({});

    try {
      const status = await houseCheckApi.uploadDocument(
        activeUserId,
        checkId,
        kind === "registry" ? "registry-file" : "building-ledger-file",
        file!,
        issuedDate,
      );
      withSectionSuccess(
        status,
        kind === "registry" ? "분석용 등기 PDF가 업로드되었습니다." : "건축물대장 PDF가 업로드되었습니다.",
      );
      if (kind === "registry") {
        setRegistryUploadName(file!.name);
      } else {
        setBuildingUploadName(file!.name);
      }
      setState("success");
    } catch (error) {
      try {
        handleKnownError(error, setErrors);
      } catch (innerError) {
        setState("error");
        if (innerError instanceof ApiError) {
          setErrors({ form: innerError.message });
        } else {
          setErrors({ form: "업로드를 완료하지 못했습니다. 파일과 발급일을 다시 확인하세요." });
        }
      }
    }
  };

  const onSaveRegistryFindings = async (event: FormEvent) => {
    event.preventDefault();
    if (!activeUserId || !checkId) return;

    const errors = validateRegistryFindings(registryFindingsForm);
    if (Object.keys(errors).length > 0) {
      setRegistryFindingsErrors(errors);
      return;
    }

    setRegistryFindingState("loading");
    setRegistryFindingsErrors({});
    try {
      const status = await houseCheckApi.saveRegistryFindings(activeUserId, checkId, registryFindingsForm);
      withSectionSuccess(status, "등기 수기 확인 내용이 저장되었습니다.");
      setRegistryFindingState("success");
    } catch (error) {
      try {
        handleKnownError(error, setRegistryFindingsErrors);
      } catch (innerError) {
        setRegistryFindingState("error");
        if (innerError instanceof ApiError) {
          setRegistryFindingsErrors({ form: innerError.message });
        } else {
          setRegistryFindingsErrors({ form: "등기 수기 확인 내용을 저장하지 못했습니다." });
        }
      }
    }
  };

  const onSaveBuildingLedgerFindings = async (event: FormEvent) => {
    event.preventDefault();
    if (!activeUserId || !checkId) return;

    const errors = validateBuildingLedgerFindings(buildingLedgerForm);
    if (Object.keys(errors).length > 0) {
      setBuildingLedgerErrors(errors);
      return;
    }

    setBuildingFindingState("loading");
    setBuildingLedgerErrors({});
    try {
      const status = await houseCheckApi.saveBuildingLedgerFindings(activeUserId, checkId, buildingLedgerForm);
      withSectionSuccess(status, "건축물대장 수기 확인 내용이 저장되었습니다.");
      setBuildingFindingState("success");
    } catch (error) {
      try {
        handleKnownError(error, setBuildingLedgerErrors);
      } catch (innerError) {
        setBuildingFindingState("error");
        if (innerError instanceof ApiError) {
          setBuildingLedgerErrors({ form: innerError.message });
        } else {
          setBuildingLedgerErrors({ form: "건축물대장 수기 확인 내용을 저장하지 못했습니다." });
        }
      }
    }
  };

  const onLookupMarketPrice = async () => {
    if (!activeUserId || !checkId) return;

    setMarketPriceLookupState("loading");
    setMarketPriceLookup(null);
    setMarketPriceLookupMessage("공공 실거래가를 조회하는 중입니다.");
    setMarketPriceErrors({});
    try {
      const lookup = await houseCheckApi.lookupMarketPrice(activeUserId, checkId);
      setMarketPriceLookup(lookup);
      setMarketPriceLookupState("success");
      if (lookup.confidence === "AVAILABLE") {
        setMarketPriceLookupMessage("조회 결과를 확인한 뒤 적용할 수 있습니다.");
      } else if (lookup.confidence === "LOW_CONFIDENCE") {
        setMarketPriceLookupMessage("표본이 부족해 계산 기준값으로 확정하지 않았습니다. 수동 입력을 함께 확인하세요.");
      } else {
        setMarketPriceLookupMessage("적용 가능한 공공 실거래가 표본을 찾지 못했습니다. 수동 입력을 사용해 주세요.");
      }
    } catch (error) {
      try {
        handleKnownError(error, setMarketPriceErrors);
      } catch (innerError) {
        setMarketPriceLookupState("error");
        if (innerError instanceof ApiError) {
          setMarketPriceLookupMessage(innerError.message);
        } else {
          setMarketPriceLookupMessage("공공 실거래가를 조회하지 못했습니다. 수동 입력을 사용해 주세요.");
        }
      }
    }
  };

  const onApplyMarketPriceLookup = () => {
    if (!marketPriceLookup) return;

    setMarketPriceForm({
      estimatedMarketValue: marketPriceLookup.estimatedMarketValue?.toString() || "",
      estimatedJeonseValue: marketPriceLookup.estimatedJeonseValue?.toString() || "",
      sourceLabel: marketPriceLookup.sourceLabel,
      referenceDate: marketPriceLookup.referenceDate,
      sourceKind: marketPriceLookup.sourceKind,
      sampleCount: marketPriceLookup.sampleCount.toString(),
      lawdCode: marketPriceLookup.lawdCode || "",
      dealYmdFrom: marketPriceLookup.dealYmdFrom || "",
      dealYmdTo: marketPriceLookup.dealYmdTo || "",
    });
    setMarketPriceLookupMessage("조회 결과를 입력값에 적용했습니다. 저장 버튼으로 최종 반영하세요.");
  };

  const updateManualMarketPriceField = (field: keyof Pick<MarketPriceFormState, "estimatedMarketValue" | "estimatedJeonseValue" | "sourceLabel" | "referenceDate">, value: string) => {
    setMarketPriceForm((current) => ({
      ...current,
      [field]: value,
      sourceKind: "USER_ENTERED",
      sampleCount: "",
      lawdCode: "",
      dealYmdFrom: "",
      dealYmdTo: "",
    }));
  };

  const onSaveMarketPrice = async (event: FormEvent) => {
    event.preventDefault();
    if (!activeUserId || !checkId) return;

    const errors = validateMarketPrice(marketPriceForm);
    if (Object.keys(errors).length > 0) {
      setMarketPriceErrors(errors);
      return;
    }

    setMarketPriceState("loading");
    setMarketPriceErrors({});
    try {
      const status = await houseCheckApi.saveMarketPrice(activeUserId, checkId, marketPriceForm);
      withSectionSuccess(status, "시세 입력이 저장되었습니다.");
      setMarketPriceState("success");
    } catch (error) {
      try {
        handleKnownError(error, setMarketPriceErrors);
      } catch (innerError) {
        setMarketPriceState("error");
        if (innerError instanceof ApiError) {
          setMarketPriceErrors({ form: innerError.message });
        } else {
          setMarketPriceErrors({ form: "시세 입력을 저장하지 못했습니다." });
        }
      }
    }
  };

  const onAnalyze = async () => {
    if (!activeUserId || !checkId) return;
    setAnalyzeState("loading");
    setResultState("loading");
    setResultMessage("분석을 실행하는 중입니다.");

    try {
      const status = await houseCheckApi.analyze(activeUserId, checkId);
      withSectionSuccess(status, "현재 입력 기준으로 분석을 실행했습니다.");
      setAnalyzeState("success");
      if (status.analysisStatus === "COMPLETED") {
        await refreshResults();
      } else {
        setResultState("not_ready");
        setResultMessage("분석 결과를 준비 중입니다. 잠시 후 다시 불러오세요.");
      }
    } catch (error) {
      try {
        handleKnownError(error);
      } catch (innerError) {
        setAnalyzeState("error");
        setResultState("error");
        if (innerError instanceof ApiError) {
          setResultMessage(innerError.message);
        } else {
          setResultMessage("분석을 완료하지 못했습니다. 입력값과 업로드 상태를 다시 확인하세요.");
        }
      }
    }
  };

  const renderDocumentSlot = (slot: DocumentUploadSlot) => {
    const document = getDocument(slot === "registry" ? "REGISTRY" : "LEASE_CONTRACT");
    const selectedFile = documentFiles[slot];
    const errors = documentUploadErrors[slot];
    const uploadState = documentUploadStates[slot];
    const slotTitle = slot === "registry" ? "등기부등본 PDF" : "임대차 계약서 PDF / 이미지";
    const accept = slot === "registry" ? ".pdf,application/pdf" : ".pdf,.jpg,.jpeg,.png,.webp,application/pdf,image/jpeg,image/png,image/webp";
    const helper =
      slot === "registry"
        ? `PDF만 지원합니다. 최대 ${DOCUMENT_INTAKE_UPLOAD_MAX_LABEL}까지 업로드할 수 있습니다.`
        : `PDF, JPEG, PNG, WebP를 지원합니다. 최대 ${DOCUMENT_INTAKE_UPLOAD_MAX_LABEL}까지 업로드할 수 있습니다.`;
    const actionLabel =
      document?.processingStatus === "FAILED"
        ? "파일 선택 후 다시 업로드"
        : document
          ? "다시 업로드"
          : "업로드";

    return (
      <section className="sub-card" key={slot}>
        <div className="sub-card-header">
          <div>
            <h3>{slotTitle}</h3>
            <p>{helper}</p>
          </div>
          <span className="status-pill" data-tone={document ? processingTone(document.processingStatus) : "muted"}>
            {document ? formatDocumentProcessingStatus(document.processingStatus) : "미업로드"}
          </span>
        </div>
        <label className="field-group">
          <span>문서 선택</span>
          <input
            type="file"
            accept={accept}
            disabled={!documentReady || uploadState === "loading"}
            onChange={(event) => {
              setDocumentFiles((previous) => ({
                ...previous,
                [slot]: event.target.files?.[0] ?? null,
              }));
              setDocumentUploadErrors((previous) => ({
                ...previous,
                [slot]: {},
              }));
            }}
          />
          <FieldError message={errors.file} />
        </label>
        <p className="meta-line">선택 파일: {selectedFile?.name || document?.fileName || "없음"}</p>
        <dl className="detail-list compact">
          <div>
            <dt>MIME</dt>
            <dd>{document?.mimeType || "미기록"}</dd>
          </div>
          <div>
            <dt>업로드 시각</dt>
            <dd>{formatDateTime(document?.uploadedAt)}</dd>
          </div>
          <div>
            <dt>마지막 처리</dt>
            <dd>{formatDateTime(document?.processedAt)}</dd>
          </div>
        </dl>
        {document?.failure ? (
          <div className="inline-warning" data-tone="critical">
            <strong>{document.failure.code}</strong>
            <span>{document.failure.message} 새 파일을 선택한 뒤 다시 업로드하세요.</span>
          </div>
        ) : null}
        <FieldError message={errors.form} />
        <div className="card-actions">
          <button
            type="button"
            className="secondary-button"
            disabled={!documentReady || uploadState === "loading"}
            onClick={() => void onUploadDocument(slot)}
          >
            {uploadState === "loading" ? "업로드 중..." : actionLabel}
          </button>
          <button
            type="button"
            className="ghost-button"
            disabled={!documentReady || !document || uploadState === "loading"}
            onClick={() => void onDeleteDocument(slot)}
          >
            삭제
          </button>
        </div>
      </section>
    );
  };

  return (
    <div className="app-shell">
      <header className="workspace-header">
        <div>
          <p className="eyebrow">Operational Workspace</p>
          <h1>주택 계약 위험 진단</h1>
          <p className="header-copy">
            이 화면은 입력한 User ID를 <code>X-User-Id</code> 헤더로 사용합니다. 다른 User ID로 바꾸면 기존 진단 접근이 거부될 수 있습니다.
          </p>
        </div>
        <div className="session-panel">
          <label className="field-group compact">
            <span>User ID</span>
            <input value={draftUserId} onChange={(event) => setDraftUserId(event.target.value)} placeholder="예: owner-a" />
          </label>
          <button type="button" className="primary-button" onClick={onApplyUserId}>
            적용
          </button>
          <button type="button" className="secondary-button" onClick={onStartFresh}>
            새 진단 시작
          </button>
          <dl className="session-meta">
            <div>
              <dt>현재 User ID</dt>
              <dd>{activeUserId || "미적용"}</dd>
            </div>
            <div>
              <dt>checkId</dt>
              <dd>{checkId || "아직 없음"}</dd>
            </div>
            <div>
              <dt>문서 세션</dt>
              <dd>{documentIntakeSessionId || "아직 없음"}</dd>
            </div>
          </dl>
        </div>
      </header>

      <div className="global-banner" data-tone={accessMode === "denied" ? "critical" : accessMode === "boundary_reset" ? "warning" : "neutral"}>
        {globalMessage}
      </div>

      <main className="workspace-grid">
        <aside className="status-rail card">
          <div className="card-header">
            <h2>진행 상태</h2>
            <span className="status-pill" data-tone={userIdApplied ? "ready" : "muted"}>
              {userIdApplied ? "User ID 적용됨" : "User ID 필요"}
            </span>
          </div>
          <ul className="progress-list">
            {progressItems.map(([title, value]) => (
              <li key={title}>
                <span>{title}</span>
                <strong>{value}</strong>
              </li>
            ))}
          </ul>
        </aside>

        <section className="workspace-content">
          <section className="card">
            <div className="card-header">
              <div>
                <h2>문서 자동 입력</h2>
                <p>등기부등본과 임대차 계약서를 등록하면 핵심 항목을 추출해 검토용 초안을 만듭니다. 승인 전에는 진단 입력값에 반영되지 않습니다.</p>
              </div>
              <div className="card-actions">
                <span className="status-pill" data-tone={documentSessionState === "success" ? "ready" : documentReady ? "warning" : "muted"}>
                  {documentReady ? "세션 준비됨" : "세션 필요"}
                </span>
                <button
                  type="button"
                  className="primary-button"
                  disabled={!userIdApplied || documentSessionState === "loading"}
                  onClick={() => void onCreateDocumentSession()}
                >
                  {documentSessionState === "loading" && !documentReady ? "세션 시작 중..." : documentReady ? "새 문서 세션 시작" : "문서 세션 시작"}
                </button>
                <button
                  type="button"
                  className="ghost-button"
                  disabled={!documentReady || documentSessionState === "loading"}
                  onClick={() => void onRefreshDocumentSession()}
                >
                  상태 새로고침
                </button>
              </div>
            </div>
            <p className="helper-copy">
              문서 원본과 추출 결과는 민감 정보로 처리됩니다. 인터넷등기소 로그인 정보, 공동인증서 비밀번호, 결제 정보는 입력하거나 저장하지 않습니다.
            </p>
            <div className="info-strip">
              <span>{documentSessionMessage}</span>
              {documentSession ? (
                <span>
                  생성 {formatDateTime(documentSession.createdAt)} · 만료 예정 {formatDateTime(documentSession.expiresAt)}
                </span>
              ) : null}
            </div>
            <div className="split-cards">{renderDocumentSlot("registry")}{renderDocumentSlot("lease-contract")}</div>
          </section>

          <section className="card">
            <div className="card-header">
              <div>
                <h2>추출 검토 및 승인</h2>
                <p>문서에서 찾은 값은 검토용 초안입니다. 원문 근거를 확인한 뒤 필요한 값만 승인하거나 수정하세요.</p>
              </div>
              <div className="summary-chips">
                <span className="status-pill" data-tone={documentSummary.reviewRequiredCount > 0 ? "warning" : "muted"}>
                  검토 필요 {documentSummary.reviewRequiredCount}건
                </span>
                <span className="status-pill" data-tone={documentSummary.lowConfidenceCount > 0 ? "warning" : "muted"}>
                  낮은 신뢰도 {documentSummary.lowConfidenceCount}건
                </span>
                <span className="status-pill" data-tone={documentSummary.warningCount > 0 ? "critical" : "muted"}>
                  문서 불일치 {documentSummary.warningCount}건
                </span>
                <span className="status-pill" data-tone={documentSummary.approvedCount > 0 ? "ready" : "muted"}>
                  승인 완료 {documentSummary.approvedCount}건
                </span>
              </div>
            </div>

            {documentSession?.warnings.length ? (
              <div className="warning-stack">
                {documentSession.warnings.map((warning) => (
                  <article className="inline-warning" data-tone={warningTone(warning.type)} key={`${warning.type}-${warning.relatedFields.join("-")}`}>
                    <div>
                      <strong>{warning.type === "ADDRESS_MISMATCH" ? "주소 확인 필요" : warning.type === "LANDLORD_OWNER_MISMATCH" ? "임대인 / 소유자 확인 필요" : "보증금 확인 필요"}</strong>
                      <p>{warning.message}</p>
                    </div>
                    <button
                      type="button"
                      className="ghost-button"
                      onClick={() => {
                        setReviewTab("warnings");
                        setFocusedFieldKey(warning.relatedFields[0] || null);
                      }}
                    >
                      관련 필드 보기
                    </button>
                  </article>
                ))}
              </div>
            ) : null}

            <div className="tab-row">
              {[
                ["all", "전체"],
                ["registry", "등기부등본"],
                ["lease-contract", "임대차 계약서"],
                ["warnings", "불일치"],
              ].map(([value, label]) => (
                <button
                  key={value}
                  type="button"
                  className={reviewTab === value ? "tab-button active" : "tab-button"}
                  onClick={() => setReviewTab(value as ReviewTab)}
                >
                  {label}
                </button>
              ))}
            </div>

            {visibleReviewFields.length === 0 ? (
              <div className="result-placeholder">아직 검토할 추출 결과가 없습니다. 문서를 업로드하거나 처리 완료를 기다려 주세요.</div>
            ) : (
              <div className="review-field-list">
                {visibleReviewFields.map((field) => {
                  const confidence = formatConfidence(field.confidence);
                  const isEditing = editingFieldKey === field.fieldKey;
                  const actionState = fieldActionStates[field.fieldKey] === "loading";
                  return (
                    <article
                      key={field.fieldKey}
                      className="review-field"
                      data-highlighted={focusedFieldKey === field.fieldKey ? "true" : "false"}
                    >
                      <div className="review-field-header">
                        <div>
                          <h3>{documentFieldLabels[field.fieldKey]}</h3>
                          <p>
                            {formatDocumentType(field.sourceDocument)}
                            {field.sourcePage ? ` ${field.sourcePage}페이지` : ""} · {field.fieldKey}
                          </p>
                        </div>
                        <div className="summary-chips">
                          <span className="status-pill" data-tone={confidence.tone}>
                            {confidence.label}
                          </span>
                          <span className="status-pill" data-tone={reviewTone(field.reviewStatus)}>
                            {formatDocumentReviewStatus(field.reviewStatus)}
                          </span>
                        </div>
                      </div>

                      {isEditing ? (
                        <div className="edit-panel">
                          <label className="field-group full-width">
                            <span>수정 값</span>
                            <input value={editingFieldValue} onChange={(event) => setEditingFieldValue(event.target.value)} disabled={actionState} />
                          </label>
                          <div className="card-actions">
                            <button
                              type="button"
                              className="primary-button"
                              disabled={actionState}
                              onClick={() => void onReviewField(field.fieldKey, "EDIT", editingFieldValue)}
                            >
                              {actionState ? "저장 중..." : "수정 승인"}
                            </button>
                            <button type="button" className="ghost-button" disabled={actionState} onClick={() => setEditingFieldKey(null)}>
                              수정 취소
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="review-field-body">
                          <div className="review-value">
                            <strong>{formatFieldValue(field)}</strong>
                          </div>
                          <details className="evidence-panel">
                            <summary>원문 근거 보기</summary>
                            <p>{field.sourceText || "원문 근거가 제공되지 않았습니다."}</p>
                          </details>
                          <div className="card-actions">
                            <button
                              type="button"
                              className="secondary-button"
                              disabled={actionState}
                              onClick={() => void onReviewField(field.fieldKey, "APPROVE")}
                            >
                              {actionState ? "저장 중..." : "승인"}
                            </button>
                            <button type="button" className="ghost-button" disabled={actionState} onClick={() => onStartEditField(field)}>
                              수정
                            </button>
                            <button
                              type="button"
                              className="ghost-button"
                              disabled={actionState}
                              onClick={() => void onReviewField(field.fieldKey, "EXCLUDE")}
                            >
                              제외
                            </button>
                          </div>
                        </div>
                      )}
                    </article>
                  );
                })}
              </div>
            )}

            <div className="apply-panel">
              <div className="missing-summary">
                <span>반영 전 요약</span>
                <strong>
                  반영 예정 {documentSummary.approvedCount}건 · 제외 {documentSummary.excludedCount}건 · 문서 불일치 {documentSummary.warningCount}건
                </strong>
              </div>
              <div className="card-actions">
                <button
                  type="button"
                  className="primary-button"
                  disabled={!documentReady || payloadPreviewState === "loading"}
                  onClick={() => void onPrepareApplyApprovedFields()}
                >
                  {payloadPreviewState === "loading" ? "비교 중..." : "승인한 필드 반영"}
                </button>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setGlobalMessage("문서 자동 입력 없이 계속 진행합니다. 필요한 값은 직접 입력해 주세요.")}
                >
                  지금은 수기 입력으로 계속
                </button>
              </div>
              {payloadPreviewMessage ? <p className="helper-copy">{payloadPreviewMessage}</p> : null}
              {applyPreview ? (
                <div className="preview-panel">
                  <div className="sub-card-header">
                    <div>
                      <h3>반영 비교 요약</h3>
                      <p>
                        승인 필드 {applyPreview.approvedFieldCount}건 중 반영 대상 {applyPreview.items.length}건 · 충돌 {applyPreview.conflictCount}건
                      </p>
                    </div>
                  </div>
                  <div className="review-field-list compact">
                    {applyPreview.items.map((item) => (
                      <article className="preview-row" key={item.id}>
                        <div>
                          <strong>{item.label}</strong>
                          <p>{item.sourceNote}</p>
                        </div>
                        <div className="preview-values">
                          <span>현재: {item.currentValue || "비어 있음"}</span>
                          <span>승인값: {item.incomingValue}</span>
                        </div>
                        <label className="checkbox-row">
                          <input
                            type="checkbox"
                            checked={Boolean(applySelection[item.id])}
                            onChange={(event) =>
                              setApplySelection((previous) => ({
                                ...previous,
                                [item.id]: event.target.checked,
                              }))
                            }
                          />
                          <span>
                            {item.conflict ? "승인값으로 교체" : item.sameAsCurrent ? "출처 노트만 유지" : "반영"}
                          </span>
                        </label>
                        {item.conflict ? (
                          <div className="choice-summary" data-selected={applySelection[item.id] ? "approved" : "current"}>
                            <span>{applySelection[item.id] ? "선택됨: 승인값으로 교체" : "선택됨: 현재 값 유지"}</span>
                            <small>체크를 끄면 현재 입력값을 유지하고, 체크하면 문서 승인값으로 교체합니다.</small>
                          </div>
                        ) : null}
                      </article>
                    ))}
                  </div>
                  <div className="card-actions">
                    <button type="button" className="primary-button" onClick={onApplyApprovedFields}>
                      선택한 방식으로 반영
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
          </section>

          <form className="card" onSubmit={onCreate}>
            <div className="card-header">
              <div>
                <h2>계약 기본 정보</h2>
                <p>문서 자동 입력으로 채운 값도 여기서 다시 확인하고 진단을 시작합니다.</p>
              </div>
              <span className="status-pill" data-tone={createState === "success" ? "ready" : "muted"}>
                {checkReady ? "생성됨" : "생성 전"}
              </span>
            </div>
            <div className="form-grid two-column">
              <label className="field-group">
                <span>도로명 주소</span>
                <SourceNote note={contractFieldSources.addressRoad} />
                <input value={contractForm.addressRoad} onChange={(event) => updateContractField("addressRoad", event.target.value)} disabled={!userIdApplied || createState === "loading"} />
                <FieldError message={contractErrors.addressRoad} />
              </label>
              <label className="field-group">
                <span>지번 주소</span>
                <SourceNote note={contractFieldSources.addressLot} />
                <input value={contractForm.addressLot} onChange={(event) => updateContractField("addressLot", event.target.value)} disabled={!userIdApplied || createState === "loading"} />
              </label>
              <label className="field-group">
                <span>계약 유형</span>
                <SourceNote note={contractFieldSources.contractType} />
                <select
                  value={contractForm.contractType}
                  onChange={(event) => updateContractField("contractType", event.target.value as ContractFormState["contractType"])}
                  disabled={!userIdApplied || createState === "loading"}
                >
                  {contractTypeOptions.map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field-group">
                <span>주택 유형</span>
                <select value={contractForm.housingType} onChange={(event) => updateContractField("housingType", event.target.value as ContractFormState["housingType"])} disabled={!userIdApplied || createState === "loading"}>
                  {housingTypeOptions.map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field-group">
                <span>보증금</span>
                <SourceNote note={contractFieldSources.depositAmount} />
                <input type="number" min="0" value={contractForm.depositAmount} onChange={(event) => updateContractField("depositAmount", event.target.value)} disabled={!userIdApplied || createState === "loading"} />
                <FieldError message={contractErrors.depositAmount} />
              </label>
              <label className="field-group">
                <span>월세</span>
                <SourceNote note={contractFieldSources.monthlyRentAmount} />
                <input type="number" min="0" value={contractForm.monthlyRentAmount} onChange={(event) => updateContractField("monthlyRentAmount", event.target.value)} disabled={!userIdApplied || createState === "loading"} />
                <FieldError message={contractErrors.monthlyRentAmount} />
              </label>
              <label className="field-group">
                <span>계약 예정일</span>
                <SourceNote note={contractFieldSources.contractPlannedDate} />
                <input type="date" value={contractForm.contractPlannedDate} onChange={(event) => updateContractField("contractPlannedDate", event.target.value)} disabled={!userIdApplied || createState === "loading"} />
                <FieldError message={contractErrors.contractPlannedDate} />
              </label>
              <label className="field-group">
                <span>입주 예정일</span>
                <SourceNote note={contractFieldSources.occupancyPlannedDate} />
                <input type="date" value={contractForm.occupancyPlannedDate} onChange={(event) => updateContractField("occupancyPlannedDate", event.target.value)} disabled={!userIdApplied || createState === "loading"} />
                <FieldError message={contractErrors.occupancyPlannedDate} />
              </label>
              <label className="field-group full-width">
                <span>임대인 이름</span>
                <SourceNote note={contractFieldSources.landlordName} />
                <input value={contractForm.landlordName} onChange={(event) => updateContractField("landlordName", event.target.value)} disabled={!userIdApplied || createState === "loading"} />
                <FieldError message={contractErrors.landlordName} />
              </label>
            </div>
            <div className="card-actions">
              <button type="submit" className="primary-button" disabled={!userIdApplied || createState === "loading"}>
                {createState === "loading" ? "진단 시작 중..." : "진단 시작"}
              </button>
            </div>
          </form>

          <div className="card">
            <div className="card-header">
              <div>
                <h2>추가 확인 자료 업로드</h2>
                <p>문서 자동 입력과 별개로 분석 참고용 등기 PDF와 건축물대장 PDF를 등록할 수 있습니다.</p>
              </div>
            </div>
            <div className="split-cards">
              <section className="sub-card">
                <div className="sub-card-header">
                  <div>
                    <h3>분석용 등기 PDF</h3>
                    <p>기존 분석 흐름의 파일 메타데이터 입력입니다.</p>
                  </div>
                  <span className="status-pill" data-tone={sectionStatus?.registryFileStatus === "UPLOADED" ? "ready" : "muted"}>
                    {sectionStatus?.registryFileStatus === "UPLOADED" ? "업로드됨" : "미업로드"}
                  </span>
                </div>
                <label className="field-group">
                  <span>PDF 선택</span>
                  <input
                    type="file"
                    accept=".pdf,application/pdf"
                    disabled={!checkReady || registryUploadState === "loading"}
                    onChange={(event) => {
                      const nextFile = event.target.files?.[0] ?? null;
                      setRegistryFile(nextFile);
                      setRegistryUploadName(nextFile?.name || "");
                    }}
                  />
                  <FieldError message={registryUploadErrors.file} />
                </label>
                <label className="field-group">
                  <span>발급일 (선택)</span>
                  <input type="date" value={registryUploadIssuedDate} onChange={(event) => setRegistryUploadIssuedDate(event.target.value)} disabled={!checkReady || registryUploadState === "loading"} />
                </label>
                <p className="meta-line">{registryUploadName ? `선택 파일: ${registryUploadName}` : "아직 업로드하지 않았습니다."}</p>
                <FieldError message={registryUploadErrors.form} />
                <button type="button" className="secondary-button" disabled={!checkReady || registryUploadState === "loading"} onClick={() => void onUpload("registry")}>
                  {registryUploadState === "loading" ? "업로드 중..." : "등기 PDF 업로드"}
                </button>
              </section>

              <section className="sub-card">
                <div className="sub-card-header">
                  <div>
                    <h3>건축물대장 PDF</h3>
                    <p>자동 입력 대상은 아니며 기존 분석 단계에서만 사용합니다.</p>
                  </div>
                  <span className="status-pill" data-tone={sectionStatus?.buildingLedgerFileStatus === "UPLOADED" ? "ready" : "muted"}>
                    {sectionStatus?.buildingLedgerFileStatus === "UPLOADED" ? "업로드됨" : "미업로드"}
                  </span>
                </div>
                <label className="field-group">
                  <span>PDF 선택</span>
                  <input
                    type="file"
                    accept=".pdf,application/pdf"
                    disabled={!checkReady || buildingUploadState === "loading"}
                    onChange={(event) => {
                      const nextFile = event.target.files?.[0] ?? null;
                      setBuildingFile(nextFile);
                      setBuildingUploadName(nextFile?.name || "");
                    }}
                  />
                  <FieldError message={buildingUploadErrors.file} />
                </label>
                <label className="field-group">
                  <span>발급일 (선택)</span>
                  <input type="date" value={buildingUploadIssuedDate} onChange={(event) => setBuildingUploadIssuedDate(event.target.value)} disabled={!checkReady || buildingUploadState === "loading"} />
                </label>
                <p className="meta-line">{buildingUploadName ? `선택 파일: ${buildingUploadName}` : "아직 업로드하지 않았습니다."}</p>
                <FieldError message={buildingUploadErrors.form} />
                <button type="button" className="secondary-button" disabled={!checkReady || buildingUploadState === "loading"} onClick={() => void onUpload("building")}>
                  {buildingUploadState === "loading" ? "업로드 중..." : "건축물대장 PDF 업로드"}
                </button>
              </section>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <div>
                <h2>수기 확인</h2>
                <p>문서 업로드와 별개로 사람이 직접 확인한 내용을 따로 저장합니다.</p>
              </div>
            </div>
            <div className="split-cards">
              <form className="sub-card" onSubmit={onSaveRegistryFindings}>
                <div className="sub-card-header">
                  <h3>등기 수기 확인</h3>
                  <span className="status-pill" data-tone={sectionStatus?.registryFindingStatus === "COMPLETED" ? "ready" : "muted"}>
                    {sectionStatus?.registryFindingStatus === "COMPLETED" ? "저장됨" : "미저장"}
                  </span>
                </div>
                <label className="field-group">
                  <span>현재 소유자 이름</span>
                  <SourceNote note={registryFieldSources.currentOwnerName} />
                  <input value={registryFindingsForm.currentOwnerName} onChange={(event) => updateRegistryField("currentOwnerName", event.target.value)} disabled={registryFindingState === "loading"} />
                  <FieldError message={registryFindingsErrors.currentOwnerName} />
                </label>
                {(Object.entries(booleanFieldLabels) as Array<[keyof RegistryFindingsFormState, string]>).map(([field, label]) => (
                  <label key={field} className="field-group">
                    <span>{label}</span>
                    <SourceNote note={registryFieldSources[field]} />
                    <select
                      value={registryFindingsForm[field]}
                      onChange={(event) => {
                        const value = event.target.value;
                        updateRegistryField(field, value);
                        if (field === "hasMortgage" && value === "false") {
                          updateRegistryField("seniorDebtAmount", "0");
                        }
                      }}
                      disabled={registryFindingState === "loading"}
                    >
                      <option value="">선택</option>
                      <option value="true">예</option>
                      <option value="false">아니오</option>
                    </select>
                    <FieldError message={registryFindingsErrors[field]} />
                  </label>
                ))}
                <label className="field-group">
                  <span>선순위 채권 금액</span>
                  <SourceNote note={registryFieldSources.seniorDebtAmount} />
                  <input
                    type="number"
                    min="0"
                    value={registryFindingsForm.seniorDebtAmount}
                    onChange={(event) => updateRegistryField("seniorDebtAmount", event.target.value)}
                    disabled={registryFindingState === "loading" || registryFindingsForm.hasMortgage === "false"}
                  />
                  <FieldError message={registryFindingsErrors.seniorDebtAmount} />
                </label>
                <FieldError message={registryFindingsErrors.form} />
                <button type="submit" className="secondary-button" disabled={!checkReady || registryFindingState === "loading"}>
                  {registryFindingState === "loading" ? "저장 중..." : "등기 수기 확인 저장"}
                </button>
              </form>

              <form className="sub-card" onSubmit={onSaveBuildingLedgerFindings}>
                <div className="sub-card-header">
                  <h3>건축물대장 수기 확인</h3>
                  <span className="status-pill" data-tone={sectionStatus?.buildingLedgerFindingStatus === "COMPLETED" ? "ready" : "muted"}>
                    {sectionStatus?.buildingLedgerFindingStatus === "COMPLETED" ? "저장됨" : "미저장"}
                  </span>
                </div>
                <label className="field-group">
                  <span>공부상 용도</span>
                  <input value={buildingLedgerForm.usage} onChange={(event) => setBuildingLedgerForm({ ...buildingLedgerForm, usage: event.target.value })} disabled={!checkReady || buildingFindingState === "loading"} />
                  <FieldError message={buildingLedgerErrors.usage} />
                </label>
                {[
                  ["isResidentialUseConfirmed", "주거용 여부가 확인되는지"],
                  ["isViolationBuilding", "위반건축물인지"],
                  ["isUnitConfirmed", "호실이 확인되는지"],
                  ["isContractAreaConsistent", "계약 면적과 공부상 면적이 일치하는지"],
                ].map(([field, label]) => (
                  <label key={field} className="field-group">
                    <span>{label}</span>
                    <select
                      value={buildingLedgerForm[field as keyof BuildingLedgerFindingsFormState] as string}
                      onChange={(event) =>
                        setBuildingLedgerForm({
                          ...buildingLedgerForm,
                          [field]: event.target.value,
                        })
                      }
                      disabled={!checkReady || buildingFindingState === "loading"}
                    >
                      <option value="">선택</option>
                      <option value="true">예</option>
                      <option value="false">아니오</option>
                    </select>
                    <FieldError message={buildingLedgerErrors[field]} />
                  </label>
                ))}
                <label className="field-group">
                  <span>사용승인일 (선택)</span>
                  <input type="date" value={buildingLedgerForm.approvalDate} onChange={(event) => setBuildingLedgerForm({ ...buildingLedgerForm, approvalDate: event.target.value })} disabled={!checkReady || buildingFindingState === "loading"} />
                </label>
                <label className="field-group">
                  <span>현장 확인 주택 유형 (선택)</span>
                  <select value={buildingLedgerForm.housingTypeObserved} onChange={(event) => setBuildingLedgerForm({ ...buildingLedgerForm, housingTypeObserved: event.target.value })} disabled={!checkReady || buildingFindingState === "loading"}>
                    <option value="">미입력</option>
                    {housingTypeOptions.map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <FieldError message={buildingLedgerErrors.form} />
                <button type="submit" className="secondary-button" disabled={!checkReady || buildingFindingState === "loading"}>
                  {buildingFindingState === "loading" ? "저장 중..." : "건축물대장 수기 확인 저장"}
                </button>
              </form>
            </div>
          </div>

          <form className="card" onSubmit={onSaveMarketPrice}>
            <div className="card-header">
              <div>
                <h2>시세 입력</h2>
                <p>추정 매매가와 전세 참고 금액을 함께 노출하며, 둘 중 하나 이상이 필요합니다.</p>
              </div>
              <span className="status-pill" data-tone={sectionStatus?.marketPriceStatus === "SAVED" ? "ready" : "muted"}>
                {sectionStatus?.marketPriceStatus === "SAVED" ? "저장됨" : "미저장"}
              </span>
            </div>
            <div className="form-grid two-column">
              <label className="field-group">
                <span>추정 매매가</span>
                <input type="number" min="0" value={marketPriceForm.estimatedMarketValue} onChange={(event) => updateManualMarketPriceField("estimatedMarketValue", event.target.value)} disabled={!checkReady || marketPriceState === "loading"} />
                <FieldError message={marketPriceErrors.estimatedMarketValue} />
              </label>
              <label className="field-group">
                <span>전세 참고 금액</span>
                <input type="number" min="0" value={marketPriceForm.estimatedJeonseValue} onChange={(event) => updateManualMarketPriceField("estimatedJeonseValue", event.target.value)} disabled={!checkReady || marketPriceState === "loading"} />
                <FieldError message={marketPriceErrors.estimatedJeonseValue} />
              </label>
              <label className="field-group">
                <span>출처 메모</span>
                <input value={marketPriceForm.sourceLabel} onChange={(event) => updateManualMarketPriceField("sourceLabel", event.target.value)} disabled={!checkReady || marketPriceState === "loading"} />
                <FieldError message={marketPriceErrors.sourceLabel} />
              </label>
              <label className="field-group">
                <span>기준일</span>
                <input type="date" value={marketPriceForm.referenceDate} onChange={(event) => updateManualMarketPriceField("referenceDate", event.target.value)} disabled={!checkReady || marketPriceState === "loading"} />
                <FieldError message={marketPriceErrors.referenceDate} />
              </label>
            </div>
            <div className="preview-panel" data-testid="market-price-lookup-panel">
              <div className="review-field-header">
                <div>
                  <h3>공공 실거래가 조회</h3>
                  <p>국토교통부 실거래가 표본 기준</p>
                </div>
                <button type="button" className="ghost-button" disabled={!checkReady || marketPriceLookupState === "loading"} onClick={() => void onLookupMarketPrice()}>
                  {marketPriceLookupState === "loading" ? "조회 중..." : "공공 실거래가 조회"}
                </button>
              </div>
              {marketPriceLookupMessage && <p className="helper-copy">{marketPriceLookupMessage}</p>}
              {marketPriceLookup && (
                <dl className="detail-list compact">
                  <div>
                    <dt>추정 매매가</dt>
                    <dd>{formatNumber(marketPriceLookup.estimatedMarketValue)}</dd>
                  </div>
                  <div>
                    <dt>전세 참고 금액</dt>
                    <dd>{formatNumber(marketPriceLookup.estimatedJeonseValue)}</dd>
                  </div>
                  <div>
                    <dt>표본 수</dt>
                    <dd>매매 {marketPriceLookup.marketSampleCount}건 · 전세 {marketPriceLookup.jeonseSampleCount}건</dd>
                  </div>
                  <div>
                    <dt>조회 기준</dt>
                    <dd>{marketPriceLookup.lawdCode || "코드 미확인"} · {marketPriceLookup.dealYmdFrom || "시작월 미확인"}~{marketPriceLookup.dealYmdTo || "종료월 미확인"}</dd>
                  </div>
                  <div>
                    <dt>출처</dt>
                    <dd>{marketPriceLookup.sourceLabel}</dd>
                  </div>
                  {marketPriceLookup.warnings.length > 0 && (
                    <div>
                      <dt>경고</dt>
                      <dd>{marketPriceLookup.warnings.join(" ")}</dd>
                    </div>
                  )}
                </dl>
              )}
              {marketPriceLookup && (
                <div className="card-actions">
                  <button type="button" className="secondary-button" disabled={marketPriceLookup.confidence !== "AVAILABLE"} onClick={onApplyMarketPriceLookup}>
                    조회 결과 적용
                  </button>
                </div>
              )}
            </div>
            <p className="helper-copy">추정 매매가가 없으면 전세가율과 총 위험 노출 비율 계산 일부가 제한될 수 있습니다.</p>
            {marketPriceForm.sourceKind === "MLIT_REAL_TRANSACTION" && (
              <p className="source-note">공공 실거래가 적용됨 · 표본 {marketPriceForm.sampleCount || "0"}건 · {marketPriceForm.dealYmdFrom || "시작월 미확인"}~{marketPriceForm.dealYmdTo || "종료월 미확인"}</p>
            )}
            <FieldError message={marketPriceErrors.form} />
            <div className="card-actions">
              <button type="submit" className="secondary-button" disabled={!checkReady || marketPriceState === "loading"}>
                {marketPriceState === "loading" ? "저장 중..." : "시세 저장"}
              </button>
            </div>
          </form>

          <section className="card">
            <div className="card-header">
              <div>
                <h2>분석 및 결과</h2>
                <p>일부 자료가 비어 있어도 분석은 실행할 수 있습니다. 결과에는 계산 불가 또는 추가 확인 필요가 표시됩니다.</p>
              </div>
              <button type="button" className="primary-button" disabled={!checkReady || analyzeState === "loading"} onClick={() => void onAnalyze()}>
                {analyzeState === "loading" ? "분석 실행 중..." : "현재 입력 기준으로 분석 실행"}
              </button>
            </div>

            <div className="missing-summary">
              <span>누락 상태 요약</span>
              <strong>
                분석용 등기 PDF {sectionStatus?.registryFileStatus === "UPLOADED" ? "완료" : "미완료"} · 건축물대장 PDF {sectionStatus?.buildingLedgerFileStatus === "UPLOADED" ? "완료" : "미완료"} · 시세 {sectionStatus?.marketPriceStatus === "SAVED" ? "완료" : "미완료"}
              </strong>
            </div>

            <div className="result-shell">
              <div className="tab-row">
                <button type="button" className={activeTab === "report" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("report")}>
                  리포트
                </button>
                <button type="button" className={activeTab === "checklist" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("checklist")}>
                  체크리스트
                </button>
                <button type="button" className="ghost-button" disabled={!checkReady || resultState === "loading"} onClick={() => void refreshResults()}>
                  결과 다시 불러오기
                </button>
              </div>

              {accessMode === "denied" ? (
                <div className="access-panel">
                  <h3>접근이 거부되었습니다</h3>
                  <p>현재 User ID로는 이 진단 요청 또는 문서 세션에 접근할 수 없습니다. 다른 User ID로 생성된 요청일 수 있습니다.</p>
                  <dl className="session-meta">
                    <div>
                      <dt>현재 User ID</dt>
                      <dd>{activeUserId || "미적용"}</dd>
                    </div>
                    <div>
                      <dt>현재 checkId</dt>
                      <dd>{checkId || "없음"}</dd>
                    </div>
                    <div>
                      <dt>현재 문서 세션</dt>
                      <dd>{documentIntakeSessionId || "없음"}</dd>
                    </div>
                  </dl>
                  <p className="access-panel-note">
                    다시 적용할 User ID: <strong>{pendingUserId || "입력 필요"}</strong>
                  </p>
                  <div className="card-actions">
                    <button type="button" className="primary-button" disabled={!pendingUserId} onClick={onApplyUserId}>
                      User ID 다시 적용
                    </button>
                    <button type="button" className="secondary-button" onClick={onStartFresh}>
                      새 진단 시작
                    </button>
                  </div>
                </div>
              ) : resultState === "loading" ? (
                <div className="result-placeholder">결과를 불러오는 중입니다.</div>
              ) : resultState === "not_ready" ? (
                <div className="result-placeholder">
                  <p>{resultMessage}</p>
                  <button type="button" className="secondary-button" disabled={!checkReady} onClick={() => void refreshResults()}>
                    결과 다시 불러오기
                  </button>
                </div>
              ) : resultState === "error" ? (
                <div className="result-placeholder">{resultMessage}</div>
              ) : resultState === "ready" && activeTab === "report" && report ? (
                <div className="report-layout">
                  <section className="report-summary-panel">
                    <span className="risk-badge" data-risk={report.riskLevel}>
                      {formatRiskLevel(report.riskLevel)}
                    </span>
                    <h3>{report.summary}</h3>
                    <p>생성 시각: {formatDate(report.generatedAt)}</p>
                  </section>

                  <section className="result-section">
                    <h4>핵심 위험 사유</h4>
                    <ul className="reason-list">
                      {report.coreReasons.slice(0, 5).map((reason) => (
                        <li key={reason.code}>
                          <strong>{reason.title}</strong>
                          <span>{formatRiskLevel(reason.riskLevel)}</span>
                          <p>{reason.detail}</p>
                        </li>
                      ))}
                    </ul>
                  </section>

                  <section className="result-section two-column-info">
                    <div>
                      <h4>등기 섹션</h4>
                      <p>{report.registry.summary}</p>
                      <dl className="detail-list">
                        <div>
                          <dt>발급일</dt>
                          <dd>{formatDate(report.registry.issuedDate?.value)}</dd>
                        </div>
                        <div>
                          <dt>현재 소유자</dt>
                          <dd>{report.registry.currentOwnerName?.value || "확인 자료 부족"}</dd>
                        </div>
                        <div>
                          <dt>임대인 일치 여부</dt>
                          <dd>{formatBoolean(report.registry.ownerMatchesLandlord?.value)}</dd>
                        </div>
                        <div>
                          <dt>선순위 채권</dt>
                          <dd>{formatNumber(report.registry.seniorDebtAmount?.value)}</dd>
                        </div>
                      </dl>
                    </div>
                    <div>
                      <h4>건축물 섹션</h4>
                      <p>{report.buildingLedger.summary}</p>
                      <dl className="detail-list">
                        <div>
                          <dt>발급일</dt>
                          <dd>{formatDate(report.buildingLedger.issuedDate?.value)}</dd>
                        </div>
                        <div>
                          <dt>용도</dt>
                          <dd>{report.buildingLedger.usage?.value || "확인 자료 부족"}</dd>
                        </div>
                        <div>
                          <dt>주거용 확인</dt>
                          <dd>{formatBoolean(report.buildingLedger.isResidentialUseConfirmed?.value)}</dd>
                        </div>
                        <div>
                          <dt>위반건축물</dt>
                          <dd>{formatBoolean(report.buildingLedger.isViolationBuilding?.value)}</dd>
                        </div>
                      </dl>
                    </div>
                  </section>

                  <section className="result-section two-column-info">
                    <div>
                      <h4>보증금 위험</h4>
                      <p>{report.depositRisk.summary}</p>
                      <dl className="detail-list">
                        <div>
                          <dt>추정 매매가</dt>
                          <dd>{formatNumber(report.depositRisk.estimatedMarketValue?.value)}</dd>
                        </div>
                        <div>
                          <dt>전세 참고 금액</dt>
                          <dd>{formatNumber(report.depositRisk.estimatedJeonseValue?.value)}</dd>
                        </div>
                        <div>
                          <dt>전세가율</dt>
                          <dd>{formatCalculation(report.depositRisk.jeonseRatio.value, report.depositRisk.jeonseRatio.calculationStatus, report.depositRisk.jeonseRatio.note)}</dd>
                        </div>
                        <div>
                          <dt>총 위험 노출 비율</dt>
                          <dd>{formatCalculation(report.depositRisk.totalExposureRatio.value, report.depositRisk.totalExposureRatio.calculationStatus, report.depositRisk.totalExposureRatio.note)}</dd>
                        </div>
                      </dl>
                    </div>
                    <div>
                      <h4>회수 시뮬레이션</h4>
                      <p>{report.recoverySimulation.note || "현재 확인된 자료 기준 계산 결과입니다."}</p>
                      <dl className="detail-list">
                        <div>
                          <dt>예상 낙찰가</dt>
                          <dd>{formatNumber(report.recoverySimulation.estimatedAuctionValue?.value)}</dd>
                        </div>
                        <div>
                          <dt>회수 가능 보증금</dt>
                          <dd>{formatNumber(report.recoverySimulation.recoverableDepositAmount?.value)}</dd>
                        </div>
                        <div>
                          <dt>예상 부족액</dt>
                          <dd>{formatNumber(report.recoverySimulation.shortfallAmount?.value)}</dd>
                        </div>
                      </dl>
                    </div>
                  </section>

                  <section className="result-section">
                    <h4>추가 확인 필요 항목</h4>
                    <ul className="plain-list">
                      {report.additionalChecks.map((item) => (
                        <li key={item}>{item}</li>
                      ))}
                    </ul>
                  </section>

                  <section className="result-section">
                    <h4>출처 메모</h4>
                    <dl className="detail-list">
                      <div>
                        <dt>시세 출처</dt>
                        <dd>
                          {report.depositRisk.sourceLabel?.value || "확인 자료 부족"} · {report.depositRisk.sourceLabel ? formatSourceType(report.depositRisk.sourceLabel.sourceType) : "미확인"}
                        </dd>
                      </div>
                      <div>
                        <dt>기준일</dt>
                        <dd>{formatDate(report.depositRisk.referenceDate?.value)}</dd>
                      </div>
                    </dl>
                  </section>
                </div>
              ) : resultState === "ready" && activeTab === "checklist" && checklist ? (
                <div className="checklist-layout">
                  {checklist.sections.map((section) => (
                    <section key={section.stage} className="result-section">
                      <h4>{section.title}</h4>
                      <ul className="plain-list">
                        {section.items.map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </section>
                  ))}
                </div>
              ) : (
                <div className="result-placeholder">{resultMessage}</div>
              )}
            </div>
          </section>
        </section>
      </main>
    </div>
  );
}

export default App;
