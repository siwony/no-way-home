# UI/UX Plan: 주택 계약 위험도 진단 서비스

Status: READY_FOR_DIRECTOR_REVIEW

## User Goal

사용자는 계약 기본 정보, 문서 업로드, 사람이 직접 확인한 문서 핵심 사실, 수동 시세 정보를 순서대로 저장한 뒤 현재 확인된 자료 기준의 위험 신호 리포트와 계약 체크리스트를 조회할 수 있어야 한다.

## Primary Flow

1. 사용자는 필수 계약 정보로 `POST /api/house-checks`를 호출해 진단 요청을 생성한다.
2. 사용자는 등기부등본 PDF와 건축물대장 PDF를 각각 업로드한다.
3. 사용자는 업로드 자체와 별개로 `문서 확인 입력` 단계에서 사람이 직접 읽은 구조화 확인 결과를 저장한다.
4. 사용자는 추정 시세 또는 전세가 정보를 출처, 기준일과 함께 저장한다.
5. 사용자는 `POST /api/house-checks/{checkId}/analyze`를 실행한다.
6. 사용자는 `GET /api/house-checks/{checkId}/report`와 `GET /api/house-checks/{checkId}/checklist`로 결과를 조회한다.

## API And Flow Decisions

### 1. Phase 1 manual structured findings capture

- Director 질문 1에 대한 결정: 수동 구조화 입력은 `진단 요청 생성` 단계에 넣지 않고, 업로드 이후 별도 `문서 확인 입력` 단계로 둔다.
- 이유: 계약 기본 정보 입력과 문서 해석 입력을 분리해야 업로드됨, 미업로드, 업로드됐지만 아직 사람이 확인하지 않음 상태를 명확히 구분할 수 있다.
- 권장 API:
- `PUT /api/house-checks/{checkId}/registry-findings`
- `PUT /api/house-checks/{checkId}/building-ledger-findings`
- 최소 저장 원칙: 자유서술 메모보다 분석에 직접 쓰는 구조화 필드를 우선한다.
- 등기 확인 입력 최소 범위:
- `currentOwnerName`
- `ownerMatchesLandlord`
- `hasTrustRegistration`
- `hasSeizure`
- `hasProvisionalSeizure`
- `hasProvisionalDisposition`
- `hasAuctionProceeding`
- `hasLeaseRegistration`
- `hasMortgage`
- `seniorDebtAmount`
- 건축물대장 확인 입력 최소 범위:
- `usage`
- `isResidentialUseConfirmed`
- `isViolationBuilding`
- `isUnitConfirmed`
- `isContractAreaConsistent`
- `approvalDate`
- `housingTypeObserved`

### 2. Minimum user-entered upload metadata

- Director 질문 2에 대한 결정: 업로드 시 사용자가 직접 입력하는 최소 메타데이터는 문서별 `issuedDate` 1개만 둔다.
- `issuedDate`는 optional이다. 입력하지 않으면 업로드는 성공시키고, 리포트에서 `발급일 미입력` 또는 `자료 기준 시점 확인 필요`로 노출한다.
- Phase 1에서는 `documentReferenceDate`와 자유 입력 `note`를 받지 않는다.
- 서버가 자동 보존해야 하는 메타데이터:
- `originalFileName`
- `mimeType`
- `fileSize`
- `uploadedAt`
- `storageKey`
- `documentType`

## API-Facing Section Status Model

- `registryFileStatus`: `MISSING | UPLOADED`
- `registryFindingStatus`: `NOT_STARTED | COMPLETED`
- `buildingLedgerFileStatus`: `MISSING | UPLOADED`
- `buildingLedgerFindingStatus`: `NOT_STARTED | COMPLETED`
- `marketPriceStatus`: `MISSING | SAVED`
- `analysisStatus`: `NOT_RUN | RUNNING | COMPLETED | FAILED`
- `reportAvailability`: `NOT_READY | AVAILABLE`

## Screens Or Interfaces

- `POST /api/house-checks`: 계약 기본 정보 저장. 성공 응답은 `checkId`와 현재 섹션 상태를 함께 반환한다.
- `POST /api/house-checks/{checkId}/registry-file`: `multipart/form-data` PDF 업로드와 optional `issuedDate` 저장.
- `POST /api/house-checks/{checkId}/building-ledger-file`: `multipart/form-data` PDF 업로드와 optional `issuedDate` 저장.
- `PUT /api/house-checks/{checkId}/registry-findings`: 등기 수동 확인 결과 저장.
- `PUT /api/house-checks/{checkId}/building-ledger-findings`: 건축물대장 수동 확인 결과 저장.
- `POST /api/house-checks/{checkId}/market-price`: 사용자 입력 시세 저장. 최소 필드: `estimatedMarketValue` 또는 `estimatedJeonseValue`, `sourceLabel`, `referenceDate`.
- `POST /api/house-checks/{checkId}/analyze`: 현재 저장된 자료 기준으로 분석 실행. 문서나 시세가 빠져 있어도 실행 가능해야 하며, 누락 사유는 리포트에 남긴다.
- `GET /api/house-checks/{checkId}/report`: 종합 위험도, 한 줄 요약, 핵심 위험 사유, 문서별 분석 상태, 계산값 또는 계산 불가 이유를 반환한다.
- `GET /api/house-checks/{checkId}/checklist`: 계약 전, 계약 직전, 계약 후 섹션별 확인 항목을 반환한다.

## States

- Loading:
- 업로드 중, 수동 확인 저장 중, 시세 저장 중, 분석 실행 중, 리포트 조회 중 상태를 구분한다.
- 분석 실행 중에는 `analysisStatus=RUNNING`을 반환하고 중복 실행은 막거나 동일 요청으로 처리한다.
- Empty:
- 진단 요청만 있고 문서 업로드가 없는 상태
- 문서는 업로드됐지만 수동 확인 결과가 없는 상태
- 시세 정보가 없는 상태
- 아직 분석을 실행하지 않은 상태
- Success:
- 요청 생성 완료
- 파일 업로드 완료
- 문서 확인 입력 완료
- 시세 저장 완료
- 리포트/체크리스트 조회 가능
- Validation:
- 필수 계약 정보 누락
- 금액 음수 또는 형식 오류
- `contractType`, `housingType` enum 오류
- PDF 이외 파일 업로드
- `issuedDate`, `referenceDate`, `approvalDate` 형식 오류
- 등기/건축물 확인 입력에서 boolean 또는 금액 필드 형식 오류
- Error:
- `HOUSE_CHECK_NOT_FOUND`
- `INVALID_CONTRACT_TYPE`
- `INVALID_DEPOSIT_AMOUNT`
- `REGISTRY_FILE_REQUIRED`
- `MARKET_PRICE_NOT_AVAILABLE`
- `ANALYSIS_NOT_READY`
- 저장소 업로드 실패 또는 분석 실패 시 일반 오류
- Permission denied:
- `ACCESS_DENIED`
- 타인 소유 요청 조회, 분석 실행, 문서 업로드, 리포트 조회, 체크리스트 조회를 모두 차단한다.

## Report And Messaging Constraints

- 자동 계산 결과와 사용자 수동 입력 사실을 섞어 쓰지 않는다.
- 문서 카드나 응답 필드는 `업로드됨`, `수동 확인 완료`, `자동 판독 아님`을 명시한다.
- `SAFE`라도 `안전하다`, `계약해도 된다` 같은 표현은 금지한다.
- 권장 문구:
- `현재 확인된 자료 기준 특이 위험은 확인되지 않았습니다.`
- `현재 확인된 자료 기준으로 보증금 회수 위험이 높을 수 있습니다.`
- `추가 확인이 필요한 항목이 있습니다.`
- 계산할 수 없는 값은 `null` 또는 `calculationStatus=NOT_AVAILABLE`로 내려주고 이유 문구를 함께 포함한다.
- 핵심 위험 사유는 최대 5개로 제한하고, 각 사유는 짧은 설명과 `추가 확인 필요` 문구를 우선한다.
- 체크리스트 문구는 지시형보다 확인형으로 유지한다.

## Simple Feature Checklist

- [ ] `POST /api/house-checks`가 필수 계약 정보 검증 후 `checkId`와 섹션 상태를 반환한다.
- [ ] 문서 업로드 2종은 PDF만 허용하고, 시스템 메타데이터와 optional `issuedDate`를 저장한다.
- [ ] 수동 구조화 확인 결과는 업로드와 분리된 별도 endpoint에 저장된다.
- [ ] 리포트 응답은 문서 업로드 상태와 수동 확인 완료 상태를 구분해서 반환한다.
- [ ] 시세 저장 endpoint는 금액, 출처, 기준일을 저장하고 출처 누락 시 검증 오류를 반환한다.
- [ ] 분석은 문서 또는 시세가 일부 없어도 실행 가능하며, 누락 데이터는 계산 불가 또는 추가 확인 필요 사유로 남는다.
- [ ] 리포트는 위험도, 한 줄 요약, 핵심 위험 사유, 등기 상태, 건축물 상태, 보증금 위험 분석, 회수 시뮬레이션, 추가 확인 필요 항목을 포함한다.
- [ ] 체크리스트는 계약 전, 계약 직전, 계약 후 세 구간으로 반환된다.
- [ ] 접근 권한이 없는 사용자는 요청, 문서, 리포트, 체크리스트에 접근할 수 없다.
- [ ] 사용자 문구는 단정형 표현 없이 `현재 확인된 자료 기준` 문장을 유지한다.

## Developer Notes

- `POST /api/house-checks/{checkId}/analyze`는 강제 선행 조건을 최소화한다. 계약 기본 정보만 있으면 실행은 허용하고, 부족한 입력은 리포트 사유로 처리한다.
- 수동 구조화 입력은 파일 엔터티와 분리 저장하는 쪽이 이후 OCR 자동 추출 단계와 병행하기 쉽다.
- 응답 DTO는 값과 함께 `sourceType` 또는 `inputOrigin`을 노출해 `USER_ENTERED`, `UPLOADED_FILE_METADATA`, `CALCULATED`를 구분하는 편이 안전하다.

## Open Questions

- 없음
