# PR Lifecycle: 등기부등본·임대차 계약서 자동 입력

Status: READY_FOR_REVIEW

## Branch

- Base branch: `feat/house-risk-agent-prompts/frontend` until PR #1 merges; rebase onto `main` after PR #1 merges.
- Work branch: `feat/house-document-auto-fill`
- Branch rule: `feat/{feature-name}` or `feat/{feature-name}/{implementation-name}`

## Draft PR

- Title: `feat: 등기부등본·임대차 계약서 자동 입력`
- Body file: `doc/agent-work/house-document-auto-fill/pr-body.md`
- PR URL: https://github.com/siwony/no-way-home/pull/2
- Created at: 2026-05-24
- Current remote state: PR #2 is ready for review on GitHub and remains stacked on `feat/house-risk-agent-prompts/frontend`.

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
| 2026-05-24 | DEVELOPMENT | Frontend document auto-fill flow completed inside the existing workspace. `cd frontend && npm test`, `cd frontend && npm run build`, and focused backend tests passed. | yes | pending |
| 2026-05-24 | UI_UX_ACCEPTANCE | UI/UX acceptance returned `CHANGES_REQUESTED`; frontend loop targets failed-document retry wording and explicit overwrite choice. | yes | pending |
| 2026-05-24 | DEVELOPMENT | UI/UX requested rework completed: failed-document action wording now matches upload behavior and overwrite conflicts show explicit current-value-vs-approved-value choice. Frontend tests/build passed. | yes | pending |
| 2026-05-24 | UI_UX_ACCEPTANCE | UI/UX re-acceptance approved the document auto-fill frontend rework; QA may proceed with mock fixture automation plus local-only real PDF upload validation. | yes | pending |
| 2026-05-24 | QA_REPORT | QA completed mock browser E2E successfully and captured screenshots, but failed local-only real PDF validation because both provided PDFs returned `HTTP 413` before persistence. | yes | pending |
| 2026-05-24 | DEVELOPMENT | Reopened backend/frontend development loop for document-intake upload size policy, Spring multipart limits, and clear oversized-file feedback. | yes | pending |
| 2026-05-24 | DEVELOPMENT | Implemented 20MB backend upload policy, multipart limits, JSON `413`, frontend preflight validation, and upload error copy. | yes | pending |
| 2026-05-24 | QA_REPORT | QA re-run passed with real local PDFs, encrypted storage evidence, frontend tests/build, focused backend tests, and `./gradlew test`. | yes | pending |
| 2026-05-24 | DIRECTOR_FINAL_REVIEW | Director final review for the original slice returned `READY`, but the loop was reopened before PR readiness was finalized. | yes | pending |
| 2026-05-24 | PR_READY | Reopened the harness loop because the default extraction path still depended on `FakeDocumentIntakeExtractionAdapter`; new in-scope requirement was real PDF parsing plus AI review with no silent fake fallback. | yes | no |
| 2026-05-24 | DIRECTOR_PLAN_APPROVAL | Director approved resumed development without a new UI/UX planning loop. Next implementation targets were real parser integration, AI extraction/review adapter wiring, explicit provider-unavailable failure, and fake-adapter opt-in only behavior. | yes | no |
| 2026-05-24 | DEVELOPMENT | Reopened implementation completed: PDFBox PDF parsing/page counting, OpenAI Responses `input_file` handoff, strict AI result validation, default provider switch to `openai`, fake opt-in only, and explicit `AI_PROVIDER_UNAVAILABLE` failure without fallback. | yes | no |
| 2026-05-24 | QA_REPORT | Reopened QA passed with focused/full backend tests, frontend tests/build, actual-PDF no-key runtime smoke, and actual-PDF mock OpenAI runtime smoke confirming `input_file` plus `data:application/pdf;base64,` request evidence. | yes | no |
| 2026-05-24 | DIRECTOR_FINAL_REVIEW | Director final review returned `READY` for the reopened scope. PR communication must preserve the stacked-base caveat and the residual risk that live OpenAI service calls were not verified locally. | yes | no |
| 2026-05-24 | PR_READY | PR body updated, `(WIP)` removed from the title, and PR marked ready for review. | yes | no |

## Visual Evidence Assets

- Asset directory: `doc/agent-work/house-document-auto-fill/assets/`
- Use this section for frontend screenshots, short screen recordings, or browser evidence that should appear in the PR body.

| File | Scenario | PR Body Link Updated |
|---|---|---|
| `qa-document-review-mock.png` | Mock fixture extraction review, warnings, field evidence, and review actions. | yes |
| `qa-apply-preview-mock.png` | Mock fixture compare/apply preview with explicit overwrite choice state. | yes |

## Ready For Review

- [x] Director final review decision is `READY`
- [x] QA report result is `PASS`
- [x] Final test results are reflected in `pr-body.md`
- [x] `(WIP)` removed from PR title
- [x] `gh pr ready` completed

## Notes

Use one status value: `NOT_CREATED`, `DRAFT_OPENED`, `UPDATED`, `READY_FOR_REVIEW`, `SKIPPED_BY_USER`, or `BLOCKED`.

- Current harness state is `READY_FOR_REVIEW`.
- Merge communication must continue to call out the stacked base branch dependency on `feat/house-risk-agent-prompts/frontend`.
- Residual implementation risk: live OpenAI service integration was not exercised locally because `OPENAI_API_KEY` was unavailable.
