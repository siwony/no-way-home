# Director Brief: 등기부등본·임대차 계약서 자동 입력

Status: READY_FOR_UI_UX

## Goal

기존 문서 자동 입력 기능을 데모용 fake extraction 수준에서 멈추지 않고, 실제 PDF 파서와 AI 검토 단계를 거치는 production-intent intake 흐름으로 확장한다. 사용자가 등기부등본과 임대차 계약서를 등록하면 시스템은 로컬 PDF 파서로 텍스트를 추출하고, AI 기반 문서 검토 어댑터를 통해 구조화된 필드 후보와 근거를 생성한 뒤, 사용자 검토/승인 후에만 기존 주택 계약 위험도 진단 입력값에 반영해야 한다.

## Background

직전 slice는 문서 intake 세션, 업로드, 검토, 승인, 반영 UI와 보안 저장 구조를 구축했지만, 실제 추출 경로는 `FakeDocumentIntakeExtractionAdapter`가 담당한다. 사용자 지적대로 현재 production path에서 `서울시 마포구 양화로 1` 같은 하드코딩 값이 조용히 반환되는 상태는 허용할 수 없다.

이번 loop의 목적은 "실제 PDF 파서 및 AI로 검토 하는 프로세스"를 기본 경로로 만들고, AI provider 또는 API key가 준비되지 않은 환경에서는 명시적으로 실패시키는 것이다. fake adapter는 테스트 또는 명시적 local opt-in 용도로만 남길 수 있으며, production default나 무음 fallback으로 사용하면 안 된다.

## Scope

### Include

- PDF 업로드 후 실제 parser library를 사용한 로컬 텍스트 추출
- 기존 `DocumentIntakeExtractionPort` 뒤에 AI extraction/review adapter 추가
- AI 요청/응답을 구조화된 JSON schema 기반으로 검증하는 흐름
- 문서 처리 상태에 AI/provider unavailable, parse failure, invalid structured output 같은 명시적 실패 사유 추가
- fake extraction adapter를 test/local opt-in 전용으로 격리하는 설정 구조
- 운영 기본값에서 fake adapter가 절대 자동 선택되지 않도록 하는 구성
- AI provider 미설정 또는 `OPENAI_API_KEY` 부재 시 명시적 오류와 사용자 안내
- 기존 검토/승인/반영 UI에서 새 실패 상태와 재시도 경로 노출
- 테스트 환경에서 provider mocking 또는 stubbed adapter로 자동화 검증
- `.env.example` 또는 동등한 설정 문서에서 필요한 AI 설정 키와 설명 정리

### Exclude

- 인터넷등기소 로그인, 인증, 결제, 공동인증서 대행
- 로컬 개발 환경에 실제 사용자 PDF나 API key를 커밋하는 행위
- 주민등록번호 원문 저장 또는 민감 전문 로그 출력
- AI 결과를 사용자 승인 없이 house check 입력값으로 확정하는 흐름
- provider 종속 SDK/응답 포맷이 도메인 계층으로 직접 새어 나가는 구조
- 이번 slice 안에서 OCR 정확도 튜닝, 다중 provider 라우팅, 비용 최적화까지 완료하는 것

## Required Documents

- `doc/feat/house-document-auto-fill/README.md`
- `doc/feat/house-document-auto-fill/scope.md`
- `doc/feat/house-document-auto-fill/expected-behavior.md`
- `doc/feat/house-document-auto-fill/constraints.md`
- `doc/feat/house-document-auto-fill/tests.md`
- `doc/agent-work/house-document-auto-fill/`

## Acceptance Criteria

- 운영 기본 경로는 fake extraction이 아니라 실제 PDF parser + AI review adapter를 사용한다.
- PDF 입력은 서버에서 실제 parser library를 통해 텍스트 추출을 시도하고, 추출 결과 또는 원본 PDF를 AI adapter 검토 입력으로 사용한다.
- `DocumentIntakeExtractionPort` 경계는 유지되며, 도메인 로직은 특정 AI SDK나 응답 포맷에 직접 결합되지 않는다.
- AI provider 설정이 없거나 `OPENAI_API_KEY`가 없으면 시스템은 명시적으로 실패하고, fake adapter로 조용히 대체되지 않는다.
- fake adapter는 테스트 또는 명시적 local opt-in 설정에서만 활성화된다.
- 사용자는 파싱 실패, AI 미설정, AI 검토 실패를 서로 구분 가능한 상태/메시지로 볼 수 있다.
- 승인 전에는 기존 계약 기본 정보나 등기 확인 값이 자동 확정되지 않는다.
- 승인 후에도 승인된 필드만 기존 입력 폼에 반영된다.
- 자동화 테스트는 parser 기반 경로, AI adapter 호출 경로, provider unavailable 경로, fake adapter opt-in 경로를 검증한다.
- 실제 사용자 PDF와 실제 API key는 저장소에 커밋하지 않는다.

## Constraints

- 기존 위험도 표현 원칙을 유지한다. “안전하다”, “계약해도 된다” 같은 단정 문구는 금지한다.
- production default는 반드시 fail-fast여야 하며, fake adapter 무음 fallback을 허용하지 않는다.
- AI provider live call은 로컬 셸에 `OPENAI_API_KEY`가 없으므로 테스트 전략에서 mock/stub 경로를 기본으로 둔다.
- 향후 live verification이 필요하면 사용자가 별도 키를 제공한 환경에서만 수행한다.
- 민감정보 로그 출력, 예외 메시지, 저장 artifact 범위를 최소화한다.
- 기존 PR #1의 backend/frontend house check 흐름에 의존한다. 이 브랜치는 PR #1 위에 stack되어 시작했고, PR #1 병합 후 `main` 기준으로 재정렬해야 한다.

## Open Questions

- 실제 AI adapter가 PDF 원본과 parser text 중 무엇을 주 입력으로 삼을지 구현 시 결정이 필요하다. 단, parser 단계는 반드시 존재해야 한다.
- OCR이 필요한 스캔형 PDF를 이번 slice에서 어디까지 지원할지 명시가 필요하다. 최소 기준은 "텍스트 추출 불가 시 명시적 실패"다.
- 운영 환경에서 사용할 모델, 비용 한도, timeout/retry 정책은 adapter 구현 시 구체화가 필요하다.
