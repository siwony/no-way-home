# PR Lifecycle: 등기부등본·임대차 계약서 자동 입력

Status: DRAFT_OPENED

## Branch

- Base branch: `feat/house-risk-agent-prompts/frontend` until PR #1 merges; rebase onto `main` after PR #1 merges.
- Work branch: `feat/house-document-auto-fill`
- Branch rule: `feat/{feature-name}` or `feat/{feature-name}/{implementation-name}`

## Draft PR

- Title: `(WIP) feat: 등기부등본·임대차 계약서 자동 입력`
- Body file: `doc/agent-work/house-document-auto-fill/pr-body.md`
- PR URL: https://github.com/siwony/no-way-home/pull/2
- Created at: 2026-05-24

## Required Before Development

- [x] Work branch is not `main`
- [x] Planning documents are committed
- [x] `pr-body.md` is filled from the repository PR template
- [x] Draft PR is opened

## Loop Updates

| Date | Gate | Summary | PR Body Updated | Pushed |
|---|---|---|---|---|
| 2026-05-24 | DIRECTOR_BRIEF | New production-grade document auto-fill harness work started on `feat/house-document-auto-fill`. | no | no |
| 2026-05-24 | DIRECTOR_PLAN_APPROVAL | Director approved UI/UX plan and production document intake scope. Planning docs are included in the initial harness planning commit before Draft PR creation. | yes | yes |
| 2026-05-24 | DRAFT_PR_OPEN | Draft PR opened against stacked base `feat/house-risk-agent-prompts/frontend`: https://github.com/siwony/no-way-home/pull/2. | yes | pending |
| 2026-05-24 | DEVELOPMENT | Backend slice completed: `/api/document-intakes` session/upload/review/application-payload APIs, encrypted storage, fake extraction adapter, migration, and integration tests. Frontend implementation remains. | yes | pending |

## Visual Evidence Assets

- Asset directory: `doc/agent-work/house-document-auto-fill/assets/`
- Use this section for frontend screenshots, short screen recordings, or browser evidence that should appear in the PR body.

| File | Scenario | PR Body Link Updated |
|---|---|---|
|  |  |  |

## Ready For Review

- [ ] Director final review decision is `READY`
- [ ] QA report result is `PASS`
- [ ] Final test results are reflected in `pr-body.md`
- [ ] `(WIP)` removed from PR title
- [ ] `gh pr ready` completed

## Notes

Use one status value: `NOT_CREATED`, `DRAFT_OPENED`, `UPDATED`, `READY_FOR_REVIEW`, `SKIPPED_BY_USER`, or `BLOCKED`.
