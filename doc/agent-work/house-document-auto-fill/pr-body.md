## 개요

- Work ID: `house-document-auto-fill`
- 기능명: 등기부등본·임대차 계약서 자동 입력
- 관련 문서:
  - `doc/agent-work/house-document-auto-fill/00-director-brief.md`
  - `doc/agent-work/house-document-auto-fill/01-ui-ux-plan.md`
  - `doc/agent-work/house-document-auto-fill/07-director-final-review.md`
  - `doc/agent-work/house-document-auto-fill/08-pr-lifecycle.md`

등기부등본과 임대차 계약서를 등록하면 문서에서 핵심 정보를 추출하고, 사용자의 검토/승인 후 기존 주택 계약 위험도 진단 입력값에 자동 반영하는 실제 서비스용 document intake 기능입니다.

이번 reopened loop에서는 기본 extraction path를 fake adapter에서 실제 PDF parser + OpenAI review 경로로 교체했습니다. PDF는 PDFBox로 유효성 및 페이지 수를 확인한 뒤 OpenAI Responses API `input_file`로 원본 PDF를 전달하며, fake extraction은 `housecheck.document-intake.extraction.provider=fake`를 명시한 경우에만 활성화됩니다.

## 작업 내용

- [x] Director brief 작성
- [x] UI/UX plan 작성 및 Director approval 완료
- [x] Draft PR 생성
- [x] Backend 재구현: 실제 PDF parser + AI extraction/review adapter
- [x] Frontend 재검증: backend failure message 노출 경로 확인
- [x] QA 재실행
- [x] Director final review 재실행

## 리뷰 필요

- 기본 provider가 `openai`로 고정되었고, API key가 없을 때 fake fallback 없이 `FAILED / AI_PROVIDER_UNAVAILABLE`로 끝나는지 확인
- AI 인증은 `HOUSECHECK_DOCUMENT_INTAKE_AI_AUTH_MODE=api-key|access-token|oauth-client-credentials` 환경변수로 선택 가능함
- `oauth-client-credentials`는 OpenAI 공식 API 인증 우회가 아니라 OAuth token endpoint가 있는 호환 AI gateway/proxy를 위한 모드임
- PDFBox 파서 이후 OpenAI 요청에 `input_file`과 `data:application/pdf;base64,` PDF payload가 포함되는지 확인
- AI 응답이 top-level `output_text`와 nested `output[].content[].text` 모두에서 파싱되는지 확인
- AI 결과가 persistence 전에 field key, document type, normalized value, `sourcePage`, `sourceText`, confidence, warning payload 기준으로 검증되는지 확인
- frontend 추가 수정 없이 backend `failure.code` / `failure.message` 노출이 충분한지 확인
- 사용자 제공 실제 PDF 두 개는 계속 local-only QA 입력으로만 사용하고 커밋하지 않는지 확인
- stacked PR caveat:
  - PR #2 base는 `feat/house-risk-agent-prompts/frontend`이며, PR #1 머지 후 `main` 기준 rebase가 필요함
- residual risk:
  - 실제 OpenAI 서비스에 대한 end-to-end 호출은 로컬 `OPENAI_API_KEY` 부재로 검증하지 못했음
- PR: https://github.com/siwony/no-way-home/pull/2

최종 검증 요약:

- `./gradlew test --rerun-tasks --tests 'com.nowayhome.housecheck.application.DocumentIntakePdfTextExtractorTest' --tests 'com.nowayhome.housecheck.application.OpenAiDocumentIntakeExtractionAdapterTest' --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerOpenAiUnavailableIntegrationTest' --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerIntegrationTest'`
  - PASS
- `./gradlew test --tests 'com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest'`
  - PASS
- `./gradlew test`
  - PASS
- `cd frontend && npm test`
  - PASS
- `cd frontend && npm run build`
  - PASS
- 실제 로컬 PDF + no-key runtime smoke
  - PASS
  - REGISTRY / LEASE_CONTRACT 모두 `FAILED / AI_PROVIDER_UNAVAILABLE`
- 실제 로컬 PDF + mock OpenAI runtime smoke
  - PASS
  - REGISTRY / LEASE_CONTRACT 모두 `REVIEW_REQUIRED`
  - mock request log에서 `input_file` 2회, `data:application/pdf;base64,` 2회 확인

## 스크린샷 (필요한 경우)

- Mock fixture extraction review: https://github.com/siwony/no-way-home/blob/feat/house-document-auto-fill/doc/agent-work/house-document-auto-fill/assets/qa-document-review-mock.png
- Mock fixture compare/apply preview: https://github.com/siwony/no-way-home/blob/feat/house-document-auto-fill/doc/agent-work/house-document-auto-fill/assets/qa-apply-preview-mock.png
