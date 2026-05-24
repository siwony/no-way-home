# Multi-Agent Work Harness

이 하네스는 여러 server agent가 같은 기능을 순서대로 넘겨받아 작업할 수 있도록 하는 파일 기반 프로토콜입니다.

## Roles

- Director agent: 전체 작업 지시, 승인, 재작업 루프 결정만 수행한다. 구현하지 않는다.
- UI/UX agent: 기능 기획, 사용자 흐름, 화면/경험 기준, 간단한 기능 체크리스트를 작성한다.
- Developer agents: frontend developer와 backend developer로 나뉘며 실제 구현을 담당한다.
- QA agent: 사용자 유즈케이스, 예외, 회귀 위험을 기준으로 상세 테스트 체크리스트와 테스트 결과를 작성한다.

## Workflow

See [workflow.md](workflow.md) for gate rules and loop handling. See [codex-subagents.md](codex-subagents.md) for mapping these roles to Codex custom subagents in `.codex/agents/*.toml`.

1. Director가 작업 지시서를 작성한다.
2. UI/UX agent가 기획서와 간단 체크리스트를 작성한다.
3. Director가 기획이 원래 기능 목표와 맞는지 승인한다.
4. Director가 planning docs와 PR body를 커밋하고 Draft PR을 연다.
5. Developer agents가 승인된 기획에 따라 구현한다.
6. Developer agents가 UI/UX 체크리스트를 통과했는지 기록한다.
7. QA agent가 기획 기준으로 상세 체크리스트를 만들고 테스트한다.
8. Director가 최종 구현 상태를 확인한다.
9. Director가 PR body를 최신화하고 PR을 review-ready 상태로 만든다.
10. 불일치가 있으면 Director가 되돌릴 단계와 이유를 명시하고 루프를 반복한다.

## Work Directory

각 작업은 `doc/agent-work/{work-id}/` 아래에 기록한다.

새 작업을 시작할 때:

- `doc/agent-work/{work-id}/` 디렉터리를 만든다.
- `doc/agent-harness/templates/*.md` 파일을 복사한다.
- 파일 안의 `{{WORK_ID}}`와 `{{TITLE}}` placeholder를 실제 값으로 바꾼다.
- 각 단계의 `Status`, `Decision`, `Result` 값을 handoff 전에 직접 확인한다.

필수 work log 파일은 `state.md`, `00-director-brief.md`, `01-ui-ux-plan.md`, `02-director-plan-approval.md`, `03-developer-implementation.md`, `04-ui-ux-acceptance.md`, `05-qa-plan.md`, `06-qa-report.md`, `07-director-final-review.md`, `08-pr-lifecycle.md`, `pr-body.md`이다.

## PR Rules

- 하네스 작업은 `main`이 아니라 feature branch에서 시작한다.
- Draft PR은 Director plan approval 이후, Developer 구현 전에 연다.
- PR 제목은 `(WIP) feat: <기능 이름>` 형식을 사용한다.
- PR 본문은 `doc/agent-work/{work-id}/pr-body.md`를 기준으로 갱신한다.
- 루프가 발생하면 실패 gate, 재작업 대상, 최신 테스트 결과를 PR body에 반영한다.
- frontend 화면 증거가 필요하면 이미지를 `doc/agent-work/{work-id}/assets/`에 저장하고 PR body에서 커밋된 이미지 URL을 참조한다.
- Director final review가 `READY`이고 QA result가 `PASS`일 때만 `(WIP)`를 제거하고 `gh pr ready`를 실행한다.

## Handoff Rules

- 각 agent는 자신의 산출물 파일만 작성한다.
- 이전 agent 산출물이 틀렸다면 직접 고치지 말고 `Open Questions` 또는 `Change Requests`에 남긴다.
- 다음 단계로 넘기려면 해당 단계의 `Status`를 명시한다.
- 구현은 Director의 planning approval 이후에만 시작한다.
- QA는 UI/UX acceptance 이후에만 시작한다.
- 최종 merge 가능 상태는 Director final review가 `READY`일 때만 인정한다.
