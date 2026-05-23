# QA Report: 주택 계약 위험도 진단 프론트엔드

Status: COMPLETE

Result: FAIL

## Summary

Required frontend gates passed: `cd frontend && npm test`, `cd frontend && npm run build`.

Source review confirmed the approved single-route flow and the direct access-denied recovery actions are implemented in `frontend/src/App.tsx`, including the `User ID` boundary handling, create/upload/manual findings/market/analyze/result sections, and the `ACCESS_DENIED` panel actions at `frontend/src/App.tsx:1028-1052`.

Live verification against the source backend on `:8081` passed for create, both uploads, both manual findings saves, `estimatedJeonseValue`-only market save, analyze, report, checklist, `ACCESS_DENIED`, and `ANALYSIS_NOT_READY`. Partial browser smoke against the source frontend on `:5174` passed for `User ID` gating, create-form validation, not-ready messaging, result rendering, storage limits, and denial recovery. One concrete defect remains in the persisted-session resume state, so this QA handoff is `FAIL`.

## Tests Executed

```text
cd frontend && npm test
- pass
- vitest 1 file passed, 4 tests passed

cd frontend && npm run build
- pass
- tsc no-emit checks passed
- vite production build passed

Source review
- inspected frontend/src/App.tsx, frontend/src/api.ts, frontend/src/validation.ts, frontend/src/validation.test.ts
- confirmed create/upload/manual findings/market/analyze/result structure and access-denied recovery actions

Live source-backend API smoke
- target: http://127.0.0.1:8081
- create 200
- registry upload 200
- building-ledger upload 200
- registry findings save 200
- building-ledger findings save 200
- market save 200 with estimatedJeonseValue only
- analyze 200 -> COMPLETED
- report 200
- checklist 200
- cross-user report access 403 ACCESS_DENIED
- pre-analysis report access 409 ANALYSIS_NOT_READY

Partial browser smoke
- source backend: SERVER_PORT=8081 ./gradlew bootRun
- source frontend: VITE_BACKEND_ORIGIN=http://127.0.0.1:8081 npm run dev -- --host 127.0.0.1 --port 5174
- fresh app shell: User ID gate + create-form validation passed
- seeded not-ready check: ANALYSIS_NOT_READY messaging passed
- seeded ready check: report/checklist rendering + storage limits passed
- seeded cross-user flow: boundary reset -> ACCESS_DENIED panel -> direct recovery -> result reload passed
```

## Passed Cases

- `User ID` 적용 전 `진단 시작`이 비활성화되고, 적용 후 생성 폼 검증 오류가 사용자에게 직접 보인다.
- UI에는 승인된 섹션이 모두 존재한다: 계약 생성, 등기/건축물대장 PDF 업로드, 등기/건축물대장 수기 확인, 시세 입력, 분석, 리포트, 체크리스트.
- `ACCESS_DENIED` 전용 패널에 `User ID 다시 적용`과 `새 진단 시작` 회복 액션이 구현되어 있고, 다른 `User ID`로 접근 시 기존 서버 유래 결과 내용이 제거된다.
- `ANALYSIS_NOT_READY`가 일반 오류와 분리된 별도 상태로 표시된다.
- 브라우저 영구 저장소는 `sessionStorage`의 `house-risk-agent-prompts.user-id`, `house-risk-agent-prompts.check-id`만 사용하며, 임대인명/파일명/리포트 payload는 남지 않는다.
- 라이브 API 기준으로 `estimatedJeonseValue` 단독 시세 입력 경로가 분석/리포트까지 정상 동작한다.

## Failed Cases

- 세션에 `User ID`와 `checkId`가 이미 남아 있는 상태로 앱을 다시 열면, 상단 배너가 현재 상태와 모순되게 `먼저 User ID를 적용하면 진단을 시작할 수 있습니다.`를 계속 표시한다.

## Defects

| ID | Severity | Scenario | Expected | Actual | Repro Steps |
|---|---|---|---|---|---|
| QA-01 | Medium | 세션 보존 후 재진입 안내 문구 | `sessionStorage`에 보존된 `User ID`와 `checkId`가 복원되면, 상단 안내 문구도 현재 세션 상태와 맞게 `현재 User ID로 이 checkId를 다시 확인할 수 있습니다.` 또는 동등한 상태 메시지를 보여야 한다. | `activeUserId`와 `checkId`는 복원되지만, 배너는 초기값인 `먼저 User ID를 적용하면 진단을 시작할 수 있습니다.`를 그대로 보여 준다. 사용자에게 세션이 복원되지 않은 것처럼 보인다. | 1. 브라우저 `sessionStorage`에 `house-risk-agent-prompts.user-id`, `house-risk-agent-prompts.check-id`를 넣는다. 2. 앱을 새로 연다. 3. 헤더의 현재 `User ID`/`checkId`는 채워져 있는 반면, 배너는 `먼저 User ID를 적용하면 진단을 시작할 수 있습니다.`를 표시하는지 확인한다. |

## Residual Risks

- 브라우저 smoke는 결과/권한/회복 상태 중심으로 검증했고, 업로드/수기 확인/시세 입력/분석 버튼을 끝까지 브라우저에서 각각 다시 누르는 풀 UI 경로는 별도 자동화하지 않았다. 해당 경로는 라이브 API smoke와 소스 리뷰로 보강했다.
- 로컬 Docker 앱(`:8080`)은 `/api/status`는 응답했지만 현재 브랜치의 `/api/house-checks` endpoint를 제공하지 않아 QA 런타임 대상으로 사용하지 않았다. 실제 검증은 working tree에서 기동한 소스 백엔드(`:8081`)와 프론트엔드(`:5174`) 기준이다.

## Result Notes

- Impact: 세션 보존 convenience를 허용한 승인안과 달리, 재진입 시 첫 상태 메시지가 잘못되어 사용자가 저장된 `checkId` 재확인 흐름을 오해할 수 있다.
- Suggested loop target: Developer
- Evidence: `frontend/src/App.tsx:103-113`에서 세션 값은 초기화하지만 `globalMessage`는 고정 초기값을 사용한다. 상태를 올바르게 맞추는 로직은 `onApplyUserId` 경로에만 있다 (`frontend/src/App.tsx:268-281`).

Use one result value: `PASS`, `FAIL`, or `BLOCKED`.
