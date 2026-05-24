# PR Lifecycle: 공공 실거래가 시세 자동 조회

Status: DRAFT_OPENED

## Branch

- Base branch: `feat/house-document-auto-fill`
- Work branch: `feat/house-market-price-public-api`
- Branch rule: `feat/{feature-name}` or `feat/{feature-name}/{implementation-name}`

## Draft PR

- Title: `(WIP) feat: 공공 실거래가 시세 자동 조회`
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

## Visual Evidence Assets

- Asset directory: `doc/agent-work/house-market-price-public-api/assets/`
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

Stacked PR base is the current document auto-fill feature branch because this work builds on the existing frontend/backend application flow in PR #2.
