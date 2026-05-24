# PR Lifecycle: 공공 실거래가 시세 자동 조회

Status: READY_FOR_REVIEW

## Branch

- Base branch: `feat/house-document-auto-fill`
- Work branch: `feat/house-market-price-public-api`
- Branch rule: `feat/{feature-name}` or `feat/{feature-name}/{implementation-name}`

## Draft PR

- Title: `feat: 공공 실거래가 시세 자동 조회`
- Body file: `doc/agent-work/house-market-price-public-api/pr-body.md`
- PR URL: https://github.com/siwony/no-way-home/pull/3
- Created at: 2026-05-24

## Required Before Development

- [x] Work branch is not `main`
- [x] Planning documents are committed
- [x] `pr-body.md` is filled from the repository PR template
- [x] Draft PR is opened

## Loop Updates

| Date | Gate | Summary | PR Body Updated | Pushed |
|---|---|---|---|---|
| 2026-05-24 | DRAFT_PR_OPEN | Draft PR #3 opened against `feat/house-document-auto-fill` | yes | yes |
| 2026-05-24 | DEVELOPMENT_COMPLETE | XML-only public market price lookup, preview/apply UI, tests, and screenshot evidence completed | yes | yes |
| 2026-05-24 | QA_PASS | Full Gradle test, frontend tests/build, Playwright screenshot, and diff check passed | yes | yes |
| 2026-05-24 | SERVICE_KEY_QA | Registered service keys tested against real public XML APIs; partial 403 fallback added and Gradle tests passed | yes | yes |
| 2026-05-24 | SERVICE_KEY_REQA | After all three public API approvals, Juso, StanReginCd, AptTrade, and AptRent returned XML 200; lookup/save passed with market and jeonse values | yes | yes |

## Visual Evidence Assets

- Asset directory: `doc/agent-work/house-market-price-public-api/assets/`
- Use this section for frontend screenshots, short screen recordings, or browser evidence that should appear in the PR body.

| File | Scenario | PR Body Link Updated |
|---|---|---|
| `assets/market-price-lookup-preview.png` | 공공 실거래가 조회 preview, 적용, 저장 완료 상태 | yes |

## Ready For Review

- [x] Director final review decision is `READY`
- [x] QA report result is `PASS`
- [x] Final test results are reflected in `pr-body.md`
- [x] `(WIP)` removed from PR title
- [x] `gh pr ready` completed

## Notes

Status value: `READY_FOR_REVIEW`.

Stacked PR base is the current document auto-fill feature branch because this work builds on the existing frontend/backend application flow in PR #2.
