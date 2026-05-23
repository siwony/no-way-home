# PR Lifecycle: 주택 계약 위험도 진단 서비스

Status: BLOCKED

## Branch

- Base branch: `main`
- Work branch: `main`
- Required branch rule: `feat/house-risk-agent-prompts` or another `feat/{feature-name}` branch

## Draft PR

- Title: `(WIP) feat: 주택 계약 위험도 진단 서비스`
- Body file: `doc/agent-work/house-risk-agent-prompts/pr-body.md`
- PR URL:
- Created at:

## Required Before Development

- [ ] Work branch is not `main`
- [x] Planning documents are committed
- [x] `pr-body.md` is filled from the repository PR template
- [ ] Draft PR is opened

## Current Blocker

This work completed the Director/Developer/UI/UX/QA loop before PR lifecycle enforcement was added. It is currently on `main`, so `scripts/create-draft-pr.sh` will not open a Draft PR.

To unblock PR creation, move this work to a valid feature branch, push it, then run:

```sh
scripts/create-draft-pr.sh --feature "주택 계약 위험도 진단 서비스" --body doc/agent-work/house-risk-agent-prompts/pr-body.md
```

## Loop Updates

| Date | Gate | Summary | PR Body Updated | Pushed |
|---|---|---|---|---|
| 2026-05-24 | PR_READY | PR lifecycle added retroactively after missing harness-to-PR connection was identified. | yes | no |

## Ready For Review

- [x] Director final review decision is `READY`
- [x] QA report result is `PASS`
- [x] Final test results are reflected in `pr-body.md`
- [ ] `(WIP)` removed from PR title
- [ ] `gh pr ready` completed

## Notes

Use one status value: `NOT_CREATED`, `DRAFT_OPENED`, `UPDATED`, `READY_FOR_REVIEW`, `SKIPPED_BY_USER`, or `BLOCKED`.
