# Repository Guidelines

## Project Structure & Module Organization

This is a Kotlin/Gradle project. Use the standard layout:

- `src/main/kotlin/`: production Kotlin code
- `src/main/resources/`: resources
- `src/test/kotlin/`: unit and integration tests
- `src/test/resources/`: test fixtures
- `e2e/`: Playwright end-to-end specs
- `doc/feat/`: feature request Markdown files for agent implementation
- `skills/`: reusable agent instructions

Keep `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, and `gradlew` in the repository root.

## Build, Test, and Development Commands

Prefer the Gradle wrapper:

```sh
./gradlew test          # run tests
./gradlew build         # compile, test, and package
./gradlew run           # run the app
./gradlew ktlintCheck   # check Kotlin style when ktlint is configured
./gradlew ktlintFormat  # format Kotlin code when ktlint is configured
```

## Coding Style & Naming Conventions

Kotlin style and responsibility rules live in [doc/kotlin-coding-convention.md](doc/kotlin-coding-convention.md). Read that document before adding or refactoring production code.

## Testing Guidelines

Add tests for behavior changes. Mirror production package paths under `src/test/kotlin/` and use descriptive names such as `createsUserWhenEmailIsValid`. JUnit 5 or Kotest are acceptable; once chosen, stay consistent.

For Playwright E2E work, use [skills/playwright-e2e/SKILL.md](skills/playwright-e2e/SKILL.md) and create the feature checklist before writing tests.

## Commit & Pull Request Guidelines

Commit rules live in [skills/git-commit-rules/SKILL.md](skills/git-commit-rules/SKILL.md). Read that skill before creating or rewriting commits.

GitHub Draft PR workflow lives in [skills/github-draft-pr/SKILL.md](skills/github-draft-pr/SKILL.md). Use it when opening WIP feature PRs with `gh`.

Pull requests should include a summary, test results, linked issues, and screenshots or logs for user-visible behavior. Call out migrations, config changes, and follow-up work.

## Feature Docs For Agents

Store implementation-ready feature specs in `doc/feat/`. Each document should describe the goal, scope, expected behavior, tests, and constraints. Start from `doc/feat/feature-template.md` or `doc/feat/work-checklist-template.md`.

## Security & Configuration Tips

Do not commit secrets or local credentials. Keep required environment variables in `.env.example` with safe placeholder values.
