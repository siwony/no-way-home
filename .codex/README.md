# Codex Project Configuration

이 디렉터리는 이 저장소에서만 사용하는 Codex 설정과 custom subagent 정의를 보관한다. 사람과 AI 모두 이 파일들을 수정할 수 있지만, 변경 범위와 검증 결과를 명확히 남겨야 한다.

## Structure

- `config.toml`: Codex subagent 실행 설정
- `agents/*.toml`: 프로젝트 전용 custom subagent 정의

현재 subagent 역할과 하네스 연결 방식은 [doc/agent-harness/codex-subagents.md](../doc/agent-harness/codex-subagents.md)를 기준으로 한다.

## Agent Files

각 `agents/*.toml` 파일은 다음 필드를 유지한다.

- `name`: Codex에서 호출할 agent 이름
- `description`: 이 agent를 사용할 상황
- `model`: 사용할 모델
- `model_reasoning_effort`: reasoning 강도
- `sandbox_mode`: 파일 접근 범위
- `developer_instructions`: agent 역할, 참조 문서, 금지 사항, 반환 형식

Agent 이름은 파일명과 같은 의미를 가져야 한다. 예: `harness-qa.toml`의 `name`은 `harness-qa`.

## Agent Summary

| Agent | Summary |
|---|---|
| `harness-director` | 작업 목표와 범위를 정리하고, UI/UX 기획 승인, 재작업 루프 결정, 최종 merge 가능 여부를 판단한다. 직접 구현하지 않는다. |
| `harness-ui-ux` | Director brief를 사용자 흐름, 화면 상태, 문구, 간단한 개발 체크리스트로 구체화하고 구현 후 UI/UX acceptance를 수행한다. |
| `kotlin-spring-backend-developer` | Kotlin + Spring Boot backend API, 도메인 로직, 검증, 권한, DB 연동, 테스트를 구현한다. 서비스 내부 임시 util 남발을 피하고 역할별 클래스로 분리한다. |
| `frontend-developer` | 승인된 UI/UX plan에 따라 frontend 화면, 상태, 입력 검증, API 연동, 사용자 피드백, 필요한 Playwright E2E 체크를 구현한다. |
| `harness-qa` | UI/UX plan과 구현 결과를 기준으로 사용자 유즈케이스, 예외, 권한, 회귀 위험 테스트를 설계하고 QA report를 작성한다. |
| `harness-code-explorer` | read-only로 코드 경로, 영향 범위, ownership boundary, 테스트 표면을 조사해 다음 agent가 실행할 수 있는 근거를 제공한다. |

## Maintenance Rules

- role 책임은 TOML에, workflow와 handoff 규칙은 `doc/agent-harness/`에 둔다.
- 같은 규칙을 여러 TOML에 복사하지 말고 공통 규칙은 문서 링크로 참조한다.
- `workspace-write` agent는 허용된 역할 파일과 담당 코드 영역만 수정하도록 지시한다.
- `read-only` agent는 분석과 경로 조사만 수행하도록 둔다.
- 새 agent를 추가하면 [doc/agent-harness/codex-subagents.md](../doc/agent-harness/codex-subagents.md)의 agent 표도 함께 갱신한다.

## Validation

TOML 문법을 수정한 뒤 다음 명령으로 확인한다.

```sh
python3 - <<'PY'
import pathlib, tomllib
for path in sorted(pathlib.Path('.codex').glob('**/*.toml')):
    tomllib.loads(path.read_text())
    print(f'OK {path}')
PY
```

하네스 관련 문서 참조는 다음 명령으로 점검한다.

```sh
rg -n "\\.codex/agents|doc/agent-harness|doc/agent-work" AGENTS.md .codex doc skills
```
