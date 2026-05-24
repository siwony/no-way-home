# Director Final Review: 주택 계약 위험도 진단 프론트엔드

Status: NEEDS_REVIEW

Decision: CHANGES_REQUESTED

## Final Review Summary

The prior `READY` decision is superseded by a post-ready live integration defect reported by the user on 2026-05-24. The work must loop back to development and QA before it can return to `PR_READY`.

Previously recorded prerequisites were satisfied, but the final QA evidence did not cover the full live backend path. The user reproduced a create-flow failure where the frontend request to `http://localhost:5173/api/house-checks` returned 404 after entering contract basic info.

Bounded source review of the shipped frontend also matches the approved slice closely enough for Director sign-off. The dedicated `frontend/` workspace is present, all documented Phase 1 user actions exist in the single-route workspace, all API requests flow through an `X-User-Id` header-aware client, market price allows either amount, analysis/result states distinguish `ANALYSIS_NOT_READY`, and `ACCESS_DENIED` removes prior server-derived content while keeping direct recovery actions in the denial panel.

The following prior residual risks are now blocking:

- The final QA rerun focused on the restored-session and access-denied regression path, not a full live backend create -> upload -> findings -> market -> analyze -> result pass.
- Local runtime smoke previously required moving the backend off `:8080` because another process was already bound there on this machine.
- Pre-analysis draft resume and generic rehydration after reload remain intentionally limited to session-scoped `User ID` and `checkId`, which is within the approved slice.

## Requirement Match

- [x] Director brief satisfied
- [x] UI/UX plan satisfied
- [x] Developer implementation complete
- [ ] QA result acceptable after live backend E2E rerun
- [x] Director plan approval is `APPROVED`
- [x] UI/UX acceptance is `APPROVED`
- [ ] QA report result is `PASS` after live backend E2E rerun
- [x] Draft PR is opened and `08-pr-lifecycle.md` is updated
- [ ] Implementation is complete enough for the approved frontend scope after fixing live API integration

## Rework Target

Use only when decision is `CHANGES_REQUESTED`.

- Target agent: `frontend-developer`
- Reason: Contract basic information create flow can hit `/api/house-checks` on the Vite frontend origin and return 404 instead of reaching the Spring backend.
- Required changes: Reproduce the live frontend/backend route, fix the frontend integration/configuration or user-facing recovery needed for local development, then hand off to QA for backend-inclusive Playwright E2E.

## Final Decision

Use one decision value: `READY`, `CHANGES_REQUESTED`, or `BLOCKED`.
