# Director Brief: 주택 계약 위험도 진단 프론트엔드

Status: READY_FOR_UI_UX

## Goal

커밋된 backend Phase 1 API를 실제로 사용할 수 있는 첫 프론트엔드 슬라이스를 이 저장소에 추가한다.

이번 슬라이스의 목표는 “완성형 제품”이 아니라, 사용자가 진단 요청을 만들고 필요한 입력을 채운 뒤 분석 결과와 체크리스트를 확인할 수 있는 첫 usable web flow를 만드는 것이다.

## Background

- Backend Phase 1 work is already committed at `520fbff`.
- 현재 저장소에는 frontend project structure가 없다.
- 따라서 이번 작업은 기능 구현과 함께 최소한의 frontend 실행 구조를 처음 도입하는 범위가 포함된다.
- 다만 backend API contract를 다시 설계하는 작업은 이번 slice의 기본 범위가 아니다. 우선 committed API를 소비하는 usable frontend를 만든다.
- MVP에서는 인증 시스템을 새로 만들지 않는다. 대신 backend의 `X-User-Id` 경계를 UI에서 명시적으로 다룰 수 있어야 한다.

## Scope

### Include

- committed Phase 1 API를 소비하는 첫 웹 프론트엔드 진입점
- 사용자가 한 건의 house check를 생성하는 입력 흐름
- 등기부등본 PDF 업로드 흐름
- 건축물대장 PDF 업로드 흐름
- 등기 수기 findings 입력 및 저장 흐름
- 건축물대장 수기 findings 입력 및 저장 흐름
- 시세 저장 흐름
- 분석 실행 흐름
- 리포트 조회 흐름
- 체크리스트 조회 흐름
- `X-User-Id` 경계를 사용자가 바꿔 볼 수 있는 MVP boundary 처리와 `ACCESS_DENIED` 에러 화면/메시지
- 로딩, 저장 중, 성공, API validation error, forbidden, not-ready 상태를 구분하는 기본 UX
- repo에 아직 frontend가 없다는 점을 고려한 최소 실행 구조 도입

권장 방향:

- 첫 버전은 여러 페이지나 복잡한 제품 구조보다, 하나의 주 진단 흐름과 결과 조회에 집중한 얇은 웹 앱으로 시작한다.
- UI/UX는 한 화면 wizard든 다단계 화면이든 자유롭게 정하되, 사용자가 다음 순서를 잃지 않게 해야 한다.
- MVP의 사용자 경계는 정식 로그인보다 단순한 user id 입력/전환 방식으로 노출하는 쪽을 권장한다. 그래야 access error를 프론트에서 명확히 검증할 수 있다.

### Exclude

- backend domain model, 민감정보 저장 방식, 암호화 정책 변경
- backend API 변경
- 회원가입, 로그인, 세션/토큰 기반 인증 도입
- OCR, PDF 내용 자동 파싱, 자동 요약
- 실거래가/외부 시세 조회 UI 자동화
- 다건 관리용 dashboard, 검색, 정렬, 공유 기능
- 관리자 기능
- 법률 판단형 안내 문구
- 주민등록번호, 인증정보, 결제정보, 세금 체납 정보 입력 UI
- 정교한 디자인 시스템 구축 자체를 목표로 한 작업

## Required Documents

- `01-ui-ux-plan.md`
  - 주 사용자 흐름
  - 화면 구조 또는 단계 구조
  - 각 API 연동 시점
  - 오류/권한/분석 전 상태 처리
  - 문구 가이드
- `02-director-plan-approval.md`
  - committed backend API를 기준으로 한 scope 적합성 검토
- `03-developer-implementation.md`
  - 실제 도입한 frontend 구조
  - API 연동 방식
  - 에러 처리 방식
  - 테스트 및 검증 결과
- 이후 QA 문서들은 승인된 UI/UX 흐름 기준으로 작성한다.

## Acceptance Criteria

- 사용자는 frontend에서 house check를 생성할 수 있다.
- 사용자는 생성된 check에 등기부등본 PDF와 건축물대장 PDF를 각각 업로드할 수 있다.
- 사용자는 등기 수기 findings를 입력하고 저장할 수 있다.
- 사용자는 건축물대장 수기 findings를 입력하고 저장할 수 있다.
- 사용자는 market price를 저장할 수 있다.
- 사용자는 분석을 실행할 수 있다.
- 분석 완료 후 사용자는 report를 조회할 수 있다.
- 사용자는 checklist를 조회할 수 있다.
- 사용자는 분석 전/부분 입력 상태에서도 section status 또는 backend error를 이해할 수 있다.
- 다른 `X-User-Id`로 동일 `checkId`에 접근하면 프론트는 `ACCESS_DENIED`를 사용자에게 명확히 보여주고 기존 데이터 내용을 노출하지 않는다.
- 프론트 문구는 “안전하다”, “계약해도 된다” 같은 단정 표현 없이 위험 신호 안내 톤을 유지한다.
- 프론트는 backend committed API로 동작해야 하며, 필요한 API 변경이 있다면 구현 전에 work log에 변경 사유와 영향 범위를 문서화하고 Director 승인을 다시 받아야 한다.

## Constraints

- deterministic legal/safety wording 금지
- 주민등록번호, 인증정보, 결제정보, 세금 체납 정보 입력/수집 금지
- backend sensitive data model 보존
- landlord name, uploaded PDF, report data를 프론트에서 불필요하게 로그/캐시/스토리지에 복제하지 않는다.
- backend API changes are forbidden unless documented and approved through the harness loop
- committed API contract를 우선 따른다.
- user id boundary는 MVP에서 숨기지 말고 테스트 가능한 형태로 유지한다.
- 파일 업로드는 PDF만 허용하는 backend 제약을 존중한다.
- frontend 세부 기술 선택은 UI/UX와 frontend developer가 결정하되, repo에 처음 들어오는 구조인 만큼 로컬 실행, 테스트, 그리고 backend 연동 경로가 단순해야 한다.

## Open Questions

- frontend entry structure를 어디에 둘지에 대한 구체안은 UI/UX와 frontend developer가 제안한다. 단, repo에 처음 도입되는 구조라는 점을 고려해 가장 단순한 형태를 우선한다.
- create 이후 report/checklist까지 한 화면 안에서 이어갈지, 편집 화면과 결과 화면을 분리할지는 UI/UX가 제안한다.
- market price 입력은 `estimatedMarketValue`, `estimatedJeonseValue` 중 어떤 조합을 기본 노출할지 UI/UX가 명확히 정리한다.
