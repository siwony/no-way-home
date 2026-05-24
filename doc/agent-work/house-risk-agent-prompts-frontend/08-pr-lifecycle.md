# PR Lifecycle: 주택 계약 위험도 진단 프론트엔드

Status: READY_FOR_REVIEW

## Branch

- Base branch: `main`
- Work branch: `feat/house-risk-agent-prompts/frontend`
- Required branch rule: `feat/house-risk-agent-prompts/frontend` or another `feat/{feature-name}/{implementation-name}` branch

## Draft PR

- Title: `feat: 주택 계약 위험도 진단 프론트엔드`
- Body file: `doc/agent-work/house-risk-agent-prompts-frontend/pr-body.md`
- PR URL: https://github.com/siwony/no-way-home/pull/1
- Created at: 2026-05-24

## Required Before Development

- [x] Work branch is not `main`
- [x] Planning documents are committed
- [x] `pr-body.md` is filled from the repository PR template
- [x] Draft PR is opened

## Current Blocker

None. Backend-inclusive E2E QA passed and Director final review is `READY`.

## Loop Updates

| Date | Gate | Summary | PR Body Updated | Pushed |
|---|---|---|---|---|
| 2026-05-24 | DEVELOPMENT | PR lifecycle added after the missing harness-to-PR connection was identified. | yes | no |
| 2026-05-24 | DRAFT_PR_OPEN | Work moved from `main` to `feat/house-risk-agent-prompts/frontend`; initial PR creation failed because the branch had no diff from `main`, so this lifecycle update will create the branch diff for retry. | yes | no |
| 2026-05-24 | DRAFT_PR_OPEN | Draft PR opened: https://github.com/siwony/no-way-home/pull/1. | yes | yes |
| 2026-05-24 | DEVELOPMENT | Frontend implementation completed under `frontend/`; validation tests and production build passed. | yes | yes |
| 2026-05-24 | UI_UX_ACCEPTANCE | UI/UX acceptance returned `CHANGES_REQUESTED`; developer loop targets the access-denied recovery panel actions. | yes | yes |
| 2026-05-24 | DEVELOPMENT | Access-denied panel recovery actions were added; frontend test and build passed again. | yes | yes |
| 2026-05-24 | UI_UX_ACCEPTANCE | UI/UX acceptance approved the rework; QA may proceed. | yes | yes |
| 2026-05-24 | QA_REPORT | QA returned `FAIL` for the session resume banner mismatch; developer loop will correct the restored-session global message. | yes | yes |
| 2026-05-24 | DEVELOPMENT | QA-01 fixed by deriving restored-session banner text from `User ID` and `checkId`; frontend tests and build passed. | yes | yes |
| 2026-05-24 | UI_UX_ACCEPTANCE | UI/UX acceptance approved QA-01 rework; QA may rerun. | yes | yes |
| 2026-05-24 | QA_REPORT | QA rerun passed; frontend tests/build and built-frontend smoke are green. | yes | yes |
| 2026-05-24 | DIRECTOR_FINAL_REVIEW | Director final review is `READY`; PR body updated with final test results and PR marked ready for review. | yes | yes |
| 2026-05-24 | PR_READY | Frontend evidence screenshots captured, committed under `assets/`, and linked from PR body. | yes | yes |
| 2026-05-24 | PR_READY | Live integration defect reported from contract basic info create flow: `/api/house-checks` returned 404 through `localhost:5173`. Reopening developer loop and requiring backend-inclusive E2E QA before returning to ready-for-review. | yes | yes |
| 2026-05-24 | DEVELOPMENT | Frontend rework added Vite backend auto-detection for local `no-way-home` backend candidates and shared `/api` proxy coverage for dev/preview. `cd frontend && npm test` and `cd frontend && npm run build` passed. | yes | yes |
| 2026-05-24 | UI_UX_ACCEPTANCE | UI/UX acceptance approved the integration rework; QA must now run live Spring Boot backend plus frontend E2E for the create -> upload -> findings -> market -> analyze -> report/checklist path. | yes | yes |
| 2026-05-24 | QA_REPORT | Backend-inclusive QA passed: wrong service on `:8080`, real Spring Boot on `:8081`, Vite dev full browser E2E, Vite preview create proxy check, frontend tests/build, and controller integration test. | yes | yes |
| 2026-05-24 | DIRECTOR_FINAL_REVIEW | Director final review is `READY` again after backend-inclusive E2E PASS; PR body and evidence links are up to date. | yes | yes |

## Visual Evidence Assets

- Asset directory: `doc/agent-work/house-risk-agent-prompts-frontend/assets/`
- Use committed screenshots for frontend PR review because `gh` cannot upload image files into the PR body.

| File | Scenario | PR Body Link Updated |
|---|---|---|
| `workspace.png` | Main diagnostic workspace with `User ID` session context and operational form flow. | yes |
| `report.png` | Report tab with risk level, reasons, registry/building/deposit sections, and conservative simulation. | yes |
| `access-denied.png` | `ACCESS_DENIED` panel with direct `User ID 다시 적용` and `새 진단 시작` recovery actions. | yes |
| `qa-access-denied.png` | Live backend E2E access-denied boundary state after cross-user reload. | yes |

## Ready For Review

- [x] Director final review decision is `READY` after the live integration rerun
- [x] QA report result is `PASS` after backend-inclusive E2E
- [x] Final test results are reflected in `pr-body.md`
- [x] `(WIP)` removed from PR title
- [x] `gh pr ready` completed

## Notes

Use one status value: `NOT_CREATED`, `DRAFT_OPENED`, `UPDATED`, `READY_FOR_REVIEW`, `SKIPPED_BY_USER`, or `BLOCKED`.
