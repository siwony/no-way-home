# Multi-Agent Workflow Gates

## State Machine

```text
DIRECTOR_BRIEF
  -> UI_UX_PLAN
  -> DIRECTOR_PLAN_APPROVAL
  -> DEVELOPMENT
  -> UI_UX_ACCEPTANCE
  -> QA_PLAN
  -> QA_REPORT
  -> DIRECTOR_FINAL_REVIEW
```

## Gate Rules

| Gate | Required Decision | Next Step | Failure Loop |
|---|---|---|---|
| Director plan approval | `APPROVED` | Development | UI/UX plan revision |
| UI/UX acceptance | `APPROVED` | QA plan | Developer implementation |
| QA report | `PASS` | Director final review | Director decides target |
| Director final review | `READY` | Merge-ready | Director chooses UI/UX, Developer, or QA loop |

## Loop Rules

- If the plan is wrong, loop to UI/UX agent.
- If implementation does not match the approved plan, loop to Developer agents.
- If tests are incomplete or blocked, loop to QA agent.
- If a defect reveals a planning gap, Director may loop back to UI/UX agent instead of Developer agents.

## Merge-Ready Definition

A work item is merge-ready only when:

- Director plan approval decision is `APPROVED`.
- UI/UX acceptance decision is `APPROVED`.
- QA report result is `PASS`.
- Director final review decision is `READY`.
- Required commits, PR body, and local work logs are up to date.
