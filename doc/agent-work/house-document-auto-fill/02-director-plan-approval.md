# Director Plan Approval: 등기부등본·임대차 계약서 자동 입력

Status: COMPLETE

Decision: APPROVED

## Review Summary

기존 UI/UX plan의 사용자 흐름 자체는 계속 유효하다. 사용자는 문서를 올리고, 추출 상태를 보고, 필드를 검토/승인한 뒤 기존 입력값에 반영한다. 이번 loop는 주로 backend extraction path와 failure semantics를 production-intent로 끌어올리는 변경이므로, UI/UX plan을 다시 작성하지 않고 Director approval addendum으로 진행해도 충분하다.

다만 이전 final review에서 follow-up으로 남겨두었던 "real OCR/provider integration"은 더 이상 후속 과제가 아니다. 이제 승인 범위 안에서 실제 PDF parser + AI review adapter를 구현해야 하며, fake extraction은 test/local opt-in 전용으로 격리해야 한다.

## Approved Scope

- 기존 house check create 전에 문서를 먼저 올리는 document intake session 구조와 review/apply UX는 유지한다.
- 등기부등본/임대차 계약서 PDF 처리에서는 실제 parser library를 사용해 로컬 텍스트 추출 단계를 반드시 거친다.
- AI extraction/review는 기존 `DocumentIntakeExtractionPort` 뒤의 새 adapter에서 수행한다.
- AI adapter는 structured output schema를 강제하고, schema 불일치 또는 provider 응답 이상 시 명시적으로 실패한다.
- 운영 기본 설정에서 fake adapter는 비활성화한다. fake adapter 활성화는 test profile 또는 명시적 local opt-in 설정이 있을 때만 허용한다.
- `OPENAI_API_KEY` 또는 동등한 provider 필수 설정이 없으면 startup validation 또는 request-time validation으로 명시적 오류를 반환해야 한다. 무음 fallback 금지.
- 사용자가 제공한 실제 파일 `6_부동산_등기사항_전부증명서.pdf`, `임대차계약서.pdf`는 local-only QA 입력으로만 사용하고 저장소에 커밋하지 않는다.
- 추출 필드는 계속 `value`, `sourceDocument`, `sourcePage`, `sourceText`, `confidence`, `reviewStatus`를 포함한다.
- 사용자 승인 전에는 기존 계약 기본 정보나 등기 확인 값이 확정 변경되지 않는다.
- 승인 후 승인된 필드만 기존 입력 폼에 반영하고 source chip을 표시한다.
- 기존 입력값과 승인값이 충돌하면 조용히 overwrite하지 않고 비교 요약을 거친다.
- 임차인명은 기본적으로 저장 제외 또는 마스킹한다. 특약 문장은 review-only 후보로 유지하고 자동 입력값에는 반영하지 않는다.
- 원본 문서 재다운로드는 이번 loop 범위에서 제외한다. 검토 UI는 제한된 원문 발췌와 페이지 정보 중심으로 유지한다.
- 문서 retention은 설정 가능한 `expiresAt` 모델을 유지한다.
- 문서 삭제/재업로드/재처리 실패 복구 경로를 유지하되, 실패 이유는 `parse failed`, `ai unavailable`, `ai review failed`처럼 구분 가능해야 한다.
- access denied 시 파일명, 추출값, 원문 근거 등 서버 유래 민감 데이터를 화면에서 제거한다.

## Planning Approval Criteria

### Backend

- 실제 PDF parser library를 추가하고, 업로드된 PDF에서 텍스트 추출을 수행하는 서비스/adapter를 구현한다.
- AI extraction/review adapter는 기존 `DocumentIntakeExtractionPort` 뒤에 구현하고, parser 결과와 문서 메타데이터를 입력으로 사용한다.
- provider/API key 미설정 시 명시적 예외 코드와 메시지를 반환하고 fake adapter로 fallback하지 않는다.
- fake adapter는 `test` 또는 명시적 local profile/property에서만 wiring 된다.
- 통합/단위 테스트는 parser 성공, parser 실패, provider unavailable, structured-output validation failure, fake opt-in 경로를 포함한다.
- 실제 OpenAI live call은 현재 로컬에 `OPENAI_API_KEY`가 없으므로 필수 검증 게이트로 두지 않는다. 대신 mock/stubbed adapter 또는 HTTP mocking으로 재현 가능한 테스트를 남긴다.

### Frontend

- 기존 intake/review UI에서 parser 실패와 AI 미설정/실패를 구분 가능한 문구로 노출한다.
- fake extraction이 자동 반영되는 것처럼 보이는 카피나 상태를 제거한다.
- 사용자는 실패 상태에서 재업로드 또는 수기 진행 결정을 할 수 있어야 한다.
- 승인/반영 UX는 유지하되, 새 실패 상태가 기존 compare/apply 흐름을 깨지 않도록 검증한다.

### QA

- mock/stub 기반 자동화로 parser + AI review success path를 재현한다.
- provider unavailable path와 fake opt-in path를 분리 검증한다.
- 로컬 실 PDF 검증은 사용자 파일을 사용하되 커밋하지 않는다.
- secrets 미커밋, local PDF 미커밋, fake adapter non-default 보장을 회귀 항목에 포함한다.

## Change Requests

- 없음. 이 scope로 바로 개발 재개를 승인한다.

## Decision Notes

Use one decision value: `APPROVED`, `CHANGES_REQUESTED`, or `BLOCKED`.

## PR Handoff

When decision is `APPROVED`, Director must update `pr-body.md`, update `08-pr-lifecycle.md`, and ensure the existing PR is moved back to Draft before Developer agents resume implementation.

## Next Agents

- Primary next agent: `kotlin-spring-backend-developer`
- Secondary next agent after backend contract is updated: `frontend-developer`
- Verification gate after implementation: `harness-qa`
