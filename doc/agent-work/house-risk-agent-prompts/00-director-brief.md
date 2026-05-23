# Director Brief: 주택 계약 위험도 진단 서비스

Status: READY_FOR_UI_UX

## Goal

- MVP Phase 1 범위에서 주택 계약 위험 신호 진단 기능의 사용자 흐름과 구현 기준을 고정한다.
- 사용자는 진단 요청을 만들고, 등기부등본/건축물대장 PDF를 업로드하고, 시세 정보를 수동 저장한 뒤, 위험도 리포트와 계약 체크리스트를 조회할 수 있어야 한다.
- 결과는 법률 판단이나 안전 보장이 아니라 현재 확인된 자료 기준의 위험 신호와 추가 확인 필요 사항으로 표현되어야 한다.

## Background

- 기능 문서는 장기적으로 OCR, 자동 추출, 외부 시세 조회까지 포함하는 로드맵을 설명하지만, 이번 work id는 자동화 이전의 안정적인 Phase 1 경계를 먼저 만드는 작업이다.
- Phase 1에서는 업로드 문서를 서버가 자동 해석하지 않는다. 업로드는 저장, 접근통제, 메타데이터 관리, 이후 수동 입력 또는 후속 자동화 연결을 위한 경계 정의가 목적이다.
- 시세 정보도 외부 연동을 강제하지 않는다. 수동 입력 저장만으로 분석이 동작해야 하며, 외부 조회는 이후 단계에서 붙일 수 있도록 provider port만 열어 둔다.
- 따라서 UI/UX plan은 “무엇이 자동 분석 결과이고 무엇이 사용자 입력/미확인 상태인지”를 혼동 없이 드러내는 흐름을 우선 설계해야 한다.

## Scope

### Include

- 진단 요청 생성 흐름과 API 범위: 주소, 계약 유형, 보증금, 월세, 계약 예정일, 입주 예정일, 임대인 이름, 주택 유형 등 필수 입력을 기준으로 `POST /api/house-checks`를 설계한다.
- 등기부등본 및 건축물대장 업로드 경계: `POST /api/house-checks/{checkId}/registry-file`, `POST /api/house-checks/{checkId}/building-ledger-file`는 PDF 파일 저장과 메타데이터 보존을 담당한다.
- 업로드 메타데이터/저장 경계: 파일 존재 여부, 원본 파일명, MIME type, 크기, 업로드 시각, 문서 기준일 또는 발급일(입력 가능한 경우), 내부 저장 참조값, 접근 권한 경계를 다룬다.
- 수동 진단 기반 UX: OCR 없이도 분석이 가능하도록, UI/UX agent는 등기/건축물 확인 결과를 사용자가 직접 정리해 입력하는 단계 또는 화면 구성을 제안해야 한다.
- 시세 정보 수동 저장: `POST /api/house-checks/{checkId}/market-price`는 사용자가 직접 입력한 예상 매매가/전세가와 출처, 기준일 등 메타데이터를 저장해야 한다.
- 시세 provider port 경계: 외부 시세 조회는 Phase 1 필수 기능이 아니지만, `MarketPriceProvider` 같은 포트/어댑터 구조로 이후 연동이 가능해야 한다.
- 위험 분석 실행: `POST /api/house-checks/{checkId}/analyze`는 요청 정보, 사용자가 입력한 구조화 위험 정보, 업로드/미업로드 상태, 수동 저장된 시세 정보를 바탕으로 위험도와 사유를 산출한다.
- 결과 조회 API: `GET /api/house-checks/{checkId}/report`, `GET /api/house-checks/{checkId}/checklist`는 null 또는 계산 불가 상태를 숨기지 않고 그대로 표현해야 한다.
- 누락 데이터 처리: 등기부등본, 건축물대장, 시세 정보, 수동 확인 결과가 없으면 분석 불가 또는 추가 확인 필요 사유를 리포트에 포함한다.
- 에러 계약: `HOUSE_CHECK_NOT_FOUND`, `INVALID_CONTRACT_TYPE`, `INVALID_DEPOSIT_AMOUNT`, `REGISTRY_FILE_REQUIRED`, `MARKET_PRICE_NOT_AVAILABLE`, `ANALYSIS_NOT_READY`, `ACCESS_DENIED` 등의 에러 처리를 Phase 1 범위에서 정리한다.

### Exclude

- 등기부등본/건축물대장 OCR, PDF 파싱, 권리 항목 자동 추출
- 외부 시세 조회를 필수 성공 조건으로 두는 구현
- 인터넷등기소 로그인, 인증, 결제 대행 또는 자동 발급
- 임대인 국세/지방세 체납 조회
- 보증보험 가입 가능 여부 확정 판정
- 법률 자문, 계약 진행 권고, “안전하다” 같은 확정 표현
- 주민등록번호, 인증서 비밀번호, 간편인증 토큰, 결제정보 저장
- 공시지가나 공개 매물 호가를 실거래가와 동일하게 사용하는 계산
- 공유 링크, 알림, 계약 직전 자동 재조회 같은 후속 운영 기능

## Required Documents

- `doc/feat/house-risk-agent-prompts/goal.md`
- `doc/feat/house-risk-agent-prompts/scope.md`
- `doc/feat/house-risk-agent-prompts/expected-behavior.md`
- `doc/feat/house-risk-agent-prompts/implementation-notes.md`
- `doc/feat/house-risk-agent-prompts/tests.md`
- `doc/feat/house-risk-agent-prompts/constraints.md`
- `doc/init/tech-stack.md`
- `doc/agent-work/house-risk-agent-prompts/01-ui-ux-plan.md`: Phase 1 사용자 흐름, 수동 입력 단계, 업로드 UX, 보고서/체크리스트 상태, 간단 체크리스트를 포함해야 한다.

## Acceptance Criteria

- 사용자는 필수 계약 정보로 진단 요청을 생성할 수 있다.
- 사용자는 등기부등본 PDF와 건축물대장 PDF를 업로드할 수 있고, 시스템은 원본 문서를 자동 판독하지 않더라도 저장 참조와 메타데이터를 안전하게 보존한다.
- Phase 1 UX는 업로드 문서 자체와 수동으로 정리한 확인 결과를 구분해서 보여 주며, 자동 판독이 아님을 명확히 한다.
- 사용자는 시세 정보를 수동으로 저장할 수 있고, 출처와 기준일을 함께 남길 수 있다.
- 외부 연동이 없어도 분석 실행이 가능해야 하며, 부족한 자료는 위험 사유 또는 계산 불가 사유로 노출된다.
- 리포트는 종합 위험도, 한 줄 요약, 핵심 위험 사유, 등기 관련 상태, 건축물 관련 상태, 보증금 위험 분석, 회수 시뮬레이션(가능한 경우), 추가 확인 필요 항목을 포함한다.
- 체크리스트 API는 계약 전/직전/후 확인 항목을 반환한다.
- 모든 사용자 문구는 단정형 표현을 피하고, 현재 확인된 자료 기준의 위험 신호 안내 형식을 유지한다.
- 접근 권한이 없는 사용자는 요청, 리포트, 체크리스트, 업로드 문서에 접근할 수 없다.

## Constraints

- 기술 스택과 실행 환경은 `doc/init/tech-stack.md`를 따른다.
- Phase 1 범위는 수동 입력 기반 진단을 우선하며, 자동 추출/자동 조회를 전제하지 않는다.
- 서비스 런타임과 테스트는 외부 skill 설치 여부나 네트워크 접근에 의존하지 않아야 한다.
- 임대인 이름과 업로드 문서는 민감정보로 간주하고 암호화 저장 및 로그 마스킹 경계를 설계해야 한다.
- 주민등록번호, 인증정보, 인증서 비밀번호, 결제정보는 저장하지 않는다.
- 시세 정보가 없으면 전세가율/총 위험 노출 비율/회수 시뮬레이션은 `null` 또는 계산 불가 상태로 남겨야 하며, 이유를 설명해야 한다.
- `gongsijiga-search` 결과는 시세 대체값으로 사용하지 않는다.
- 공개 매물/호가 정보는 참고 자료로만 취급하며, 위험도 계산의 필수 입력으로 섞지 않는다.

## Open Questions

- Phase 1의 수동 구조화 입력을 `진단 요청 생성` 단계에 포함할지, 업로드 이후 별도 `문서 확인 입력` 단계로 둘지 UI/UX plan에서 제안이 필요하다.
- 업로드 메타데이터 중 사용자가 직접 입력해야 하는 최소 항목을 어디까지 둘지 정리가 필요하다. 기본 후보는 발급일, 문서 기준일, 비고다.
