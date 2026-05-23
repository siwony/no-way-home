# UI/UX Plan: 주택 계약 위험도 진단 프론트엔드

Status: READY_FOR_DIRECTOR_REVIEW

## User Goal

사용자는 별도 로그인 없이 `X-User-Id` 경계를 직접 지정한 뒤, 한 건의 주택 계약 진단 요청을 생성하고 필요한 문서와 수기 확인값을 입력해 현재 확인된 자료 기준의 리포트와 체크리스트를 확인할 수 있어야 한다.

## Primary Flow

1. 사용자는 첫 화면 상단의 `User ID` 입력란에 값을 넣고 적용한다. 적용 전에는 모든 저장/조회 액션이 비활성화된다.
2. 사용자는 계약 기본 정보 폼을 작성하고 `진단 시작`을 눌러 `POST /api/house-checks`를 호출한다.
3. 생성 성공 후 프론트는 `checkId`와 섹션 상태를 고정 헤더와 진행 상태 영역에 표시하고, 이후 모든 요청에 같은 `checkId`와 현재 `User ID`를 사용한다.
4. 사용자는 등기부등본 PDF와 건축물대장 PDF를 각각 업로드한다. 각 업로드는 optional `issuedDate`를 함께 보낼 수 있다.
5. 사용자는 업로드와 별개로 `등기 수기 확인`과 `건축물대장 수기 확인` 폼을 각각 작성하고 저장한다.
6. 사용자는 `시세 입력`에서 `estimatedMarketValue` 또는 `estimatedJeonseValue` 중 하나 이상과 `sourceLabel`, `referenceDate`를 저장한다.
7. 사용자는 `분석 실행`을 눌러 `POST /api/house-checks/{checkId}/analyze`를 호출한다. 누락된 자료가 있어도 실행은 허용하되, 프론트는 누락 경고를 먼저 보여준다.
8. 분석 응답이 `COMPLETED`이면 즉시 `GET /api/house-checks/{checkId}/report`와 `GET /api/house-checks/{checkId}/checklist`를 조회해 결과 영역을 채운다.
9. 사용자가 동일 `checkId` 상태에서 `User ID`를 바꾸거나 서버가 `ACCESS_DENIED`를 반환하면, 프론트는 기존 리포트/체크리스트/상태 내용을 즉시 숨기고 접근 거부 화면으로 전환한다.

## Recommended UI Structure

- 기본 진입은 마케팅 랜딩 페이지가 아니라 단일 도구형 화면이다. 기본 route는 바로 진단 워크스페이스로 연결한다.
- 첫 버전은 멀티페이지보다 `단일 route + 섹션 카드` 구조를 권장한다. 이유는 committed API에 `draft/read` endpoint가 없어 생성부터 분석까지를 한 세션 안에서 이어가는 편이 단순하기 때문이다.
- 데스크톱은 `좌측 진행 상태 rail + 우측 작업 영역`, 모바일은 `상단 상태 요약 + 세로 카드 스택`으로 접는다.
- 결과 영역은 긴 한 페이지 스크롤보다 `리포트`와 `체크리스트` 2개 탭 또는 세그먼트 전환을 권장한다.
- 시각 톤은 운영 도구형으로 유지한다. 큰 hero, 장식성 카피, 과한 일러스트는 두지 않는다.

## Screens Or Interfaces

- `앱 셸 / 세션 경계 바`
  - 노출 요소: `User ID` 입력, `적용` 버튼, 현재 `checkId` 표시, `새 진단 시작` 버튼.
  - 보조 문구: `이 화면은 입력한 User ID를 X-User-Id 헤더로 사용합니다. 다른 User ID로 바꾸면 기존 진단 접근이 거부될 수 있습니다.`
  - 규칙: `User ID` 미입력 상태에서는 아래 모든 카드가 disabled 상태여야 한다.
- `진행 상태 rail`
  - 항목: 계약 정보, 등기 PDF, 등기 수기 확인, 건축물대장 PDF, 건축물대장 수기 확인, 시세 입력, 분석, 결과.
  - 상태 소스: 각 mutation 응답의 `SectionStatusResponse`.
  - 표시 값: `미시작`, `업로드됨`, `저장됨`, `분석 전`, `분석 완료`, `결과 준비 안 됨`.
- `계약 기본 정보 카드`
  - API: `POST /api/house-checks`
  - 필드: `addressRoad`, `addressLot`, `contractType`, `housingType`, `depositAmount`, `monthlyRentAmount`, `contractPlannedDate`, `occupancyPlannedDate`, `landlordName`.
  - UI 규칙:
    - `contractType`, `housingType`는 자유 입력이 아니라 select로 제한한다.
    - `monthlyRentAmount`는 월세가 아닌 경우에도 payload 요구사항 때문에 표시하고 기본값 `0`을 허용한다.
    - 생성 성공 후에는 카드 상단에 `checkId`를 복사 가능한 읽기 전용 값으로 노출한다.
- `문서 업로드 카드`
  - 하위 카드 2개: `등기부등본 PDF`, `건축물대장 PDF`.
  - API: `POST /api/house-checks/{checkId}/registry-file`, `POST /api/house-checks/{checkId}/building-ledger-file`
  - 필드: `file`, optional `issuedDate`
  - UI 규칙:
    - 파일 선택과 업로드 실행은 문서별 독립 동작으로 둔다.
    - 파일 내용 미리보기는 제공하지 않는다. 업로드 후에는 파일명, 발급일, 업로드 성공 상태만 노출한다.
    - `accept=".pdf,application/pdf"`로 제한하되, 서버 오류도 그대로 처리한다.
- `수기 확인 카드`
  - 하위 폼 2개: `등기 수기 확인`, `건축물대장 수기 확인`
  - API: `PUT /api/house-checks/{checkId}/registry-findings`, `PUT /api/house-checks/{checkId}/building-ledger-findings`
  - 등기 필드:
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
  - 건축물대장 필드:
    - `usage`
    - `isResidentialUseConfirmed`
    - `isViolationBuilding`
    - `isUnitConfirmed`
    - `isContractAreaConsistent`
    - `approvalDate`
    - `housingTypeObserved`
  - UI 규칙:
    - boolean 값은 모두 명시적 `예/아니오` 선택으로 구현한다. 빈 상태로 submit되지 않게 한다.
    - `hasMortgage=false`이면 `seniorDebtAmount`를 `0`으로 자동 채우거나 disabled 처리하되, payload가 일관되게 전달되도록 한다.
    - 업로드가 없어도 수기 확인을 먼저 저장하지 못하게 막을지 여부는 프론트에서 강제하지 않는다. 다만 `문서 없이 수기 확인만 저장할 수 있습니다` 같은 보조 문구는 두지 않는다. 권장 순서만 안내한다.
- `시세 입력 카드`
  - API: `POST /api/house-checks/{checkId}/market-price`
  - 필드: `estimatedMarketValue`, `estimatedJeonseValue`, `sourceLabel`, `referenceDate`
  - UI 규칙:
    - `estimatedMarketValue`와 `estimatedJeonseValue`를 둘 다 노출하되, `둘 중 하나 이상 필수` 규칙을 명시한다.
    - helper copy: `추정 매매가가 없으면 전세가율과 총 위험 노출 비율 계산 일부가 제한될 수 있습니다.`
    - 저장 후 상태 rail에는 단순히 `시세 저장 완료`만 노출하고, 값 자체는 카드 안에서만 보여준다.
- `분석 및 결과 카드`
  - API: `POST /api/house-checks/{checkId}/analyze`, `GET /api/house-checks/{checkId}/report`, `GET /api/house-checks/{checkId}/checklist`
  - 상단에는 `현재 입력 기준으로 분석 실행` 버튼과 누락 상태 요약을 둔다.
  - 실행 전 경고 문구: `일부 자료가 비어 있어도 분석은 실행할 수 있습니다. 결과에는 계산 불가 또는 추가 확인 필요가 표시됩니다.`
  - 결과 영역 구성:
    - `리포트` 탭: 종합 위험도, 한 줄 요약, 핵심 위험 사유 최대 5개, 등기 섹션, 건축물 섹션, 보증금 위험, 회수 시뮬레이션, 추가 확인 필요 항목
    - `체크리스트` 탭: `계약 전`, `계약 직전`, `계약 후` 3개 그룹
  - 리포트의 숫자/문자 항목은 가능하면 `입력값`, `업로드 메타데이터`, `계산값` 출처 표시를 함께 둔다.
- `접근 거부 화면`
  - 트리거: 어느 endpoint에서든 `403 ACCESS_DENIED`
  - 구성: 경고 제목, 현재 `User ID`, 현재 `checkId`, `User ID 다시 적용`, `새 진단 시작` 액션
  - 금지: 직전 사용자의 리포트 내용, 파일명, 소유자 이름, 위험 사유를 남겨두지 않는다.

## States

- Loading:
- `진단 시작`, 문서별 `업로드`, 수기 확인 `저장`, 시세 `저장`, `분석 실행`, 결과 `다시 불러오기`는 각 카드 단위 loading으로 처리한다.
- 전체 화면 blocking spinner는 쓰지 않는다. 현재 작업 중인 카드만 disabled 처리하고 나머지는 읽기 가능하게 둔다.
- 분석 직후 결과 조회 중에는 리포트/체크리스트 영역에 skeleton 또는 `결과 불러오는 중` 상태를 표시한다.
- Empty:
- `User ID` 미적용 상태: `먼저 User ID를 적용하면 진단을 시작할 수 있습니다.`
- `checkId` 없음: 계약 기본 정보 카드만 활성.
- 문서 업로드 전: 각 문서 카드에 `아직 업로드하지 않았습니다.`
- 수기 확인 전: `업로드 후 사람이 직접 확인한 내용을 저장하세요.`
- 시세 미입력: `추정 매매가 또는 전세 참고 금액이 아직 없습니다.`
- 분석 전: 결과 탭 대신 `아직 분석을 실행하지 않았습니다.` 안내를 노출한다.
- 결과 없음: `분석 완료 후 리포트와 체크리스트가 표시됩니다.`
- Not ready:
- `analysisStatus=RUNNING` 또는 `GET report/checklist`가 `ANALYSIS_NOT_READY`를 반환하면 `분석 결과를 준비 중입니다. 잠시 후 다시 불러오세요.` 상태와 `다시 불러오기` 버튼을 보여준다.
- Success:
- 생성 성공 후: `진단 요청이 생성되었습니다. 아래 단계들을 순서대로 채워 주세요.`
- 업로드 성공 후: 문서 카드 안에 `업로드됨` 뱃지와 업로드한 파일명 표시
- 수기 저장 성공 후: `수기 확인 내용이 저장되었습니다.`
- 시세 저장 성공 후: `시세 입력이 저장되었습니다.`
- 분석 성공 후: `현재 확인된 자료 기준 결과를 불러왔습니다.`
- Validation:
- 계약 폼: 필수값 누락, 음수 금액, enum mismatch, 날짜 누락
- 문서 업로드: PDF 외 파일, 빈 파일, 잘못된 발급일 형식
- 수기 확인: 필수 문자열 누락, 미선택 boolean, 금액 형식 오류
- 시세 입력: 금액 2개 모두 누락, `sourceLabel` 누락, `referenceDate` 누락
- 표시 방식:
  - 서버 `fieldErrors`가 오면 필드 하단에 직접 매핑한다.
  - 서버 field 정보가 빈약하면 카드 상단 요약 오류와 함께 해당 필드 그룹을 강조한다.
- Error:
- `400 VALIDATION_ERROR`: 섹션 내부 에러로 표시하고 다른 카드 상태는 유지한다.
- `400 MARKET_PRICE_NOT_AVAILABLE`: 시세 카드에 `추정 매매가 또는 전세 참고 금액 중 하나는 필요합니다.` 표시
- `404 HOUSE_CHECK_NOT_FOUND`: 현재 `checkId`가 유효하지 않음을 알리고 `새 진단 시작` 유도
- `409 ANALYSIS_NOT_READY`: not-ready 상태로 분기
- 네트워크/알 수 없는 오류: `요청을 완료하지 못했습니다. 입력값은 다시 확인해 주세요.`와 재시도 버튼
- Permission denied:
- `403 ACCESS_DENIED` 발생 시 현재 화면의 서버 기반 데이터는 즉시 제거한다.
- 안내 문구: `현재 User ID로는 이 진단 요청에 접근할 수 없습니다. 다른 User ID로 생성된 요청일 수 있습니다.`
- 허용 액션: `User ID 변경`, `새 진단 시작`
- 금지 동작: access denied 이후 기존 결과를 배경에 그대로 남겨두는 것

## Copy And Messaging

- 화면 제목은 도구형으로 단순하게 둔다. 예: `주택 계약 위험 진단`
- CTA는 기능 중심으로 쓴다. 예: `진단 시작`, `등기 PDF 업로드`, `등기 수기 확인 저장`, `시세 저장`, `분석 실행`, `결과 다시 불러오기`
- 리포트 핵심 문구는 반드시 `현재 확인된 자료 기준` 프레이밍을 유지한다.
- 허용 문구 예시:
  - `현재 확인된 자료 기준 특이 위험은 확인되지 않았습니다.`
  - `현재 확인된 자료 기준으로 보증금 회수 위험이 높을 수 있습니다.`
  - `추가 확인이 필요한 항목이 있습니다.`
- 금지 문구 예시:
  - `안전합니다`
  - `계약해도 됩니다`
  - `보증금을 회수할 수 있습니다`
  - `문제 없습니다`
- 계산 불가 지표는 빈칸으로 숨기지 않고 `계산 불가` 또는 `확인 자료 부족`으로 표시한다.
- 체크리스트 문구는 명령형보다 확인형을 우선한다. 예: `계약 직전 등기를 다시 확인했는지 확인하세요.`
- 위험도 badge는 색상만으로 의미를 전달하지 않는다. `SAFE`, `CAUTION`, `DANGER`, `CRITICAL` 또는 대응 한글 라벨을 함께 표기한다.

## Simple Feature Checklist

- [ ] 기본 route가 곧바로 진단 도구 화면으로 열리고 마케팅 hero 페이지가 없다.
- [ ] `User ID` 적용 전에는 API 호출 버튼이 비활성화되고, 적용 후 모든 요청에 `X-User-Id`가 일관되게 포함된다.
- [ ] 계약 기본 정보 생성 성공 시 `checkId`와 섹션 상태가 UI에 즉시 반영된다.
- [ ] 등기/건축물대장 업로드가 서로 독립적으로 동작하고 PDF 외 파일은 프론트와 서버 양쪽에서 방어된다.
- [ ] 등기 수기 확인과 건축물대장 수기 확인이 별도 저장 동작으로 분리되어 있다.
- [ ] 시세 입력은 `estimatedMarketValue` 또는 `estimatedJeonseValue` 중 하나 이상을 허용하고, 둘 다 비면 저장을 막는다.
- [ ] 분석 버튼은 `checkId` 생성 이후 사용할 수 있고, 누락 데이터가 있어도 실행 자체는 막지 않는다.
- [ ] 분석 성공 시 리포트와 체크리스트를 자동 조회하거나 사용자가 한 번에 확인할 수 있는 흐름이 있다.
- [ ] `estimatedJeonseValue`만 저장된 경우에도 결과 화면은 값을 보여 주고, 비율 계산은 `계산 불가`와 사유 문구로 구분한다.
- [ ] `ANALYSIS_NOT_READY`와 `ACCESS_DENIED`가 일반 오류와 구분된 별도 상태로 보인다.
- [ ] 리포트/체크리스트/파일명/임대인 이름 등 서버 데이터가 브라우저 영구 저장소에 남지 않는다.

## Developer Notes

- 첫 슬라이스는 새 frontend workspace를 분리된 디렉터리 하나로 scaffold하는 구성이 가장 안전하다. 핵심은 기술 선택보다 `backend와 독립 실행 가능`, `API base URL 설정 단순`, `테스트 추가 용이`다.
- 상태 관리는 복잡한 전역 store보다 `현재 User ID`, `currentCheckId`, 각 카드의 local form state, `latest section status`, `report/checklist fetch state` 정도로 제한한다.
- committed API에는 `draft 조회` endpoint가 없다. 따라서 `생성 전/생성 후 편집 폼 복구`는 같은 세션 메모리 흐름을 기본으로 잡는다.
- reload 편의가 필요하면 `User ID`와 `checkId`만 session 범위에 보존할 수 있다. 단, `landlordName`, 업로드 파일, 리포트 payload, 체크리스트 payload는 localStorage/sessionStorage에 저장하지 않는다.
- API client는 공통 error mapper를 두고 `code`, `message`, `fieldErrors`를 섹션 UI가 재사용할 수 있게 한다.
- 결과 화면은 카드/테이블/목록 위주로 단정하게 구현한다. 차트나 시각 장식은 첫 버전 범위에 넣지 않는다.
- 모바일에서는 상태 rail을 상단 accordion 요약으로 줄이고, 주요 CTA를 각 카드 하단에 고정하지 말고 카드 안에서 끝내는 편이 단순하다.

## Open Questions

- committed API에는 `GET /api/house-checks/{checkId}` 또는 저장된 입력값 재조회 endpoint가 없다. 이 계획은 첫 usable UI를 `같은 세션의 생성 -> 입력 -> 분석 -> 결과 확인` 흐름으로 한정한다.
- 만약 Director가 `새로고침 후 미완료 draft 재개`, `기존 check 재편집`, `분석 전 상태 재조회`까지 기대한다면, 이는 프론트 구현 문제가 아니라 별도 backend/API work item으로 다시 열어야 한다.
