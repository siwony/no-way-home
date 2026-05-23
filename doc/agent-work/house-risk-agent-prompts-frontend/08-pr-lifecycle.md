# PR Lifecycle: 주택 계약 위험도 진단 프론트엔드

Status: BLOCKED

## Branch

- Base branch: `main`
- Work branch: `main`
- Required branch rule: `feat/house-risk-agent-prompts/frontend` or another `feat/{feature-name}/{implementation-name}` branch

## Draft PR

- Title: `(WIP) feat: 주택 계약 위험도 진단 프론트엔드`
- Body file: `doc/agent-work/house-risk-agent-prompts-frontend/pr-body.md`
- PR URL:
- Created at:

## Required Before Development

- [ ] Work branch is not `main`
- [x] Planning documents are committed
- [x] `pr-body.md` is filled from the repository PR template
- [ ] Draft PR is opened

## Current Blocker

This harness work is already in `DEVELOPMENT`, but it is running on `main`. `scripts/create-draft-pr.sh` intentionally refuses to open a Draft PR from `main`.

To unblock PR creation, move this work to a valid feature branch, push it, then run:

```sh
scripts/create-draft-pr.sh --feature "주택 계약 위험도 진단 프론트엔드" --body doc/agent-work/house-risk-agent-prompts-frontend/pr-body.md
```

## Loop Updates

| Date | Gate | Summary | PR Body Updated | Pushed |
|---|---|---|---|---|
| 2026-05-24 | DEVELOPMENT | PR lifecycle added after the missing harness-to-PR connection was identified. | yes | no |

## Ready For Review

- [ ] Director final review decision is `READY`
- [ ] QA report result is `PASS`
- [ ] Final test results are reflected in `pr-body.md`
- [ ] `(WIP)` removed from PR title
- [ ] `gh pr ready` completed

## Notes

Use one status value: `NOT_CREATED`, `DRAFT_OPENED`, `UPDATED`, `READY_FOR_REVIEW`, `SKIPPED_BY_USER`, or `BLOCKED`.
