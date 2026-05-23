---
name: multi-agent-harness
description: Use this skill when coordinating Director, UI/UX, Developer, and QA agents through the repository's file-based multi-agent harness.
---

# Multi-Agent Harness

Use this skill when the user asks to run, prepare, inspect, or continue multi-agent work involving Director, UI/UX, Developer, and QA agents.

## References

Do not duplicate the harness rules in this skill. Load the repository references as needed:

- `doc/agent-harness/README.md`: overview, roles, handoff rules
- `doc/agent-harness/workflow.md`: state machine, gates, loop rules, merge-ready definition
- `doc/agent-harness/codex-subagents.md`: Codex subagent mapping, delegation prompt contracts, role ownership
- `doc/agent-harness/roles/director.md`: Director responsibilities
- `doc/agent-harness/roles/ui-ux.md`: UI/UX responsibilities
- `doc/agent-harness/roles/developer.md`: frontend/backend developer responsibilities
- `doc/agent-harness/roles/qa.md`: QA responsibilities
- `doc/agent-harness/templates/`: work-log templates
- `doc/agent-work/`: per-feature work logs created from templates
- `.codex/agents/*.toml`: project-specific Codex custom subagent definitions
- `skills/github-draft-pr/SKILL.md`: Draft PR creation and ready-for-review rules
- `scripts/create-draft-pr.sh`: Draft PR helper

## Workflow

1. Read `doc/agent-harness/README.md`, `doc/agent-harness/workflow.md`, and `doc/agent-harness/codex-subagents.md`.
2. If acting as a specific role, read only that role file.
3. When starting a harness task, create `doc/agent-work/<work-id>/` from `doc/agent-harness/templates/` and replace `{{WORK_ID}}` and `{{TITLE}}`.
4. Before implementation starts, make sure the work is on a valid `feat/...` branch and read `skills/github-draft-pr/SKILL.md`.
5. After Director plan approval is `APPROVED`, commit planning docs and `pr-body.md`, then open a Draft PR with `scripts/create-draft-pr.sh`.
6. Delegate role work to the matching project TOML subagent in `.codex/agents/` when the user asks for multi-agent or delegated execution.
7. Before handoff, inspect `Status`, `Decision`, and `Result` fields in `doc/agent-work/<work-id>/`.
8. After each loop, update `08-pr-lifecycle.md` and `pr-body.md`, then push and update the PR body.
9. Update only the file for the current agent role unless the user explicitly asks for harness maintenance.

## Role File Mapping

- Director brief: `00-director-brief.md`
- UI/UX plan: `01-ui-ux-plan.md`
- Director planning approval: `02-director-plan-approval.md`
- Developer implementation: `03-developer-implementation.md`
- UI/UX acceptance: `04-ui-ux-acceptance.md`
- QA plan: `05-qa-plan.md`
- QA report: `06-qa-report.md`
- Director final review: `07-director-final-review.md`
- PR lifecycle: `08-pr-lifecycle.md`
- PR body: `pr-body.md`

## Guardrails

- Delegate to Codex subagents only when the user explicitly asks for multi-agent, delegation, or harness-based work.
- Prefer the project TOML subagents in `.codex/agents/` when delegating harness roles.
- Do not start implementation before Director planning approval is `APPROVED`.
- Do not start implementation before the Draft PR is opened or explicitly marked `SKIPPED_BY_USER` in `08-pr-lifecycle.md`.
- Do not start QA before UI/UX acceptance is `APPROVED`.
- Do not mark work merge-ready unless Director final review is `READY` and PR lifecycle status is `READY_FOR_REVIEW`.
- If a prior stage is wrong, record change requests instead of silently editing another role's output.
