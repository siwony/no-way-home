---
name: git-branch-rules
description: Use this skill when creating, checking, or naming Git branches for this repository.
---

# Git Branch Rules

Create work branches from the latest `main`. Do not implement directly on `main`.

## Branch Name Patterns

Use only these patterns:

```text
bug/{버그명}
feat/{기능명}
feat/{기능명}/{구현명}
doc/{문서화명}
```

## When To Use

- `bug/{버그명}`: 버그 수정 작업
- `feat/{기능명}`: 하나의 기능을 한 브랜치에서 구현하는 작업
- `feat/{기능명}/{구현명}`: 하나의 큰 기능을 여러 구현 단위로 나누는 작업
- `doc/{문서화명}`: 문서화만 수행하는 작업

## Naming Rules

Use short, descriptive branch segments. Prefer lowercase kebab-case slugs for tooling compatibility.

Examples:

```text
bug/token-expiration
feat/member-auth
feat/member-auth/jwt-filter
feat/member-auth/role-authorization
doc/api-guide
```

Keep the branch name aligned with the commit and PR title. A branch under `feat/member-auth/jwt-filter` should open a PR such as `(WIP) feat: 회원 인증 JWT 필터 구현`. A branch under `doc/api-guide` should use a title such as `chore: API 문서 정리`.
