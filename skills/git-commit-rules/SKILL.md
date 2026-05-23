---
name: git-commit-rules
description: Use this skill when writing, reviewing, or rewriting Git commit messages for this repository.
---

# Git Commit Rules

Use one of these commit types:

- `feat`: 기능 추가 또는 기존 기능 확장
- `fix`: 버그 수정
- `chore`: 빌드, 설정, 문서, 정리 등 기타 작업

## Format

Write commit messages in this format:

```text
type: short summary

- detailed work item
- detailed work item
```

Examples:

```text
feat: 회원 기능 개발

- spring security 에 대한 jwt filter 수행
- role based 인가 구현
- 회원 가입 및 로그인 API 추가
```

```text
fix: 토큰 만료 응답 처리 수정

- 만료된 access token 요청 시 401 응답 반환
- refresh token 검증 실패 케이스 보완
- 인증 실패 로그 메시지 정리
```

```text
chore: gradle 설정 정리

- kotlin jvm plugin 버전 업데이트
- 테스트 실행 옵션을 JUnit 5 기준으로 통일
- 사용하지 않는 의존성 제거
```

## Rules

Keep the summary concise and imperative. Use lowercase commit types only. Choose `feat` only when user-facing or API-facing behavior is added. Choose `fix` only when correcting broken behavior. Use `chore` for maintenance tasks that do not change product behavior.

Always include a commit body when creating a commit for actual implementation work. Leave one blank line after the title, then list the work performed with `-` bullets. Each bullet should describe a concrete change, not a vague category. Mention important framework, API, test, configuration, migration, or security changes when they are part of the work.

Short one-line commits are acceptable only for trivial repository maintenance, such as typo fixes or formatting-only changes.
