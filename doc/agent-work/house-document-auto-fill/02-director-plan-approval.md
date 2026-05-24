# Director Plan Approval: 등기부등본·임대차 계약서 자동 입력

Status: COMPLETE

Decision: APPROVED

## Review Summary

UI/UX plan은 Director brief의 목표와 맞다. 계획은 기존 단일 operational workspace를 유지하면서 문서 업로드, 추출 상태, 필드별 근거/신뢰도, 사용자 승인, 승인 필드 반영, 실패 시 수기 입력 복귀를 모두 포함한다.

다만 "실제 서비스용" 요구에 맞춰 다음 구현 기준을 승인 범위에 명시한다. 첫 구현은 서버가 인터넷등기소 로그인/인증/결제/공동인증서 처리를 대신 수행하지 않는다. 사용자가 확보한 등기부등본과 임대차 계약서를 안전하게 업로드하고 구조화해 검토/승인을 돕는다. 공식 등기 확인 provider 또는 OCR provider는 포트/어댑터 확장점으로 둔다.

## Approved Scope

- 기존 house check create 전에 문서를 먼저 올릴 수 있도록 document intake session을 둔다. 이 session은 `X-User-Id` 소유자 경계와 document set id를 가진다.
- 등기부등본 PDF와 임대차 계약서 PDF/JPEG/PNG/WebP를 지원한다. HEIC는 첫 구현에서 제외한다.
- mock extraction adapter와 fixture 기반 테스트를 제공한다. 외부 OCR/provider 네트워크 접근은 핵심 테스트 전제 조건으로 두지 않는다.
- 사용자가 제공한 실제 파일 `6_부동산_등기사항_전부증명서.pdf`, `임대차계약서.pdf`는 local-only QA 입력으로 사용하되 커밋하지 않는다.
- 추출 필드는 `value`, `sourceDocument`, `sourcePage`, `sourceText`, `confidence`, `reviewStatus`를 포함한다.
- 사용자 승인 전에는 기존 계약 기본 정보나 등기 확인 값이 확정 변경되지 않는다.
- 승인 후 승인된 필드만 기존 입력 폼에 반영하고 source chip을 표시한다.
- 기존 입력값과 승인값이 충돌하면 조용히 overwrite하지 않고 비교 요약을 거친다.
- 임차인명은 기본적으로 저장 제외 또는 마스킹한다. 특약 문장은 첫 구현에서 review-only 후보로 보여주고 자동 입력값에는 반영하지 않는다.
- 원본 문서 재다운로드는 첫 구현 범위에서 제외한다. 검토 UI는 제한된 원문 발췌와 페이지 정보 중심으로 제공한다.
- 문서 retention은 설정 가능한 `expiresAt` 모델을 둔다. 기본 사용자 안내는 "민감 문서이며 삭제할 수 있음"까지 제공하고 실제 기간은 환경 설정값으로 운영 가능하게 한다.
- 문서 삭제/재업로드/재처리 실패 복구 경로를 제공한다.
- access denied 시 파일명, 추출값, 원문 근거 등 서버 유래 민감 데이터를 화면에서 제거한다.

## Change Requests

- 없음.

## Decision Notes

Use one decision value: `APPROVED`, `CHANGES_REQUESTED`, or `BLOCKED`.

## PR Handoff

When decision is `APPROVED`, Director must prepare `pr-body.md`, update `08-pr-lifecycle.md`, commit planning docs, and open a Draft PR before Developer agents start implementation.
