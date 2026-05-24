import type {
  CalculationStatus,
  DocumentIntakeFieldReviewStatus,
  DocumentIntakeProcessingStatus,
  DocumentIntakeSessionResponse,
  ReportValueSourceType,
  RiskLevel,
  SectionStatusResponse,
} from "./types";

const numberFormatter = new Intl.NumberFormat("ko-KR");
const dateFormatter = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

export function formatNumber(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return "확인 자료 부족";
  }

  return `${numberFormatter.format(value)}원`;
}

export function formatDate(value: string | null | undefined) {
  if (!value) {
    return "확인 자료 부족";
  }

  return dateFormatter.format(new Date(value));
}

export function formatBoolean(value: boolean | null | undefined) {
  if (value === null || value === undefined) {
    return "확인 자료 부족";
  }

  return value ? "예" : "아니오";
}

export function formatSourceType(sourceType: ReportValueSourceType) {
  switch (sourceType) {
    case "USER_ENTERED":
      return "사용자 입력";
    case "UPLOADED_FILE_METADATA":
      return "업로드 문서 메타데이터";
    case "CALCULATED":
      return "계산값";
    default:
      return sourceType;
  }
}

export function formatRiskLevel(level: RiskLevel) {
  const labels: Record<RiskLevel, string> = {
    SAFE: "SAFE / 낮음",
    CAUTION: "CAUTION / 주의",
    DANGER: "DANGER / 높음",
    CRITICAL: "CRITICAL / 매우 높음",
  };

  return labels[level];
}

export function formatCalculation(value: string | number | null | undefined, status: CalculationStatus, note?: string | null) {
  if (status === "NOT_AVAILABLE" || value === null || value === undefined) {
    return note ? `계산 불가 · ${note}` : "계산 불가";
  }

  if (typeof value === "number") {
    return `${value.toFixed(2)}%`;
  }

  return String(value);
}

export function formatDocumentProcessingStatus(status: DocumentIntakeProcessingStatus) {
  const labels: Record<DocumentIntakeProcessingStatus, string> = {
    UPLOADED: "업로드됨",
    EXTRACTING: "추출 중",
    REVIEW_REQUIRED: "검토 필요",
    APPROVED: "승인 완료",
    FAILED: "실패",
    DELETED: "삭제됨",
  };

  return labels[status];
}

export function formatDocumentReviewStatus(status: DocumentIntakeFieldReviewStatus) {
  const labels: Record<DocumentIntakeFieldReviewStatus, string> = {
    REVIEW_REQUIRED: "검토 필요",
    APPROVED: "승인됨",
    EDITED: "수정 승인",
    EXCLUDED: "제외됨",
  };

  return labels[status];
}

export function formatDocumentType(documentType: "REGISTRY" | "LEASE_CONTRACT") {
  return documentType === "REGISTRY" ? "등기부등본" : "임대차 계약서";
}

function resolveDocumentProgress(session: DocumentIntakeSessionResponse | null, documentType: "REGISTRY" | "LEASE_CONTRACT") {
  const document = session?.documents.find((item) => item.documentType === documentType);
  return document ? formatDocumentProcessingStatus(document.processingStatus) : "미업로드";
}

function resolveReviewProgress(session: DocumentIntakeSessionResponse | null) {
  if (!session) {
    return "미시작";
  }

  const reviewRequiredCount = session.fields.filter((field) => field.reviewStatus === "REVIEW_REQUIRED").length;
  if (reviewRequiredCount > 0) {
    return `${reviewRequiredCount}건 검토 필요`;
  }

  return session.fields.length > 0 ? "검토 완료" : "대기 중";
}

export function buildProgressItems(
  status: SectionStatusResponse | null,
  documentSession: DocumentIntakeSessionResponse | null,
  hasAppliedDocumentFields: boolean,
) {
  if (!status && !documentSession) {
    return [
      ["문서 세션", "미시작"],
      ["등기 문서", "미업로드"],
      ["계약서 문서", "미업로드"],
      ["추출 검토", "미시작"],
      ["승인 반영", "미시작"],
      ["계약 정보", "미시작"],
      ["분석용 등기 PDF", "미시작"],
      ["등기 수기 확인", "미시작"],
      ["건축물대장 PDF", "미시작"],
      ["건축물대장 수기 확인", "미시작"],
      ["시세 입력", "미시작"],
      ["분석", "미시작"],
      ["결과", "결과 준비 안 됨"],
    ];
  }

  return [
    ["문서 세션", documentSession ? "준비됨" : "미시작"],
    ["등기 문서", resolveDocumentProgress(documentSession, "REGISTRY")],
    ["계약서 문서", resolveDocumentProgress(documentSession, "LEASE_CONTRACT")],
    ["추출 검토", resolveReviewProgress(documentSession)],
    ["승인 반영", hasAppliedDocumentFields ? "반영됨" : "미반영"],
    ["계약 정보", status ? "생성됨" : "미시작"],
    ["분석용 등기 PDF", status?.registryFileStatus === "UPLOADED" ? "업로드됨" : "미시작"],
    ["등기 수기 확인", status?.registryFindingStatus === "COMPLETED" ? "저장됨" : "미시작"],
    ["건축물대장 PDF", status?.buildingLedgerFileStatus === "UPLOADED" ? "업로드됨" : "미시작"],
    ["건축물대장 수기 확인", status?.buildingLedgerFindingStatus === "COMPLETED" ? "저장됨" : "미시작"],
    ["시세 입력", status?.marketPriceStatus === "SAVED" ? "저장됨" : "미시작"],
    ["분석", status?.analysisStatus === "COMPLETED" ? "분석 완료" : status?.analysisStatus === "RUNNING" ? "분석 중" : "분석 전"],
    ["결과", status?.reportAvailability === "AVAILABLE" ? "준비됨" : "결과 준비 안 됨"],
  ];
}
