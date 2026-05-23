# UI/UX Acceptance: 주택 계약 위험도 진단 프론트엔드

Status: APPROVED

Decision: APPROVED

## Review Summary

The frontend still matches the approved single-route operational workspace. The `User ID` boundary remains explicit, the major create/upload/manual-findings/market/analyze/result actions remain present, card-level loading and validation states remain separated, `ANALYSIS_NOT_READY` is still distinct from generic errors, and browser persistence is still limited to `sessionStorage` for `User ID` and `checkId`.

The prior `ACCESS_DENIED` change request is resolved. The denial panel now includes direct `User ID 다시 적용` and `새 진단 시작` recovery actions, shows the current `User ID` and `checkId`, and keeps the server-derived result/status content cleared while the user recovers.

The QA-01 rework is also acceptable from a UI/UX perspective. Restored-session messaging now uses a neutral session-aware banner instead of reverting to the first-entry prompt when `User ID` and `checkId` are already present, and that change does not weaken the approved access-denied recovery flow.

## Commands Run

```text
cd frontend && npm test
- success
- vitest: 1 file passed, 5 tests passed

cd frontend && npm run build
- success
- tsc no-emit checks passed, vite production build passed
```

## Checklist Review

- [x] 기본 route가 곧바로 진단 도구 화면으로 열리고 마케팅 hero 페이지가 없다.
- [x] `User ID` 적용 전에는 주요 API 호출 버튼이 비활성화되고, 적용 후 요청 헤더 경계가 일관되게 구현되어 있다.
- [x] 계약 생성, 등기/건축물대장 PDF 업로드, 등기/건축물대장 수기 확인 저장, 시세 저장, 분석 실행, 리포트/체크리스트 조회 흐름이 한 화면 안에 구성되어 있다.
- [x] 정상, 빈 상태, 로딩, validation, `ANALYSIS_NOT_READY`, 일반 오류 상태가 구분되어 있다.
- [x] 위험 안내 문구는 도구형 톤을 유지하고, 금지된 단정 표현을 프론트 copy에서 사용하지 않는다.
- [x] `ACCESS_DENIED` 발생 시 서버 기반 결과/상태/파일명 노출은 제거되고 영구 저장소에도 서버 민감 데이터가 남지 않는다.
- [x] 접근 거부 패널 자체에 `User ID 다시 적용`과 `새 진단 시작` 회복 액션이 포함되어 있다.

## Findings

- 없음. QA-01 rework에서 추가된 restored-session neutral message helper와 그 테스트는 승인된 도구형 copy 방향을 유지하며, `ACCESS_DENIED` 패널의 회복 액션과 기존 서버 데이터 제거 흐름도 계속 보존된다.

## Changed Files

- `doc/agent-work/house-risk-agent-prompts-frontend/04-ui-ux-acceptance.md`

## Decision Notes

Use one decision value: `APPROVED`, `CHANGES_REQUESTED`, or `BLOCKED`.
