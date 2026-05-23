# Codex Subagent Mapping

이 문서는 Codex의 custom subagent 실행 모델에 맞춰 `doc/agent-harness/`를 사용하는 방법을 정의한다.

## Core Model

Codex에서 Director, UI/UX, Developer, QA는 프로젝트 로컬 TOML 파일로 정의한다. Parent Codex가 orchestration을 맡고, subagent를 호출할 때 역할 문서와 작업 파일을 prompt로 전달해 harness role로 사용한다.

Project subagents live in `.codex/agents/`. The project config is `.codex/config.toml`.

## Project TOML Agents

| Agent | File | Sandbox | Purpose |
|---|---|---|---|
| `harness-director` | `.codex/agents/harness-director.toml` | workspace-write | 작업 범위, 승인 gate, loop target, 최종 merge 가능 여부 판단 |
| `harness-ui-ux` | `.codex/agents/harness-ui-ux.toml` | workspace-write | UI/UX 기획, 사용자 흐름, 개발 체크리스트, UI/UX acceptance |
| `kotlin-spring-backend-developer` | `.codex/agents/kotlin-spring-backend-developer.toml` | workspace-write | Kotlin + Spring Boot backend 구현과 테스트 |
| `frontend-developer` | `.codex/agents/frontend-developer.toml` | workspace-write | frontend 구현, 상태/검증/API 연동, Playwright E2E 체크 |
| `harness-qa` | `.codex/agents/harness-qa.toml` | workspace-write | QA 계획, 상세 유즈케이스/예외 테스트, QA report |
| `harness-code-explorer` | `.codex/agents/harness-code-explorer.toml` | read-only | read-only 코드 경로 조사와 영향 범위 분석 |

## Role Mapping

| Harness Role | Codex Execution | Sandbox | Write Scope |
|---|---|---|---|
| Director | Parent Codex or `harness-director` | workspace-write | Director files, `08-pr-lifecycle.md`, `pr-body.md` |
| UI/UX agent | `harness-ui-ux` | workspace-write | `01-ui-ux-plan.md`, `04-ui-ux-acceptance.md` |
| Frontend developer | `frontend-developer` | workspace-write | frontend files and `03-developer-implementation.md` |
| Backend developer | `kotlin-spring-backend-developer` | workspace-write | backend files and `03-developer-implementation.md` |
| QA agent | `harness-qa` | workspace-write | `05-qa-plan.md`, `06-qa-report.md`, test files if assigned |
| Code explorer | `harness-code-explorer` | read-only | no file writes |

Director remains in the parent Codex session so approval gates and loop decisions stay centralized.

## When To Delegate

Delegate to custom subagents only when the user explicitly asks for multi-agent, delegation, parallel agent work, or a harness workflow. Do not delegate ordinary single-agent edits.

Good delegation cases:

- UI/UX plan can run while Director prepares constraints.
- Frontend and backend implementation have disjoint file ownership.
- QA can prepare checklist while developers finish implementation notes.
- Explorer can inspect a bounded codebase question while parent continues non-overlapping work.

Bad delegation cases:

- The next local step is blocked on the subagent result.
- The task is small enough for the parent Codex to complete directly.
- Multiple agents would write the same files.

## Common Delegation Prompt Contract

Every delegated subagent should receive:

- work id and title
- role file path
- current work-log file path
- PR lifecycle file path
- PR body file path
- allowed write paths
- forbidden write paths
- expected status or decision value
- instruction not to revert other agents' work

Use this contract in prompts:

```text
You are acting as the <ROLE> for work id <WORK_ID>.
Read <ROLE_DOC> and <WORK_DIR>/README.md.
Update only <ALLOWED_FILES>.
Do not modify files owned by other agents.
Do not revert changes made by others.
If you find a problem outside your scope, record it under Open Questions or Change Requests.
Return a concise summary and list changed files.
```

## Delegation Prompt Examples

### UI/UX Planning

```text
You are acting as the UI/UX agent for work id house-risk-report.
Read doc/agent-harness/roles/ui-ux.md and doc/agent-work/house-risk-report/00-director-brief.md.
Write only doc/agent-work/house-risk-report/01-ui-ux-plan.md.
Create a user flow, interface states, copy guidance, and a simple developer checklist.
Set Status to READY_FOR_DIRECTOR_REVIEW when complete.
Do not implement code.
```

### Backend Developer

```text
You are acting as the backend developer for work id house-risk-report.
Read doc/agent-harness/roles/developer.md, 01-ui-ux-plan.md, and 02-director-plan-approval.md.
Implement only backend-owned files and update doc/agent-work/house-risk-report/03-developer-implementation.md.
Do not modify frontend files.
Do not start unless Director planning approval is APPROVED.
Run relevant tests and record results.
```

### Frontend Developer

```text
You are acting as the frontend developer for work id house-risk-report.
Read doc/agent-harness/roles/developer.md, 01-ui-ux-plan.md, and 02-director-plan-approval.md.
Implement only frontend-owned files and update doc/agent-work/house-risk-report/03-developer-implementation.md.
Do not modify backend files.
Run relevant tests and record results.
```

### QA Agent

```text
You are acting as the QA agent for work id house-risk-report.
Read doc/agent-harness/roles/qa.md, 01-ui-ux-plan.md, 03-developer-implementation.md, and 04-ui-ux-acceptance.md.
Write doc/agent-work/house-risk-report/05-qa-plan.md and 06-qa-report.md.
Do not fix implementation defects unless explicitly assigned.
Create detailed user, exception, permission, and regression cases.
Record PASS, FAIL, or BLOCKED.
```

## Parent Codex Responsibilities

Parent Codex must:

- create `doc/agent-work/<work-id>/` from `doc/agent-harness/templates/`
- ensure work runs on a valid feature branch before implementation starts
- open or update the Draft PR after Director plan approval
- delegate subagents only for approved parallel work
- keep file ownership disjoint
- keep `08-pr-lifecycle.md` and `pr-body.md` in sync with each loop
- review subagent outputs before advancing gates
- update Director approval and final review files
- mark the PR ready only after Director final review is `READY`
- close subagents when no longer needed

## Work Log Checks

Before handoff, inspect required status fields directly:

```sh
rg -n "^(Status|Decision|Result):" doc/agent-work/<work-id>
```

If the work log status conflicts with the intended next step, Director resolves the mismatch before delegating more work.
