# QA Report: 등기부등본·임대차 계약서 자동 입력

Status: COMPLETED

Result: PASS

## Summary

Mock/generated fixture 기준 승인 흐름은 최초 QA에서 통과했다. 이후 사용자 제공 실제 PDF 2종이 `HTTP 413`으로 거부되는 결함을 발견했고, upload size policy와 multipart limit, frontend 안내 문구를 수정한 뒤 같은 local-only PDF로 재검증했다.

최종 재검증 기준으로 `User ID 적용 -> 문서 세션 생성 -> 실제 등기부등본 PDF 업로드 -> 실제 임대차계약서 PDF 업로드 -> 추출 검토 -> 20개 필드 승인 -> 반영 비교 -> 계약/등기 입력 자동 반영 -> house check 생성`이 통과했다. 실제 PDF 원본은 repo로 복사하지 않았고, 저장된 intake 원본은 암호화된 `.bin` 파일에서 `%PDF` 평문 헤더가 검출되지 않았다.

## Tests Executed

```text
cd frontend && npm test
- PASS (3 files, 17 tests)

cd frontend && npm run build
- PASS

./gradlew test --tests 'com.nowayhome.housecheck.application.DocumentIntakeFilePolicyTest' --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerIntegrationTest' --tests 'com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest' --rerun-tasks
- PASS after `docker compose up -d postgres`

./gradlew test
- PASS

docker compose up -d postgres
- PASS; `no-way-home-postgres` healthy on `localhost:5432`

./gradlew bootRun
- PASS on `http://127.0.0.1:8080`

cd frontend && npm run dev -- --host 127.0.0.1 --port 4173
- PASS on `http://127.0.0.1:4173`

Playwright browser QA with mock/generated fixtures
- PASS for approved flow:
  - applied `User ID=qa-browser-pass`
  - created document intake session
  - uploaded mock registry PDF and mock lease PDF
  - observed extraction review with mismatch warnings
  - executed representative field actions: approve, edit (`보증금=62000000`), exclude (`특약 핵심 문장 후보`)
  - opened compare/apply summary and verified overwrite choice state
  - applied approved fields into contract and registry forms with source notes
  - created house check from applied contract values
  - uploaded analysis-side registry PDF and saved registry findings
  - verified browser storage contained only allowed session keys
  - simulated stale document session under a different `User ID` and observed `ACCESS_DENIED` boundary cleanup

Playwright browser QA with user-provided real local PDFs
- PASS
  - registry file: `/Users/jeongcool/me/no-way-home/6_부동산_등기사항_전부증명서.pdf`
  - lease file: `/Users/jeongcool/me/no-way-home/임대차계약서.pdf`
  - uploaded both files through the document-intake UI
  - observed both slots transition to `검토 필요`
  - approved 20 extracted fields
  - opened `반영 비교 요약`
  - applied approved fields into contract and registry inputs
  - created house check `bd31b759-c0a1-4f45-b0ad-26297b8cac5d`
  - document session was `30f7e3a3-033a-491f-980a-334c739d2786`
  - applied contract values included address `서울시 마포구 양화로 1`, deposit `60000000`, landlord `임대인`, current owner `임대인`
```

## DB And Storage Evidence

```text
select document_type, file_size, processing_status, storage_key
from document_intake_document
where session_id = '30f7e3a3-033a-491f-980a-334c739d2786';

LEASE_CONTRACT | 9679921  | APPROVED | document-intakes/.../lease-contract-*.pdf.bin
REGISTRY       | 11006061 | APPROVED | document-intakes/.../registry-*.pdf.bin
```

- Stored encrypted intake files were created under `/private/var/folders/.../no-way-home/housecheck/document-intakes/30f7e3a3-033a-491f-980a-334c739d2786/`.
- Stored encrypted file sizes were original size + 32 bytes of encryption metadata/tag overhead.
- `grep -a '%PDF'` against both stored `.bin` files returned no match.
- The two real PDFs remained local-only inputs and were not copied into `doc/agent-work/**` or committed.

## Passed Cases

- [05-qa-plan.md](/Users/jeongcool/me/no-way-home/doc/agent-work/house-document-auto-fill/05-qa-plan.md:1) checklist was created before browser E2E and used as execution scope.
- Frontend regression commands passed: `npm test`, `npm run build`.
- Backend focused integration suite passed with PostgreSQL: `DocumentIntakeFilePolicyTest`, `DocumentIntakeControllerIntegrationTest`, `HouseCheckControllerIntegrationTest`.
- Full backend test suite passed with `./gradlew test`.
- Mock document-intake flow passed end to end through review/apply and contract form population.
- Compare/apply UX showed explicit conflict choice state for approved overwrite and correctly applied the edited deposit in mock flow.
- Contract and registry forms showed document source notes after apply.
- House check creation from applied contract values succeeded.
- Registry findings saved successfully in mock flow once the existing analysis-side registry PDF prerequisite was satisfied.
- Stale session access boundary cleared document session state and hid prior file names/extracted values for a different `User ID`.
- Browser persistent storage remained limited; the real-PDF run ended with no stored browser keys in the fresh Playwright context.
- User-provided real local registry PDF and lease PDF are now accepted and persisted as encrypted intake files.
- Oversized upload behavior is bounded by a 20MB policy and returns structured `DOCUMENT_INTAKE_FILE_TOO_LARGE` / `413` handling.
- Stable visual evidence captured in [qa-document-review-mock.png](/Users/jeongcool/me/no-way-home/doc/agent-work/house-document-auto-fill/assets/qa-document-review-mock.png).
- Stable visual evidence captured in [qa-apply-preview-mock.png](/Users/jeongcool/me/no-way-home/doc/agent-work/house-document-auto-fill/assets/qa-apply-preview-mock.png).

## Resolved Defects

| ID | Severity | Scenario | Previous Actual | Resolution |
|---|---|---|---|---|
| QA-01 | High | Real local PDFs provided for QA should be accepted by document intake | Both provided PDFs were rejected before persistence with `HTTP 413` / `MaxUploadSizeExceededException`, and direct API response body was empty. | Added explicit 20MB document-intake upload policy, Spring multipart limits, JSON `413` handling, frontend 20MiB preflight validation, and server-message upload feedback. Re-ran the real-PDF browser flow successfully through house check creation. |

## Result Notes

Use one result value: `PASS`, `FAIL`, or `BLOCKED`.

- Result is `PASS`.
- The current extraction adapter remains fake by design. Real OCR/provider integration is still a follow-up behind the existing port.
- Existing analysis-side `등기 PDF 업로드` is still required before `등기 수기 확인 저장` succeeds. This did not block document-intake real-PDF upload, extraction review, apply, or house check creation.

## Reopen QA: real PDF parser + OpenAI default path

Status: COMPLETED

Result: PASS

### Reopen Scope

- 현재 uncommitted workspace 기준으로 실제 PDF 파서 + OpenAI 기본 경로가 독립적으로 재현되는지 최소 범위로 재검증
- 실제 로컬 PDF 2종에 대해 no-key 실패 경로와 mock OpenAI 성공 경로를 각각 재실행
- frontend가 새 backend failure code/message를 그대로 노출하는지 소스/테스트 근거 확인

### Commands Executed

```text
docker compose up -d postgres
- PASS

./gradlew test --rerun-tasks --tests 'com.nowayhome.housecheck.application.DocumentIntakePdfTextExtractorTest' --tests 'com.nowayhome.housecheck.application.OpenAiDocumentIntakeExtractionAdapterTest' --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerOpenAiUnavailableIntegrationTest' --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerIntegrationTest'
- PASS

./gradlew test --tests 'com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest'
- PASS

cd frontend && npm test
- PASS (3 files, 17 tests)

cd frontend && npm run build
- PASS

OPENAI_API_KEY='' ./gradlew bootRun
- PASS for runtime smoke on http://127.0.0.1:8080

curl -X POST /api/document-intakes
curl -X POST /api/document-intakes/{sessionId}/documents/registry -F file=@6_부동산_등기사항_전부증명서.pdf
curl -X POST /api/document-intakes/{sessionId}/documents/lease-contract -F file=@임대차계약서.pdf
- PASS
- sessionId: c6a49e54-f179-42ac-8933-967f701af290
- REGISTRY: FAILED / AI_PROVIDER_UNAVAILABLE
- LEASE_CONTRACT: FAILED / AI_PROVIDER_UNAVAILABLE
- fields=0, warnings=0 after lease upload

python3 mock OpenAI server on http://127.0.0.1:18080/v1/responses
OPENAI_API_KEY='qa-mock-key' HOUSECHECK_DOCUMENT_INTAKE_AI_BASE_URL='http://127.0.0.1:18080/v1' ./gradlew bootRun
curl -X POST /api/document-intakes
curl -X POST /api/document-intakes/{sessionId}/documents/registry -F file=@6_부동산_등기사항_전부증명서.pdf
curl -X POST /api/document-intakes/{sessionId}/documents/lease-contract -F file=@임대차계약서.pdf
- PASS
- sessionId: 14bcc279-025b-48b1-baef-6dcb915ab57f
- REGISTRY: REVIEW_REQUIRED
- LEASE_CONTRACT: REVIEW_REQUIRED
- fields=5, warnings=0 after lease upload
- mock request log evidence:
  - INPUT_FILE_REQUESTS=2
  - PDF_DATA_URL_REQUESTS=2
```

### Executed Checks And Evidence

- PDFBox 파서 단위 테스트가 통과했다. 빈 페이지 PDF도 메타데이터를 유지하고, 잘못된 PDF는 `PDF_PARSE_FAILED`로 실패한다.
- OpenAI 어댑터 단위 테스트가 통과했다. top-level `output_text`와 nested `output[].content[].text`를 모두 파싱하고, blank-text PDF도 `input_file` 첨부로 처리한다.
- 기본 provider no-key 통합 테스트가 통과했다. `DocumentIntakeControllerOpenAiUnavailableIntegrationTest`는 기본 OpenAI provider에서 API key가 없을 때 `FAILED / AI_PROVIDER_UNAVAILABLE`를 검증한다.
- 실제 로컬 PDF 2종으로 재실행한 런타임 smoke에서도 두 문서가 모두 `AI_PROVIDER_UNAVAILABLE`에 도달했다. 이때 `PDF_TEXT_EXTRACTION_EMPTY`로 막히지 않았으므로 스캔형 PDF가 파서 단계를 통과하는 점은 재확인됐다.
- mock OpenAI 런타임 smoke에서 같은 로컬 PDF 2종이 모두 `REVIEW_REQUIRED`에 도달했다. mock 서버 로그 `/tmp/mock-openai-requests.log`에는 `input_file`와 `data:application/pdf;base64,` 흔적이 각각 2회 기록됐다.
- frontend 실패 노출은 소스/테스트로 재확인했다.
  - failed slot은 backend `failure.code`와 `failure.message`를 그대로 렌더링한다. 근거: [frontend/src/App.tsx](/Users/jeongcool/me/no-way-home/frontend/src/App.tsx:1245)
  - 업로드 단계의 `ApiError`도 서버 `message`를 그대로 유지한다. 근거: [frontend/src/validation.ts](/Users/jeongcool/me/no-way-home/frontend/src/validation.ts:96)
  - 이에 대한 단위 테스트가 있다. 근거: [frontend/src/validation.test.ts](/Users/jeongcool/me/no-way-home/frontend/src/validation.test.ts:111)

### Defects

- 없음

### Residual Risks

- 실제 OpenAI 서비스와의 end-to-end 호출은 이번 QA에서 검증하지 않았다. 이번 루프는 로컬 mock Responses 서버까지만 확인했다.
- 이미지 입력 경로는 이번 재검증 범위에서 제외했다. 현재 reopen scope는 사용자 제공 실제 PDF 2종 기준이다.
- 브라우저 실화면 smoke는 이번 루프에서 생략했다. failure message 노출은 source/test evidence로 충분하다고 판단했다.
