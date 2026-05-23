# Director Plan Approval: 주택 계약 위험도 진단 프론트엔드

Status: COMPLETE

Decision: APPROVED

## Review Summary
The UI/UX plan satisfies the Director brief for the first frontend slice and stays within the committed backend Phase 1 boundary. It keeps the product to a single usable tool flow, makes the `X-User-Id` boundary explicit and testable, covers creation, PDF upload, manual findings save, market price save, analysis, report/checklist retrieval, and separates `ACCESS_DENIED` and `ANALYSIS_NOT_READY` from generic errors.

The plan also matches the committed backend constraints reflected in the approved backend work: `estimatedJeonseValue`-only market price input remains a valid path, non-deterministic risk wording is preserved, and sensitive server data is not copied into browser persistent storage. The open question about draft resume or re-edit is not required for this slice because the committed API does not provide a general draft/readback contract for pre-analysis form restoration.

## Approved Scope

- Single-route diagnostic workspace for the first usable frontend flow.
- Explicit `User ID` input that maps to `X-User-Id` on every request, including clear `ACCESS_DENIED` handling that removes prior server-derived content from view.
- Contract creation, registry/building-ledger PDF upload, separate registry/building-ledger manual findings save, market price save, analysis execution, report retrieval, and checklist retrieval using the committed Phase 1 backend.
- UI states for loading, success, validation error, forbidden, and not-ready conditions, with risk-signal wording that avoids deterministic legal or safety conclusions.
- Session-scoped continuity only. `User ID` and `checkId` may be retained for convenience, but landlord name, uploaded files, report payloads, and checklist payloads must not be stored in browser persistent storage.
- `estimatedMarketValue` and `estimatedJeonseValue` are both shown, and saving with only `estimatedJeonseValue` remains within approved scope.
- Result presentation may use tabs or segmented sections, but it must continue to distinguish missing-data cases as `계산 불가` or equivalent explanatory copy.
- Explicit exclusion for this slice: cross-session draft resume, full existing-check re-edit after reload, and generic pre-analysis input rehydration beyond same-session memory flow.

## Change Requests

- None.

## Decision Notes

Use one decision value: `APPROVED`, `CHANGES_REQUESTED`, or `BLOCKED`.
