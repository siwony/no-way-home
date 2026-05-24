## 개요

- Work ID: `house-risk-agent-prompts-frontend`
- 기능명: 주택 계약 위험도 진단 프론트엔드
- 관련 문서:
  - `doc/agent-work/house-risk-agent-prompts-frontend/00-director-brief.md`
  - `doc/agent-work/house-risk-agent-prompts-frontend/01-ui-ux-plan.md`
  - `doc/agent-work/house-risk-agent-prompts-frontend/08-pr-lifecycle.md`

Backend Phase 1 API를 실제로 사용할 수 있는 첫 프론트엔드 슬라이스를 추가한다.

## 현재 상태

- 2026-05-24 live integration 결함 루프 진행 중
- 사용자 재현: 계약 기본 정보 입력 후 `http://localhost:5173/api/house-checks` 요청이 404를 반환하고 다음 행동이 없음
- frontend-developer rework 완료: Vite dev/preview에서 실제 `no-way-home` backend를 `/api/status`로 감지하도록 보강
- 다음 기준: UI/UX acceptance 후 실제 Spring Boot backend를 포함한 Playwright E2E가 통과해야 다시 ready-for-review로 복귀

## 작업 내용

- [x] Director brief 작성
- [x] UI/UX plan 작성
- [x] Director approval 완료
- [x] Draft PR 생성
- [x] Frontend 실행 구조 도입
- [x] House check 생성/업로드/수기 입력/분석/리포트/체크리스트 흐름 구현
- [x] UI/UX acceptance 완료
- [x] QA plan/report 완료
- [ ] Director final review 재완료

## 리뷰 필요

- 최초 frontend 구조가 Kotlin/Spring backend와 분리되어 단순하게 유지되는지 확인
- committed backend API contract를 변경하지 않고 소비하는지 확인
- `X-User-Id` boundary와 `ACCESS_DENIED` 처리가 사용자에게 명확한지 확인
- Draft PR: https://github.com/siwony/no-way-home/pull/1
- Frontend implementation status: `READY_FOR_UI_UX_ACCEPTANCE`
- Current verification:
  - `cd frontend && npm test` passed, 1 file / 4 tests
  - `cd frontend && npm run build` passed
- UI/UX acceptance status: `APPROVED`
- Rework completed: added direct `User ID 다시 적용` and `새 진단 시작` actions inside the access-denied panel.
- Rework verification:
  - `cd frontend && npm test` passed, 1 file / 4 tests
  - `cd frontend && npm run build` passed
- QA status: `PASS`
- QA-01 rework completed:
  - restored `User ID` and `checkId` from `sessionStorage` now produce a matching initial global banner message.
  - `cd frontend && npm test` passed, 1 file / 5 tests
  - `cd frontend && npm run build` passed
- UI/UX re-acceptance after QA-01: `APPROVED`
- QA rerun:
  - `cd frontend && npm test` passed, 1 file / 5 tests
  - `cd frontend && npm run build` passed
  - built-frontend smoke passed for restored-session banner and access-denied recovery
- Director final review: `CHANGES_REQUESTED` for live integration rerun
- PR lifecycle status: `UPDATED` until backend-inclusive E2E passes
- Current loop: live backend-inclusive E2E rework required for `/api/house-checks` create flow 404
- Integration rework verification:
  - `cd frontend && npm test` passed, 2 files / 10 tests
  - `cd frontend && npm run build` passed

## 스크린샷

### 진단 워크스페이스

![진단 워크스페이스](https://github.com/siwony/no-way-home/blob/feat/house-risk-agent-prompts/frontend/doc/agent-work/house-risk-agent-prompts-frontend/assets/workspace.png?raw=1)

### 리포트 결과

![리포트 결과](https://github.com/siwony/no-way-home/blob/feat/house-risk-agent-prompts/frontend/doc/agent-work/house-risk-agent-prompts-frontend/assets/report.png?raw=1)

### 접근 거부 복구 상태

![접근 거부 복구 상태](https://github.com/siwony/no-way-home/blob/feat/house-risk-agent-prompts/frontend/doc/agent-work/house-risk-agent-prompts-frontend/assets/access-denied.png?raw=1)
