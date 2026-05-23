import { useMemo, useState } from "react";
import { ApiError, houseCheckApi } from "./api";
import { buildProgressItems, formatBoolean, formatCalculation, formatDate, formatNumber, formatRiskLevel, formatSourceType } from "./format";
import type {
  BuildingLedgerFindingsFormState,
  ContractFormState,
  HouseChecklistResponse,
  HouseRiskReportResponse,
  MarketPriceFormState,
  RegistryFindingsFormState,
  SectionStatusResponse,
} from "./types";
import {
  applyAccessDeniedReset,
  validateBuildingLedgerFindings,
  validateContract,
  validateMarketPrice,
  validateRegistryFindings,
  validateUpload,
  type ValidationErrors,
} from "./validation";

type AsyncState = "idle" | "loading" | "success" | "error";
type ResultState = "idle" | "loading" | "ready" | "not_ready" | "error";
type ResultTab = "report" | "checklist";
type AccessMode = "none" | "boundary_reset" | "denied";

const sessionKeys = {
  userId: "house-risk-agent-prompts.user-id",
  checkId: "house-risk-agent-prompts.check-id",
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
};

function readSessionValue(key: string) {
  return window.sessionStorage.getItem(key) || "";
}

function readCheckId() {
  return window.sessionStorage.getItem(sessionKeys.checkId);
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p className="field-error">{message}</p>;
}

function App() {
  const [draftUserId, setDraftUserId] = useState(() => readSessionValue(sessionKeys.userId));
  const [activeUserId, setActiveUserId] = useState(() => readSessionValue(sessionKeys.userId));
  const [checkId, setCheckId] = useState<string | null>(() => readCheckId());
  const [sectionStatus, setSectionStatus] = useState<SectionStatusResponse | null>(null);
  const [report, setReport] = useState<HouseRiskReportResponse | null>(null);
  const [checklist, setChecklist] = useState<HouseChecklistResponse | null>(null);
  const [activeTab, setActiveTab] = useState<ResultTab>("report");
  const [resultState, setResultState] = useState<ResultState>("idle");
  const [resultMessage, setResultMessage] = useState("아직 분석을 실행하지 않았습니다.");
  const [accessMode, setAccessMode] = useState<AccessMode>("none");
  const [globalMessage, setGlobalMessage] = useState("먼저 User ID를 적용하면 진단을 시작할 수 있습니다.");

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
  const [analyzeState, setAnalyzeState] = useState<AsyncState>("idle");

  const progressItems = useMemo(() => buildProgressItems(sectionStatus), [sectionStatus]);
  const userIdApplied = activeUserId.trim().length > 0;
  const pendingUserId = draftUserId.trim();
  const checkReady = Boolean(checkId);

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
    setContractForm(initialContractForm);
  };

  const clearMessages = () => {
    setContractErrors({});
    setRegistryUploadErrors({});
    setBuildingUploadErrors({});
    setRegistryFindingsErrors({});
    setBuildingLedgerErrors({});
    setMarketPriceErrors({});
  };

  const syncSession = (nextUserId: string, nextCheckId: string | null) => {
    window.sessionStorage.setItem(sessionKeys.userId, nextUserId);
    if (nextCheckId) {
      window.sessionStorage.setItem(sessionKeys.checkId, nextCheckId);
    } else {
      window.sessionStorage.removeItem(sessionKeys.checkId);
    }
  };

  const handleAccessDenied = (message: string) => {
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

  const handleKnownError = (error: unknown, setErrors?: (errors: ValidationErrors) => void) => {
    if (error instanceof ApiError) {
      if (error.code === "ACCESS_DENIED") {
        handleAccessDenied(error.message);
        return;
      }

      if (error.code === "ANALYSIS_NOT_READY") {
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
    syncSession(activeUserId, status.checkId);
    setAccessMode("none");
    setGlobalMessage(message);
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

  const onApplyUserId = () => {
    const nextUserId = draftUserId.trim();
    if (!nextUserId) {
      setGlobalMessage("User ID를 입력해야 진단을 시작할 수 있습니다.");
      return;
    }

    const boundaryReset = applyAccessDeniedReset(checkId, nextUserId, activeUserId);
    setActiveUserId(nextUserId);
    window.sessionStorage.setItem(sessionKeys.userId, nextUserId);
    clearMessages();

    if (boundaryReset.shouldClearServerData) {
      resetServerData(checkId);
      setAccessMode("boundary_reset");
      setGlobalMessage("User ID가 바뀌어 기존 서버 데이터는 화면에서 제거했습니다. 같은 checkId 접근은 다시 확인해야 합니다.");
      return;
    }

    setAccessMode("none");
    setGlobalMessage(checkId ? "현재 User ID로 이 checkId를 다시 확인할 수 있습니다." : "User ID가 적용되었습니다. 진단을 시작할 수 있습니다.");
  };

  const onStartFresh = () => {
    clearMessages();
    resetServerData(null);
    setAccessMode("none");
    setGlobalMessage(userIdApplied ? "새 진단을 시작할 수 있습니다." : "먼저 User ID를 적용하면 진단을 시작할 수 있습니다.");
    syncSession(activeUserId, null);
  };

  const onCreate = async (event: React.FormEvent) => {
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
        kind === "registry" ? "등기 PDF가 업로드되었습니다." : "건축물대장 PDF가 업로드되었습니다.",
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

  const onSaveRegistryFindings = async (event: React.FormEvent) => {
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

  const onSaveBuildingLedgerFindings = async (event: React.FormEvent) => {
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

  const onSaveMarketPrice = async (event: React.FormEvent) => {
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
            <input
              value={draftUserId}
              onChange={(event) => setDraftUserId(event.target.value)}
              placeholder="예: owner-a"
            />
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
          <form className="card" onSubmit={onCreate}>
            <div className="card-header">
              <div>
                <h2>계약 기본 정보</h2>
                <p>진단 시작 전에 계약 기본값을 먼저 저장합니다.</p>
              </div>
              <span className="status-pill" data-tone={createState === "success" ? "ready" : "muted"}>
                {checkReady ? "생성됨" : "생성 전"}
              </span>
            </div>
            <div className="form-grid two-column">
              <label className="field-group">
                <span>도로명 주소</span>
                <input
                  value={contractForm.addressRoad}
                  onChange={(event) => setContractForm({ ...contractForm, addressRoad: event.target.value })}
                  disabled={!userIdApplied || createState === "loading"}
                />
                <FieldError message={contractErrors.addressRoad} />
              </label>
              <label className="field-group">
                <span>지번 주소</span>
                <input
                  value={contractForm.addressLot}
                  onChange={(event) => setContractForm({ ...contractForm, addressLot: event.target.value })}
                  disabled={!userIdApplied || createState === "loading"}
                />
              </label>
              <label className="field-group">
                <span>계약 유형</span>
                <select
                  value={contractForm.contractType}
                  onChange={(event) =>
                    setContractForm({
                      ...contractForm,
                      contractType: event.target.value as ContractFormState["contractType"],
                    })
                  }
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
                <select
                  value={contractForm.housingType}
                  onChange={(event) =>
                    setContractForm({
                      ...contractForm,
                      housingType: event.target.value as ContractFormState["housingType"],
                    })
                  }
                  disabled={!userIdApplied || createState === "loading"}
                >
                  {housingTypeOptions.map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field-group">
                <span>보증금</span>
                <input
                  type="number"
                  min="0"
                  value={contractForm.depositAmount}
                  onChange={(event) => setContractForm({ ...contractForm, depositAmount: event.target.value })}
                  disabled={!userIdApplied || createState === "loading"}
                />
                <FieldError message={contractErrors.depositAmount} />
              </label>
              <label className="field-group">
                <span>월세</span>
                <input
                  type="number"
                  min="0"
                  value={contractForm.monthlyRentAmount}
                  onChange={(event) => setContractForm({ ...contractForm, monthlyRentAmount: event.target.value })}
                  disabled={!userIdApplied || createState === "loading"}
                />
                <FieldError message={contractErrors.monthlyRentAmount} />
              </label>
              <label className="field-group">
                <span>계약 예정일</span>
                <input
                  type="date"
                  value={contractForm.contractPlannedDate}
                  onChange={(event) => setContractForm({ ...contractForm, contractPlannedDate: event.target.value })}
                  disabled={!userIdApplied || createState === "loading"}
                />
                <FieldError message={contractErrors.contractPlannedDate} />
              </label>
              <label className="field-group">
                <span>입주 예정일</span>
                <input
                  type="date"
                  value={contractForm.occupancyPlannedDate}
                  onChange={(event) => setContractForm({ ...contractForm, occupancyPlannedDate: event.target.value })}
                  disabled={!userIdApplied || createState === "loading"}
                />
                <FieldError message={contractErrors.occupancyPlannedDate} />
              </label>
              <label className="field-group full-width">
                <span>임대인 이름</span>
                <input
                  value={contractForm.landlordName}
                  onChange={(event) => setContractForm({ ...contractForm, landlordName: event.target.value })}
                  disabled={!userIdApplied || createState === "loading"}
                />
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
                <h2>문서 업로드</h2>
                <p>PDF 미리보기는 제공하지 않고, 파일명과 발급일 메타데이터만 표시합니다.</p>
              </div>
            </div>
            <div className="split-cards">
              <section className="sub-card">
                <div className="sub-card-header">
                  <h3>등기부등본 PDF</h3>
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
                  <input
                    type="date"
                    value={registryUploadIssuedDate}
                    onChange={(event) => setRegistryUploadIssuedDate(event.target.value)}
                    disabled={!checkReady || registryUploadState === "loading"}
                  />
                </label>
                <p className="meta-line">{registryUploadName ? `선택 파일: ${registryUploadName}` : "아직 업로드하지 않았습니다."}</p>
                <FieldError message={registryUploadErrors.form} />
                <button
                  type="button"
                  className="secondary-button"
                  disabled={!checkReady || registryUploadState === "loading"}
                  onClick={() => void onUpload("registry")}
                >
                  {registryUploadState === "loading" ? "업로드 중..." : "등기 PDF 업로드"}
                </button>
              </section>

              <section className="sub-card">
                <div className="sub-card-header">
                  <h3>건축물대장 PDF</h3>
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
                  <input
                    type="date"
                    value={buildingUploadIssuedDate}
                    onChange={(event) => setBuildingUploadIssuedDate(event.target.value)}
                    disabled={!checkReady || buildingUploadState === "loading"}
                  />
                </label>
                <p className="meta-line">{buildingUploadName ? `선택 파일: ${buildingUploadName}` : "아직 업로드하지 않았습니다."}</p>
                <FieldError message={buildingUploadErrors.form} />
                <button
                  type="button"
                  className="secondary-button"
                  disabled={!checkReady || buildingUploadState === "loading"}
                  onClick={() => void onUpload("building")}
                >
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
                  <input
                    value={registryFindingsForm.currentOwnerName}
                    onChange={(event) => setRegistryFindingsForm({ ...registryFindingsForm, currentOwnerName: event.target.value })}
                    disabled={!checkReady || registryFindingState === "loading"}
                  />
                  <FieldError message={registryFindingsErrors.currentOwnerName} />
                </label>
                {[
                  ["ownerMatchesLandlord", "임대인과 소유자가 일치하는지"],
                  ["hasTrustRegistration", "신탁등기가 있는지"],
                  ["hasSeizure", "압류가 있는지"],
                  ["hasProvisionalSeizure", "가압류가 있는지"],
                  ["hasProvisionalDisposition", "가처분이 있는지"],
                  ["hasAuctionProceeding", "경매개시결정이 있는지"],
                  ["hasLeaseRegistration", "임차권등기가 있는지"],
                  ["hasMortgage", "근저당이 있는지"],
                ].map(([field, label]) => (
                  <label key={field} className="field-group">
                    <span>{label}</span>
                    <select
                      value={registryFindingsForm[field as keyof RegistryFindingsFormState] as string}
                      onChange={(event) => {
                        const value = event.target.value;
                        const next = {
                          ...registryFindingsForm,
                          [field]: value,
                        } as RegistryFindingsFormState;
                        if (field === "hasMortgage" && value === "false") {
                          next.seniorDebtAmount = "0";
                        }
                        setRegistryFindingsForm(next);
                      }}
                      disabled={!checkReady || registryFindingState === "loading"}
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
                  <input
                    type="number"
                    min="0"
                    value={registryFindingsForm.seniorDebtAmount}
                    onChange={(event) => setRegistryFindingsForm({ ...registryFindingsForm, seniorDebtAmount: event.target.value })}
                    disabled={!checkReady || registryFindingState === "loading" || registryFindingsForm.hasMortgage === "false"}
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
                  <input
                    value={buildingLedgerForm.usage}
                    onChange={(event) => setBuildingLedgerForm({ ...buildingLedgerForm, usage: event.target.value })}
                    disabled={!checkReady || buildingFindingState === "loading"}
                  />
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
                  <input
                    type="date"
                    value={buildingLedgerForm.approvalDate}
                    onChange={(event) => setBuildingLedgerForm({ ...buildingLedgerForm, approvalDate: event.target.value })}
                    disabled={!checkReady || buildingFindingState === "loading"}
                  />
                </label>
                <label className="field-group">
                  <span>현장 확인 주택 유형 (선택)</span>
                  <select
                    value={buildingLedgerForm.housingTypeObserved}
                    onChange={(event) => setBuildingLedgerForm({ ...buildingLedgerForm, housingTypeObserved: event.target.value })}
                    disabled={!checkReady || buildingFindingState === "loading"}
                  >
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
                <input
                  type="number"
                  min="0"
                  value={marketPriceForm.estimatedMarketValue}
                  onChange={(event) => setMarketPriceForm({ ...marketPriceForm, estimatedMarketValue: event.target.value })}
                  disabled={!checkReady || marketPriceState === "loading"}
                />
                <FieldError message={marketPriceErrors.estimatedMarketValue} />
              </label>
              <label className="field-group">
                <span>전세 참고 금액</span>
                <input
                  type="number"
                  min="0"
                  value={marketPriceForm.estimatedJeonseValue}
                  onChange={(event) => setMarketPriceForm({ ...marketPriceForm, estimatedJeonseValue: event.target.value })}
                  disabled={!checkReady || marketPriceState === "loading"}
                />
                <FieldError message={marketPriceErrors.estimatedJeonseValue} />
              </label>
              <label className="field-group">
                <span>출처 메모</span>
                <input
                  value={marketPriceForm.sourceLabel}
                  onChange={(event) => setMarketPriceForm({ ...marketPriceForm, sourceLabel: event.target.value })}
                  disabled={!checkReady || marketPriceState === "loading"}
                />
                <FieldError message={marketPriceErrors.sourceLabel} />
              </label>
              <label className="field-group">
                <span>기준일</span>
                <input
                  type="date"
                  value={marketPriceForm.referenceDate}
                  onChange={(event) => setMarketPriceForm({ ...marketPriceForm, referenceDate: event.target.value })}
                  disabled={!checkReady || marketPriceState === "loading"}
                />
                <FieldError message={marketPriceErrors.referenceDate} />
              </label>
            </div>
            <p className="helper-copy">
              추정 매매가가 없으면 전세가율과 총 위험 노출 비율 계산 일부가 제한될 수 있습니다.
            </p>
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
                등기 PDF {sectionStatus?.registryFileStatus === "UPLOADED" ? "완료" : "미완료"} · 건축물대장 PDF{" "}
                {sectionStatus?.buildingLedgerFileStatus === "UPLOADED" ? "완료" : "미완료"} · 시세{" "}
                {sectionStatus?.marketPriceStatus === "SAVED" ? "완료" : "미완료"}
              </strong>
            </div>

            <div className="result-shell">
              <div className="tab-row">
                <button type="button" className={activeTab === "report" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("report")}>
                  리포트
                </button>
                <button
                  type="button"
                  className={activeTab === "checklist" ? "tab-button active" : "tab-button"}
                  onClick={() => setActiveTab("checklist")}
                >
                  체크리스트
                </button>
                <button type="button" className="ghost-button" disabled={!checkReady || resultState === "loading"} onClick={() => void refreshResults()}>
                  결과 다시 불러오기
                </button>
              </div>

              {accessMode === "denied" ? (
                <div className="access-panel">
                  <h3>접근이 거부되었습니다</h3>
                  <p>현재 User ID로는 이 진단 요청에 접근할 수 없습니다. 다른 User ID로 생성된 요청일 수 있습니다.</p>
                  <dl className="session-meta">
                    <div>
                      <dt>현재 User ID</dt>
                      <dd>{activeUserId || "미적용"}</dd>
                    </div>
                    <div>
                      <dt>현재 checkId</dt>
                      <dd>{checkId || "없음"}</dd>
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
                          <dd>
                            {formatCalculation(
                              report.depositRisk.totalExposureRatio.value,
                              report.depositRisk.totalExposureRatio.calculationStatus,
                              report.depositRisk.totalExposureRatio.note,
                            )}
                          </dd>
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
                          {report.depositRisk.sourceLabel?.value || "확인 자료 부족"} ·{" "}
                          {report.depositRisk.sourceLabel ? formatSourceType(report.depositRisk.sourceLabel.sourceType) : "미확인"}
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
