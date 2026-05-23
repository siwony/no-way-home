# Agent Work State: 주택 계약 위험도 진단 서비스

Status: BLOCKED

Work ID: house-risk-agent-prompts

## Current Stage

PR_READY

## Stage Order

1. DIRECTOR_BRIEF
2. UI_UX_PLAN
3. DIRECTOR_PLAN_APPROVAL
4. DRAFT_PR_OPEN
5. DEVELOPMENT
6. UI_UX_ACCEPTANCE
7. QA_PLAN
8. QA_REPORT
9. DIRECTOR_FINAL_REVIEW
10. PR_READY

## Loop History

| Date | From | To | Reason |
|---|---|---|---|
| 2026-05-24 | DEVELOPMENT | DEVELOPMENT | Backend slice implemented but blocked by Flyway/JPA startup mismatch; integration tests cannot pass until schema migration runs before Hibernate validation. |
| 2026-05-24 | QA_PLAN | QA_REPORT | QA executed. Result is FAIL due to an unusable estimatedJeonseValue-only flow and missing sensitive-data encryption boundary. |
| 2026-05-24 | QA_REPORT | DEVELOPMENT | Director final review set `CHANGES_REQUESTED` for backend developer. Required rework: preserve the approved `estimatedJeonseValue`-only API contract or reject it explicitly, add Phase 1 encryption at rest for landlord name and uploaded documents, and add/rerun automated tests for both defects. |
| 2026-05-24 | DEVELOPMENT | UI_UX_ACCEPTANCE | Backend rework for QA-01 and QA-02 completed. Jeonse-only market input is now preserved end-to-end with conservative report semantics, landlord name and uploaded documents are encrypted at rest, and the focused plus full Gradle test suites passed. |
| 2026-05-24 | UI_UX_ACCEPTANCE | QA_PLAN | UI/UX re-review approved the backend rework. `estimatedJeonseValue`-only input is preserved as saved user input, market-value-dependent metrics remain explicitly unavailable when needed, and report wording stays aligned with the approved Phase 1 messaging constraints. |
| 2026-05-24 | QA_PLAN | DIRECTOR_FINAL_REVIEW | QA rerun passed. Full `./gradlew test` and fresh focused reruns verified QA-01 and QA-02 fixes: jeonse-only input remains visible through report generation, and landlord name plus uploaded document bytes are encrypted at rest. |
| 2026-05-24 | DIRECTOR_FINAL_REVIEW | MERGE_READY | Director final review marked the work item READY. Required gate decisions are satisfied, local work logs are updated, and the approved Phase 1 scope is merge-ready with only non-blocking residual risks remaining. |
| 2026-05-24 | MERGE_READY | PR_READY | Harness-to-PR lifecycle was added retroactively. The work is implementation-ready but PR-ready is blocked because the work is on `main` and no Draft PR lifecycle exists yet. |
