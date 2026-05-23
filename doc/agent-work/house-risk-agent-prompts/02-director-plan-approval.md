# Director Plan Approval: 주택 계약 위험도 진단 서비스

Status: APPROVED

Decision: APPROVED

## Review Summary

UI/UX plan satisfies the Director brief and feature documents for Phase 1. It keeps manual structured findings separate from file upload, preserves analysis with partial inputs, distinguishes uploaded metadata from user-entered facts, and keeps report language within the required non-deterministic risk-signal framing.

## Approved Scope

- Phase 1 flow is fixed as request creation -> document upload -> separate manual findings entry -> manual market price save -> analysis -> report and checklist retrieval.
- Manual structured findings remain outside request creation so `uploaded`, `not reviewed`, and `review completed` states stay distinct for each document type.
- Upload UX stores PDF plus system metadata, with only optional user-entered `issuedDate` in Phase 1.
- Analysis may run without complete document findings or market price, and the report must expose missing-data reasons instead of hiding them.
- Report and checklist responses must distinguish `USER_ENTERED`, `UPLOADED_FILE_METADATA`, and calculated values so automatic analysis and manual facts are not conflated.
- User-facing wording must remain in the `현재 확인된 자료 기준` framing and avoid deterministic safety or legal-judgment language.

## Change Requests

- None.

## Decision Notes

Implementation may proceed. Developer work must still honor the feature constraints outside pure UI/UX scope, including encrypted handling of landlord name and uploaded documents, access control on documents and reports, and a Phase 1 market price design that does not require external lookup and remains compatible with a future provider port boundary.

Use one decision value: `APPROVED`, `CHANGES_REQUESTED`, or `BLOCKED`.
