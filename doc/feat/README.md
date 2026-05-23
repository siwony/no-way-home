# Feature Documents

이 폴더는 agent가 `feat` 작업을 구현할 때 참고할 Markdown 문서를 보관합니다.

새 기능 요청은 `feature-template.md`를 복사해 작성하고, 파일명은 구현 대상을 알 수 있게 `kebab-case`로 정합니다. 내용이 큰 기능은 `kebab-case/` 폴더를 만들고 템플릿 섹션을 여러 Markdown 파일로 나눕니다.

작업 전 체크리스트가 필요하면 `work-checklist-template.md`를 복사해 사용합니다. Draft PR을 먼저 올릴 작업은 체크리스트 또는 개발 문서를 커밋한 뒤 PR 본문에 해당 문서 경로를 연결합니다.

예시:

```text
doc/feat/user-registration.md
doc/feat/order-cancel-policy.md
doc/feat/house-risk-agent-prompts/README.md
```
