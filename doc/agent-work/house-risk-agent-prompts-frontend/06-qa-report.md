# QA Report: 주택 계약 위험도 진단 프론트엔드

Status: COMPLETE

Result: PASS

## Summary

QA rerun focused on the prior QA-01 regression and the minimum frontend gates requested for the rework loop.

Required frontend gates passed: `cd frontend && npm test`, `cd frontend && npm run build`.

Source review confirmed the restored-session banner fix is implemented in `frontend/src/App.tsx` by seeding `globalMessage` from `deriveNeutralGlobalMessage(...)`, and `frontend/src/validation.test.ts` now covers the restored `User ID + checkId` case directly.

Built-frontend Playwright smoke also passed. With mocked `report`/`checklist` responses, the app showed the neutral restored-session banner, loaded an existing result, cleared prior server-derived content on boundary reset, showed the `ACCESS_DENIED` panel for a cross-user reload, and kept both recovery actions working. No blocking or new regression was found in access-denied recovery.

## Tests Executed

```text
cd frontend && npm test
- pass
- vitest 1 file passed, 5 tests passed

cd frontend && npm run build
- pass
- tsc no-emit checks passed
- vite production build passed

Source review
- inspected frontend/src/App.tsx, frontend/src/validation.ts, frontend/src/validation.test.ts
- confirmed App initial state uses deriveNeutralGlobalMessage(readSessionValue(...), readCheckId())
- confirmed unit test covers "", user-only, and user+checkId banner states

Built frontend browser smoke
- preview server: cd frontend && npm run preview -- --host 127.0.0.1 --port 4174
- Playwright smoke with request interception for /api/house-checks/check-1/report and /checklist
- restored session banner: pass
- owner-a result reload and report rendering: pass
- owner-b boundary reset and prior result removal: pass
- owner-b ACCESS_DENIED panel with recovery actions: pass
- denial-panel re-apply recovery hides denied state again: pass
- start-fresh recovery clears checkId: pass
```

## Passed Cases

- 저장된 `User ID`와 `checkId`가 있을 때 상단 배너가 더 이상 첫 진입 문구를 보이지 않고 neutral restored-session 문구를 표시한다.
- QA-01 수정은 source와 unit test 양쪽에서 확인된다. `deriveNeutralGlobalMessage(...)` helper와 그 3가지 상태 검증이 함께 존재한다.
- 기존 결과가 보이던 상태에서 다른 `User ID`를 적용하면 boundary reset 경고가 보이고 기존 서버 유래 결과가 즉시 사라진다.
- 같은 `checkId`를 다른 `User ID`로 다시 불러오면 `ACCESS_DENIED` 전용 패널이 나타나고, 현재 `User ID`/`checkId`와 `User ID 다시 적용`, `새 진단 시작` 회복 액션이 유지된다.
- denial panel 안에서 `User ID 다시 적용`을 실행하면 denied 상태가 해제되고, `새 진단 시작`을 실행하면 `checkId`가 제거된다.

## Defects

- 없음.

## Residual Risks

- 이번 rerun은 QA-01과 access-denied recovery 회귀에 집중했다. create/upload/manual findings/market/analyze 전체 live backend 경로는 이번 루프에서 다시 끝까지 재실행하지 않았다.
- 브라우저 smoke는 built frontend + request interception 기준이다. 실제 backend와의 통합 경로는 이전 QA/개발 기록의 범위를 재사용한다.

## Result Notes

- Suggested loop target: none
- Evidence focus: QA-01 fixed and covered, access-denied recovery preserved, basic frontend gates green

Use one result value: `PASS`, `FAIL`, or `BLOCKED`.
