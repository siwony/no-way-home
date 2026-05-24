---
name: playwright-e2e
description: Use this skill when planning, writing, running, or reviewing Playwright-based end-to-end tests for repository features.
---

# Playwright E2E Testing

Use Playwright for browser-level or API-level end-to-end verification. Before writing or running tests, create a feature checklist so the test scope is explicit.

## Pre-Test Checklist

Read the feature request from `doc/feat/`, issue text, or implementation diff. Then write a checklist in the feature document or create `doc/feat/<feature>-e2e-checklist.md`.

The checklist must cover:

- main user flow or API flow
- required inputs and validation failures
- authentication and authorization behavior
- important success states and persisted results
- error states, empty states, and boundary cases
- security, privacy, or permission-sensitive behavior
- browser, viewport, or device coverage when UI is involved

Do not start E2E implementation until the checklist exists.

## Test Location

Prefer `e2e/` for Playwright specs and fixtures:

```text
e2e/
  fixtures/
  specs/
    user-registration.spec.ts
playwright.config.ts
```

If the project already has a different Playwright layout, follow the existing convention.

## Test Design

Map each checklist item to at least one test or assertion. Use stable locators such as `getByRole`, `getByLabel`, and `getByTestId`; avoid brittle CSS selectors. Keep tests independent by creating their own data or resetting state through fixtures, APIs, or database cleanup.

For Kotlin backend flows without a UI, use Playwright request tests to verify real HTTP behavior across authentication, validation, persistence, and authorization boundaries.

## Commands

Discover the package manager and scripts before running commands. Common commands:

```sh
npx playwright test
npx playwright test e2e/specs/user-registration.spec.ts
npx playwright test --ui
npx playwright show-report
```

Report the checklist coverage, command results, and any untested checklist items after running tests.

## PR Evidence

For frontend work that needs screenshots in a PR, save stable evidence under `doc/agent-work/<work-id>/assets/` instead of temporary Playwright output directories. Commit those assets and reference them from `doc/agent-work/<work-id>/pr-body.md`.
