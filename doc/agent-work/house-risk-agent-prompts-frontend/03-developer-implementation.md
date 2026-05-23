# Developer Implementation: 주택 계약 위험도 진단 프론트엔드

Status: READY_FOR_UI_UX_ACCEPTANCE

## Prerequisites

- Director planning approval is `APPROVED`: [x] yes / [ ] no

## Backend Work

- [x] 없음. committed backend API contract는 변경하지 않았다.

## Frontend Work

- [x] 분리된 `frontend/` Vite + React + TypeScript workspace를 추가했다.
- [x] 기본 route가 곧바로 진단 도구 화면으로 열리는 단일 작업형 UI를 구현했다.
- [x] `User ID` 적용 바를 추가하고 모든 API 요청에 `X-User-Id`를 포함하도록 공통 API client를 구현했다.
- [x] `POST /api/house-checks` 생성 폼과 클라이언트 검증을 구현했다.
- [x] 등기부등본 PDF, 건축물대장 PDF 업로드 카드와 optional `issuedDate` 입력을 구현했다.
- [x] 등기 수기 확인, 건축물대장 수기 확인을 별도 폼/저장 액션으로 분리했다.
- [x] 시세 입력 카드에서 `estimatedMarketValue` / `estimatedJeonseValue`를 함께 노출하고, 둘 중 하나 이상 필수 검증을 구현했다.
- [x] 분석 실행, 리포트 탭, 체크리스트 탭, `ANALYSIS_NOT_READY` 상태 분기를 구현했다.
- [x] `ACCESS_DENIED` 발생 시 기존 서버 데이터 영역을 비우고 접근 거부 패널로 전환하는 흐름을 구현했다.
- [x] `ACCESS_DENIED` 패널 내부에 `User ID 다시 적용`과 `새 진단 시작` 회복 액션을 추가해 denial panel만으로 다음 동작을 바로 실행할 수 있게 했다.
- [x] 브라우저 영구 저장소에는 `User ID`와 `checkId`만 `sessionStorage`에 저장하고, 리포트/체크리스트/임대인명/파일명 payload는 저장하지 않도록 제한했다.
- [x] 로컬 기본 API 연결은 `VITE_API_BASE_URL=/api` + Vite proxy 기본 target `http://localhost:8080`로 구성하고, 필요 시 `VITE_BACKEND_ORIGIN`으로 오버라이드할 수 있게 했다.
- [x] `frontend/.gitignore`를 추가해 `node_modules`, `dist`, `.vite`, `*.tsbuildinfo`를 제외했다.

## UI/UX Checklist Result

- [x] 마케팅 hero 없이 기본 route가 곧바로 진단 도구 화면으로 열린다.
- [x] `User ID` 적용 전에는 주요 API 호출 버튼이 비활성화된다.
- [x] 생성 성공 시 `checkId`와 진행 상태 rail이 즉시 갱신된다.
- [x] 등기/건축물대장 업로드 UI가 분리되어 있고 PDF 외 파일을 프론트에서 방어한다.
- [x] 등기 수기 확인과 건축물대장 수기 확인이 별도 저장 동작으로 분리되어 있다.
- [x] 시세 입력은 `estimatedMarketValue` 또는 `estimatedJeonseValue` 중 하나 이상을 허용한다.
- [x] 분석 버튼은 `checkId` 생성 이후 활성화되고, 누락 데이터가 있어도 실행을 막지 않는다.
- [x] 리포트/체크리스트를 같은 화면 안의 탭으로 전환해 볼 수 있다.
- [x] `ANALYSIS_NOT_READY`와 `ACCESS_DENIED`가 일반 오류와 분리된 상태로 보인다.
- [x] 서버 데이터는 브라우저 영구 저장소에 남기지 않는다.

## Tests Run

```text
npm install
- success
- added 98 packages, 0 vulnerabilities

npm test
- success
- vitest 1 file passed, 4 tests passed

npm run build
- success
- type-check without emit + vite production build completed

Playwright CLI browser smoke against Vite dev server
- validation path confirmed: empty create submit showed inline required-field errors
- normal path confirmed: create request through frontend returned POST /api/house-checks => 200 and UI showed generated checkId + updated progress state
- boundary failure confirmed: after switching the same checkId from `owner-a` to `owner-b`, frontend requests returned GET report => 403 and GET checklist => 403, and the UI switched to the access-denied panel while prior server-derived result content was removed

Smoke environment note
- default frontend config targets backend at `http://localhost:8080`
- local machine already had another process on `:8080` serving a different app, so runtime smoke used `SERVER_PORT=8081 ./gradlew bootRun` plus `VITE_BACKEND_ORIGIN=http://127.0.0.1:8081 npm run dev`
- committed default config itself was not changed
```

## Rework Loop: UI_UX_ACCEPTANCE -> DEVELOPMENT

- 요청된 수정만 반영했다. `ACCESS_DENIED` 결과 패널 안에 `User ID 다시 적용`, `새 진단 시작` 버튼을 직접 추가했고, 패널 안에서 현재 재적용 대상 `User ID`도 함께 보이도록 했다.
- 기존 동작은 유지했다. `ACCESS_DENIED` 발생 시 서버 유래 데이터(`sectionStatus`, `report`, `checklist`, 업로드 파일명)는 계속 즉시 제거되고, 회복 액션은 기존 `onApplyUserId` / `onStartFresh` 경로를 재사용한다.

### Rework Verification

```text
cd frontend && npm test
- success
- vitest: 1 file passed, 4 tests passed

cd frontend && npm run build
- success
- tsc no-emit checks passed, vite production build passed
```

### Rework Changed Files

- `frontend/src/App.tsx`
- `frontend/src/styles.css`
- `doc/agent-work/house-risk-agent-prompts-frontend/03-developer-implementation.md`

## Rework Loop: QA_REPORT -> DEVELOPMENT

- QA-01만 수정했다. 세션 복원 시 초기 글로벌 배너가 고정 첫 진입 문구를 쓰지 않도록, 복원된 `User ID`와 `checkId` 기준의 neutral message helper를 추가하고 `App.tsx` 초기 상태와 재적용 경로에서 재사용했다.
- 복원 상태 회귀를 막기 위해 helper 단위 테스트를 추가했다. 빈 세션, `User ID`만 있는 세션, `User ID + checkId`가 모두 복원된 세션의 문구를 함께 검증한다.

### QA Rework Verification

```text
cd frontend && npm test
- success
- vitest: 1 file passed, 5 tests passed

cd frontend && npm run build
- success
- tsc no-emit checks passed, vite production build passed
```

### QA Rework Changed Files

- `frontend/src/App.tsx`
- `frontend/src/validation.ts`
- `frontend/src/validation.test.ts`
- `doc/agent-work/house-risk-agent-prompts-frontend/03-developer-implementation.md`

## Functional Smoke Checklist

- [x] 기본 route가 진단 도구 화면으로 열리고 User ID 적용 전 주요 액션이 비활성화된다.
- [x] User ID 적용 후 house check 생성이 성공하고 checkId와 진행 상태가 갱신된다.
- [ ] 등기 PDF와 건축물대장 PDF 업로드를 브라우저에서 끝까지 재검증하지는 않았다.
- [ ] 등기 수기 확인과 건축물대장 수기 확인 저장을 브라우저에서 끝까지 재검증하지는 않았다.
- [ ] 시세 입력과 분석/리포트/체크리스트를 브라우저에서 끝까지 재검증하지는 않았다.
- [x] 필수값 누락 검증이 사용자에게 직접 보인다.
- [x] 다른 User ID를 같은 checkId에 적용하면 기존 서버 데이터가 화면에서 제거되고 접근 거부 상태를 확인할 수 있다.

## Changed Files

- `frontend/.env.example`
- `frontend/.gitignore`
- `frontend/index.html`
- `frontend/package-lock.json`
- `frontend/package.json`
- `frontend/public/favicon.svg`
- `frontend/src/App.tsx`
- `frontend/src/api.ts`
- `frontend/src/format.ts`
- `frontend/src/main.tsx`
- `frontend/src/styles.css`
- `frontend/src/types.ts`
- `frontend/src/validation.test.ts`
- `frontend/src/validation.ts`
- `frontend/tsconfig.app.json`
- `frontend/tsconfig.json`
- `frontend/tsconfig.node.json`
- `frontend/vite.config.ts`
- `doc/agent-work/house-risk-agent-prompts-frontend/03-developer-implementation.md`

## Risks Or Follow-ups

- The browser smoke did not run the entire upload -> findings -> market price -> analyze -> result path end to end. UI/UX acceptance and QA should cover that full integrated path.
- Runtime smoke required `:8081` because this machine already had a different process on `:8080`. If a reviewer sees raw 404s through the frontend proxy, 먼저 `:8080`에서 어떤 프로세스가 응답 중인지 확인해야 한다.
- The committed backend still lacks a general pre-analysis readback endpoint, so refresh/re-entry after mid-flow edits is intentionally limited to session-scoped `User ID` and `checkId` continuity.
- `sessionStorage` keeps only `User ID` and `checkId`, but same-browser user switching intentionally clears visible server-derived data to reduce boundary leakage. QA should still verify this on multiple browsers.

## Handoff Notes

- Frontend slice is ready for UI/UX acceptance.
- Generated artifacts were cleaned before handoff: `frontend/node_modules`, `frontend/dist`, `.playwright-cli`, `output`, emitted `vite.config.js` / `vite.config.d.ts`, and `*.tsbuildinfo` were removed.
- For local reruns:
  - `cd frontend && npm install`
  - `npm run dev`
  - backend default expectation is `http://localhost:8080`, or override proxy target with `VITE_BACKEND_ORIGIN=http://127.0.0.1:8081`.
