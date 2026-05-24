# Director Plan Approval: 공공 실거래가 시세 자동 조회

Status: APPROVED

Decision: APPROVED

## Review Summary

UI/UX plan은 사용자가 조회 결과를 검토하고 명시적으로 적용/저장하는 흐름을 유지한다. XML-only, 수동 fallback, KB 제외, public API key server-only 제약을 충족한다.

## Approved Scope

- 공공 실거래가 XML lookup API와 provider 구현
- 기존 시세 입력 카드의 조회/preview/적용 UI 구현
- mock XML 기반 backend/frontend/e2e 테스트
- PR body에 frontend 증거 스크린샷 첨부

## Change Requests

- 없음.

## Decision Notes

Use one decision value: `APPROVED`, `CHANGES_REQUESTED`, or `BLOCKED`.

## PR Handoff

When decision is `APPROVED`, Director must prepare `pr-body.md`, update `08-pr-lifecycle.md`, commit planning docs, and open a Draft PR before Developer agents start implementation.
