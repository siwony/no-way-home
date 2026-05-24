# Director Brief: 등기부등본·임대차 계약서 자동 입력

Status: READY_FOR_UI_UX

## Goal

기존 주택 계약 위험도 진단을 실제 서비스용 문서 기반 흐름으로 확장한다. 사용자가 등기부등본과 임대차 계약서를 등록하면 시스템이 문서에서 핵심 정보를 추출하고, 사용자의 검토/승인 후 계약 기본 정보와 등기 확인 입력에 자동 반영한다.

## Background

현재 기능은 계약 기본 정보, 등기 확인, 건축물대장 확인, 시세 입력을 사용자가 직접 입력하는 흐름이다. 실제 서비스에서는 사용자가 등기부등본과 계약서를 이미 보유하고 있으며, 이 문서를 기반으로 주소, 임대인, 소유자, 보증금, 월세, 계약일, 권리관계 위험 신호를 자동으로 채우는 경험이 필요하다.

이 기능은 MVP식 demo parser가 아니라 production document intake로 다룬다. 원본 문서, 추출 텍스트, 추출 필드, 사용자 승인 상태, 분석 입력 반영 상태가 분리되어야 한다.

## Scope

### Include

- 등기부등본 PDF 업로드와 자동 추출 흐름
- 임대차 계약서 PDF/image 업로드와 자동 추출 흐름
- 문서 처리 상태 모델: `UPLOADED`, `EXTRACTING`, `REVIEW_REQUIRED`, `APPROVED`, `FAILED`, `DELETED`
- 필드별 값, 원문 근거, 신뢰도, 검토 상태
- 사용자 수정/제외/승인 UI
- 승인된 필드만 기존 house check 입력값에 반영
- 계약서와 등기부등본의 주소/임대인/소유자/금액 불일치 감지
- 문서 원본과 추출 artifact 암호화 저장
- 접근 권한, 감사 로그, 삭제 요청 대응을 고려한 도메인 모델
- fake OCR/extraction adapter 기반 테스트 가능한 구조

### Exclude

- 서버가 인터넷등기소 로그인, 인증, 결제, 공동인증서 처리를 대신 수행하는 기능
- 인증서, 공동인증서 비밀번호, 카드정보, 인터넷등기소 계정 정보 저장
- 주민등록번호 원문 저장
- 자동 추출값을 사용자 승인 없이 최종 분석 입력으로 확정하는 흐름
- 법률 자문, 계약 가능 여부 확정, 보증금 회수 가능성 확정 문구
- 특정 외부 OCR/provider에 도메인 로직이 직접 결합되는 구현

## Required Documents

- `doc/feat/house-document-auto-fill/README.md`
- `doc/feat/house-document-auto-fill/scope.md`
- `doc/feat/house-document-auto-fill/expected-behavior.md`
- `doc/feat/house-document-auto-fill/constraints.md`
- `doc/feat/house-document-auto-fill/tests.md`
- `doc/agent-work/house-document-auto-fill/`

## Acceptance Criteria

- 사용자는 등기부등본과 임대차 계약서를 업로드할 수 있다.
- 사용자는 문서별 처리 상태와 실패 원인을 볼 수 있다.
- 추출된 각 필드는 값, 문서 출처, 원문 근거, 신뢰도, 검토 필요 여부를 표시한다.
- 사용자는 추출 필드를 수정하거나 제외할 수 있다.
- 사용자 승인 전에는 기존 분석 입력값이 자동 확정되지 않는다.
- 승인 후 승인된 필드만 기존 진단 입력 모델에 반영된다.
- 계약서와 등기부등본 간 주소/임대인/소유자/금액 불일치를 확인 필요 항목으로 표시한다.
- 다른 사용자는 문서, 추출 결과, 승인 결과에 접근할 수 없다.
- 원본 문서와 추출 artifact는 민감 정보로 처리되고 브라우저 영구 저장소에 남지 않는다.
- backend/frontend/Playwright E2E 테스트가 문서 업로드 -> 추출 검토 -> 승인 -> 자동 입력 반영 -> 분석 흐름을 검증한다.
- 자동화 테스트는 mock fixture를 사용할 수 있고, 사용자가 제공한 실제 PDF 두 개는 local-only 검증 입력으로 추가 사용한다. 해당 PDF는 커밋하지 않는다.

## Constraints

- 기존 위험도 표현 원칙을 유지한다. “안전하다”, “계약해도 된다” 같은 단정 문구는 금지한다.
- 실제 service-ready 구조를 목표로 하되, 첫 구현은 외부 provider 없이 fake adapter와 fixture로 검증 가능해야 한다.
- 외부 OCR/provider 연동은 포트/어댑터 경계로 둔다.
- 민감정보 로그 출력과 예외 메시지를 제한한다.
- 기존 PR #1의 backend/frontend house check 흐름에 의존한다. 이 브랜치는 PR #1 위에 stack되어 시작했고, PR #1 병합 후 `main` 기준으로 재정렬해야 한다.

## Open Questions

- 실제 OCR/provider 후보와 비용/정확도/보안 기준은 별도 제품 결정이 필요하다.
- 등기부등본 “직접 확인”의 법적/운영 의미를 확정해야 한다. 첫 scope에서는 사용자가 확보한 문서의 구조화와 검토 지원을 구현하고, 공식 확인 provider는 adapter 확장점으로 둔다.
- 문서 retention 기간과 삭제 정책의 기본값이 필요하다.
