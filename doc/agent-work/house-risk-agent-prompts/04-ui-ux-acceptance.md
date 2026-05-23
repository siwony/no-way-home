# UI/UX Acceptance: 주택 계약 위험도 진단 서비스

Status: APPROVED

Decision: APPROVED

## Accepted User Flow

- 계약 기본 정보 저장 -> 등기부등본 PDF 업로드 -> 건축물대장 PDF 업로드 -> 등기 수동 확인 입력 -> 건축물대장 수동 확인 입력 -> 시세 정보 저장 -> 분석 실행 -> 리포트/체크리스트 조회 흐름이 승인안과 일치합니다.
- `estimatedJeonseValue`만 저장한 시세 입력도 `marketPriceStatus=SAVED`인 사용자 입력으로 유지되며, 분석 후 리포트에서 값이 그대로 노출됩니다.
- 다만 매매 시세가 없으면 전세가율, 총 위험 노출 비율, 회수 시뮬레이션은 계속 `NOT_AVAILABLE`로 남고, 그 이유가 현재 자료 기준 문구로 설명됩니다.

## Checklist Review

- [x] `POST /api/house-checks`가 `checkId`와 섹션 상태를 반환합니다.
- [x] 등기부등본/건축물대장 업로드는 PDF만 허용하고 optional `issuedDate`를 받습니다.
- [x] 수동 구조화 확인 결과가 업로드와 분리된 `PUT` endpoint로 저장됩니다.
- [x] 리포트 응답이 `sectionStatus`, `sourceType`, `calculationStatus`로 업로드 상태, 수동 입력, 계산값을 구분합니다.
- [x] 시세 저장 endpoint가 `estimatedMarketValue` 또는 `estimatedJeonseValue`를 받아 금액, 출처, 기준일을 저장하고 누락/형식 오류를 검증합니다.
- [x] `estimatedJeonseValue` 단독 입력이 더 이상 `MARKET_PRICE_REQUIRED`로 오판되지 않고, `estimatedJeonseValue`가 `USER_ENTERED` 값으로 리포트에 유지됩니다.
- [x] 매매 시세 의존 계산은 매매 시세가 없을 때만 제한되며, 이 경우 `MARKET_VALUE_REQUIRED_FOR_RATIO`와 `NOT_AVAILABLE` note로 이유를 분명히 노출합니다.
- [x] 분석은 일부 자료가 없어도 완료되며 누락 데이터는 리포트 요약, 위험 사유, 계산 불가 note로 노출됩니다.
- [x] 리포트가 위험도, 한 줄 요약, 핵심 위험 사유, 등기/건축물 섹션, 보증금 위험 분석, 회수 시뮬레이션, 추가 확인 항목을 포함합니다.
- [x] 체크리스트가 `계약 전`, `계약 직전`, `계약 후` 3구간으로 반환됩니다.
- [x] 소유자 불일치 접근은 `ACCESS_DENIED`로 차단됩니다.
- [x] 요약/사유/체크리스트 문구가 `현재 확인된 자료 기준`과 `확인 필요` 중심 표현을 유지하고 단정형 안전 표현을 피합니다.

## Findings

- 없음.

## Change Requests

- 없음.

## Decision Notes

- 검토 범위는 backend-only UI/UX acceptance입니다. 시각 UI는 검토하지 않았습니다.
- 이번 재검토는 QA-01, QA-02 backend rework 이후 승인된 계약과 리포트 문구가 유지되는지에 집중했습니다.
- 확인 근거: `HouseCheckCommands`, `HouseCheckResponses`, `HouseCheckQueryService`, `HouseRiskAnalysisService`, `MarketPriceAssessmentService`, `MarketPriceRiskAnalyzer`, `HouseRiskAnalysisComponentsTest`, `HouseCheckControllerIntegrationTest`.
- 런타임 확인: `./gradlew test --tests com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest --rerun-tasks` 통과.
- 재검토 결론: 승인된 Phase 1 contract는 유지됐고, 전세가-only 입력은 저장된 사용자 입력으로 취급되며, 매매 시세 의존 지표만 명확하게 unavailable 처리됩니다.

Use one decision value: `APPROVED`, `CHANGES_REQUESTED`, or `BLOCKED`.
