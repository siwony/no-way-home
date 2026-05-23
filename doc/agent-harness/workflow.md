# Multi-Agent Workflow Gates

## State Machine

```text
DIRECTOR_BRIEF
  -> UI_UX_PLAN
  -> DIRECTOR_PLAN_APPROVAL
  -> DRAFT_PR_OPEN
  -> DEVELOPMENT
  -> UI_UX_ACCEPTANCE
  -> QA_PLAN
  -> QA_REPORT
  -> DIRECTOR_FINAL_REVIEW
  -> PR_READY
```

## Gate Rules

| Gate | Required Decision | Next Step | Failure Loop |
|---|---|---|---|
| Director plan approval | `APPROVED` | Draft PR | UI/UX plan revision |
| Draft PR | `OPENED` | Development | Director PR setup |
| UI/UX acceptance | `APPROVED` | QA plan | Developer implementation |
| QA report | `PASS` | Director final review | Director decides target |
| Director final review | `READY` | PR ready | Director chooses UI/UX, Developer, or QA loop |
| PR ready | `READY_FOR_REVIEW` | Merge-ready | Director PR update |

## PR Lifecycle

The harness must keep the local work log and GitHub PR in sync.

1. Work starts on a feature branch following `skills/git-branch-rules/SKILL.md`, usually `feat/{feature-name}` or `feat/{feature-name}/{implementation-name}`.
2. After Director plan approval is `APPROVED`, Director prepares `doc/agent-work/<work-id>/pr-body.md` from the PR template and records PR state in `08-pr-lifecycle.md`.
3. Director commits the planning docs and PR body before implementation starts.
4. Director opens a Draft PR with title `(WIP) feat: <기능 이름>` using `scripts/create-draft-pr.sh --feature "<기능 이름>" --body doc/agent-work/<work-id>/pr-body.md`.
5. After each meaningful implementation or QA loop, the owning agent updates its work log and Director updates `pr-body.md` before pushing.
6. When Director final review is `READY`, Director updates final test results in `pr-body.md`, removes `(WIP)` from the PR title, and runs `gh pr ready`.

## Loop Rules

- If the plan is wrong, loop to UI/UX agent.
- If implementation does not match the approved plan, loop to Developer agents.
- If tests are incomplete or blocked, loop to QA agent.
- If a defect reveals a planning gap, Director may loop back to UI/UX agent instead of Developer agents.
- On every loop, update `08-pr-lifecycle.md` and the PR body with the current status, failed gate, and next loop target.

## Merge-Ready Definition

A work item is merge-ready only when:

- Director plan approval decision is `APPROVED`.
- UI/UX acceptance decision is `APPROVED`.
- QA report result is `PASS`.
- Director final review decision is `READY`.
- `08-pr-lifecycle.md` status is `READY_FOR_REVIEW`.
- The GitHub PR is no longer Draft and its body matches the latest local work logs.
- Required commits, PR body, and local work logs are up to date.
