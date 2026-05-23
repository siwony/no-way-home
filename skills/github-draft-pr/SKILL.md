---
name: github-draft-pr
description: Use this skill when creating, updating, or preparing GitHub Draft Pull Requests with the GitHub CLI for repository feature work.
---

# GitHub Draft PR Workflow

Use this workflow when a feature starts from checklist or planning Markdown and should be opened as a Draft PR before implementation is complete.

## Prerequisites

- `gh` must be installed.
- `gh auth status` must succeed.
- `origin` must point to the GitHub repository.
- Work must happen on a feature branch, not directly on `main`.
- Branch names must follow `skills/git-branch-rules/SKILL.md`.

If GitHub CLI is not authenticated, stop and ask the user to run:

```sh
gh auth login
```

## Before Opening The Draft PR

1. Create or update a feature document under `doc/feat/`.
2. If the feature needs task tracking, start from `doc/feat/work-checklist-template.md`.
3. Copy `.github/PULL_REQUEST_TEMPLATE.md` to `doc/feat/<feature>/pr-body.md` and fill it.
4. Commit the feature document, checklist, and PR body first.
5. Follow `skills/git-branch-rules/SKILL.md` for branch naming and `skills/git-commit-rules/SKILL.md` for the commit message.

## Draft PR Creation

Use the PR title format:

```text
(WIP) feat: 기능 이름
```

Fill `.github/PULL_REQUEST_TEMPLATE.md` as follows:

- `개요`: feature goal and linked checklist/document path
- `작업 내용`: planned implementation tasks
- `리뷰 필요`: design decisions, checklist review points, or risky areas
- `스크린샷 (필요한 경우)`: `해당 없음` until UI evidence exists

Use the helper script when possible:

```sh
scripts/create-draft-pr.sh --feature "기능 이름" --body doc/feat/<feature>/pr-body.md
```

## During Implementation

After each meaningful work unit:

- update the local checklist in `doc/feat/`
- commit the code and checklist update together when they describe the same progress
- push the branch
- update the PR body with current status when needed:

```sh
gh pr edit --body-file doc/feat/<feature>/pr-body.md
```

## Ready For Review

When all checklist items are complete:

1. Run relevant tests.
2. Update the PR body with final test results and review notes.
3. Remove `(WIP)` from the PR title.
4. Mark the PR ready:

```sh
gh pr ready
```

Do not mark a PR ready while required tests are failing or checklist items remain open.
