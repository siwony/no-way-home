# PR Lifecycle: {{TITLE}}

Status: NOT_CREATED

## Branch

- Base branch: `main`
- Work branch:
- Branch rule: `feat/{feature-name}` or `feat/{feature-name}/{implementation-name}`

## Draft PR

- Title: `(WIP) feat: {{TITLE}}`
- Body file: `doc/agent-work/{{WORK_ID}}/pr-body.md`
- PR URL:
- Created at:

## Required Before Development

- [ ] Work branch is not `main`
- [ ] Planning documents are committed
- [ ] `pr-body.md` is filled from the repository PR template
- [ ] Draft PR is opened

## Loop Updates

| Date | Gate | Summary | PR Body Updated | Pushed |
|---|---|---|---|---|
|  |  |  |  |  |

## Visual Evidence Assets

- Asset directory: `doc/agent-work/{{WORK_ID}}/assets/`
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
