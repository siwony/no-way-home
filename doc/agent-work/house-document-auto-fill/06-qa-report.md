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
