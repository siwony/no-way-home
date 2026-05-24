# QA Plan: 주택 계약 위험도 진단 프론트엔드

Status: COMPLETE

## Scope

- 대상 work id: `house-risk-agent-prompts-frontend`
- 승인 기준 문서: `01-ui-ux-plan.md`, `03-developer-implementation.md`, `04-ui-ux-acceptance.md`
- 재검증 핵심: Vite dev proxy가 잘못된 `:8080` 가정 없이 실제 `no-way-home` backend를 찾아 `POST /api/house-checks`를 포함한 전체 흐름을 통과시키는지 확인
- 이번 QA는 request interception 또는 mocked API response 없이 실제 Spring Boot backend와 실제 PostgreSQL을 사용한다

## Environment Plan

- PostgreSQL: repository `compose.yaml` 기반 로컬 Docker Compose
- Backend: repository Spring Boot app, `server.port=8081`
- Wrong upstream simulation: `127.0.0.1:8080`에는 `no-way-home`가 아닌 응답을 반환하는 별도 dummy HTTP process를 띄워 proxy detection fallback을 검증
- Frontend dev: `cd frontend && npm run dev -- --host 127.0.0.1 --port 5173`
- Frontend preview fallback check: 필요 시 `npm run preview -- --host 127.0.0.1 --port 4173`로 동일 detection 규칙 추가 확인

## Required Command Gates

- [x] `cd frontend && npm test`
- [x] `cd frontend && npm run build`
- [x] backend regression: `./gradlew test --tests com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest`

## Playwright E2E Checklist

- [x] 기본 route가 즉시 진단 도구 화면으로 열리고 `User ID` 적용 전 `진단 시작`이 비활성화된다
- [x] `User ID` 적용 후 계약 기본 정보 제출이 성공하고, dev frontend `http://127.0.0.1:5173/api/house-checks` 경유 요청이 실제 backend `:8081`로 전달되어 real `checkId`가 생성된다
- [x] `:8080`이 비어 있거나 wrong service여도 frontend dev proxy가 `service=no-way-home` 응답을 주는 `:8081`을 선택한다
- [x] 등기부등본 PDF 업로드가 실제 backend에 저장되고 UI 상태가 `업로드됨`으로 바뀐다
- [x] 건축물대장 PDF 업로드가 실제 backend에 저장되고 UI 상태가 `업로드됨`으로 바뀐다
- [x] 등기 수기 확인 저장이 성공하고 UI 상태가 `저장됨`으로 바뀐다
- [x] 건축물대장 수기 확인 저장이 성공하고 UI 상태가 `저장됨`으로 바뀐다
- [x] 시세 입력 저장이 성공하고 UI 상태가 `완료`로 바뀐다
- [x] 분석 실행 후 `analysisStatus=COMPLETED`가 반영되고 결과 영역이 자동으로 리포트/체크리스트를 불러온다
- [x] 리포트 탭에 risk badge, 핵심 위험 사유, 등기/건축물/보증금/회수 시뮬레이션 정보가 표시된다
- [x] 체크리스트 탭에서 단계별 checklist section이 표시된다
- [x] 동일 `checkId` 상태에서 다른 `User ID`를 적용하면 기존 결과 내용이 제거되고 `ACCESS_DENIED` 전용 패널이 보인다
- [x] `ACCESS_DENIED` 상태에서 `User ID 다시 적용` 또는 `새 진단 시작` 회복 액션이 동작한다

## Validation And Boundary Checks

- [x] PDF가 아닌 파일 선택 시 프론트 validation 또는 업로드 실패 메시지가 노출된다
- [x] 시세 입력에서 `estimatedMarketValue`와 `estimatedJeonseValue`를 모두 비우면 저장이 차단된다
- [x] 결과 조회 후 브라우저 `sessionStorage`에는 `User ID`와 `checkId`만 남고, 리포트/체크리스트/임대인명/파일명 payload는 저장되지 않는다

## Evidence Plan

- 필요 시 frontend evidence를 `doc/agent-work/house-risk-agent-prompts-frontend/assets/`에 저장한다
- QA report에는 실제 실행 명령, 사용 포트, 주요 응답, defect reproduction, residual risk를 기록한다
