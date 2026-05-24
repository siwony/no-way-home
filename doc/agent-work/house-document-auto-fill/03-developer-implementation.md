# Developer Implementation: 등기부등본·임대차 계약서 자동 입력

Status: READY_FOR_DIRECTOR_FINAL_REVIEW

## Prerequisites

- Director planning approval is `APPROVED`: [x] yes / [ ] no

## Backend Work

- [x] Added a standalone `document_intake_session` model owned by `X-User-Id`, independent from `house_check_request`, so document upload/review can start before normal house check creation.
- [x] Added encrypted document storage for intake originals under the existing filesystem+AES/GCM convention and encrypted sensitive extracted values/evidence in the database.
- [x] Added document lifecycle states `UPLOADED`, `EXTRACTING`, `REVIEW_REQUIRED`, `APPROVED`, `FAILED`, `DELETED`.
- [x] Added a fake extraction port/adapter boundary that returns canonical extracted fields plus fake mismatch signals without calling an external OCR/provider.
- [x] Added field review actions (`APPROVE`, `EDIT`, `EXCLUDE`) and an application payload response that frontend can apply into the existing contract form and registry findings form.
- [x] Enforced owner boundary on session/document/field/payload endpoints with the same `X-User-Id` guard style used by the existing house check APIs.
- [x] Avoided storing resident registration numbers. Tenant information is not persisted; special terms remain review-only and are not included in the application payload.

### Endpoint Contract

- `POST /api/document-intakes`
  - Header: `X-User-Id`
  - Creates a new intake session and returns `sessionId`, `documents`, `fields`, `warnings`, `createdAt`, `updatedAt`, `expiresAt`.
- `POST /api/document-intakes/{sessionId}/documents/{documentType}`
  - Header: `X-User-Id`
  - Multipart: `file`
  - `documentType` values: `registry`, `lease-contract`
  - Registry accepts PDF only. Lease contract accepts PDF/JPEG/PNG/WebP. HEIC is rejected.
  - Upload stores the original, runs fake extraction synchronously, and returns the full session snapshot.
- `GET /api/document-intakes/{sessionId}`
  - Header: `X-User-Id`
  - Returns the current session snapshot.
- `PUT /api/document-intakes/{sessionId}/fields/{fieldKey}`
  - Header: `X-User-Id`
  - Body: `{"action":"APPROVE"|"EDIT"|"EXCLUDE","editedValue":"..."?}`
  - Updates one extracted field and returns the full session snapshot.
- `GET /api/document-intakes/{sessionId}/application-payload`
  - Header: `X-User-Id`
  - Returns approved data mapped into:
    - `contractForm`
    - `registryFindingsForm`
  - This endpoint does not mutate `house_check_request`.
- `DELETE /api/document-intakes/{sessionId}/documents/{documentType}`
  - Header: `X-User-Id`
  - Deletes the stored original, clears extracted artifacts for that document, marks the document `DELETED`, and returns the full session snapshot.

### Response Notes

- Extracted field rows return at least:
  - `fieldKey`
  - `value`
  - `sourceDocument`
  - `sourcePage`
  - `sourceText`
  - `confidence`
  - `reviewStatus`
- Warning rows return:
  - `type`
  - `message`
  - `relatedFields`
- Application payload includes typed values for:
  - contract form: address, contract type, deposit, monthly rent, dates, landlord
  - registry findings form: owner name, owner/landlord match, trust/seizure/mortgage flags, senior debt amount

## Frontend Work

- [x] Added `/api/document-intakes` frontend client coverage for session create/read, registry or lease upload, field review, delete, and approved-field payload fetch.
- [x] Added document-intake response types and pure client helpers for field labels, compare/apply previews, and field-level source notes.
- [x] Inserted a `문서 자동 입력` card into the existing single-route operational workspace before normal house-check creation so intake can start before `checkId` exists.
- [x] Added fixed upload slots for `등기부등본 PDF` and `임대차 계약서 PDF / 이미지`, including file metadata, processing status, failure messages, re-upload/delete actions, and session status messaging.
- [x] Added `추출 검토 및 승인` UI with mismatch warnings, review counts, document filters, evidence disclosure, and field-level `승인 / 수정 / 제외` actions.
- [x] Added explicit compare/apply flow for approved fields. The UI fetches the application payload only when the user clicks apply, shows conflict rows before overwrite, and updates local contract or registry forms only after confirmation.
- [x] Added field-level source notes (`자동 입력 반영`, `사용자 수정됨`) and preserved manual edits after apply.
- [x] Preserved `ACCESS_DENIED` handling for document-intake calls by clearing document session state, apply previews, and document-derived source notes from the screen.
- [x] Kept browser storage limited to `user-id`, `check-id`, and `document-intake-session-id`. No file contents, extracted values, payloads, or filenames are written to localStorage/sessionStorage.

## UI/UX Checklist Result

- [x] Backend returns document states, extracted field evidence, review status, mismatch warnings, and approved-form payload in shapes the frontend can render directly.
- [x] Backend keeps house check data unchanged before frontend/user approval is applied through existing forms.
- [x] Frontend now renders the approved document-intake card layout, source chips/notes, overwrite comparison flow, retry/delete messaging, and browser-storage-safe session handling.

## Tests Run

```text
./gradlew test --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerIntegrationTest' --tests 'com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest'
- PASS

./gradlew test --tests 'com.nowayhome.housecheck.*'
- PASS

cd frontend && npm test
- PASS (13 tests)

cd frontend && npm run build
- PASS

Live smoke (2026-05-24, local backend + Vite)
- Started `docker compose up -d postgres`
- Started backend with `./gradlew bootRun`
- Started frontend with `cd frontend && npm run dev -- --host 127.0.0.1 --port 4173`
- Browser smoke with generated local fixtures:
  - Applied `User ID=smoke-owner`
  - Created document intake session
  - Uploaded temp `registry.pdf` to registry slot and temp `lease.png` to lease slot
  - Observed both documents move to `검토 필요`
  - Observed mismatch warnings (`보증금 확인 필요`, `임대인 / 소유자 확인 필요`)
  - Approved fields through backend API for the same session, refreshed the UI, and observed both documents move to `승인 완료`
  - Opened compare/apply summary, confirmed apply, and observed contract/registry form fields populated with `자동 입력 반영` notes
- Validation smoke:
  - `POST /api/document-intakes/{sessionId}/documents/registry` with PNG returned `400 DOCUMENT_INTAKE_INVALID_FILE_TYPE`
- Browser storage smoke:
  - `localStorage`: empty
  - `sessionStorage`: only `house-risk-agent-prompts.user-id` and `house-risk-agent-prompts.document-intake-session-id`
- Stopped frontend dev server, backend bootRun, Playwright browser session, and `docker compose stop postgres`
```

## Changed Files

- `src/main/kotlin/com/nowayhome/housecheck/api/DocumentIntakeController.kt`
- `src/main/kotlin/com/nowayhome/housecheck/api/HouseCheckExceptionHandler.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeAccessGuard.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeApplicationPayloadAssembler.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeCommandService.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeCommands.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeExtractionPort.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeFieldValueParser.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeFilePolicy.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeQueryService.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeResponses.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeSessionResponseAssembler.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeWarningPolicy.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/HouseCheckErrorCode.kt`
- `src/main/kotlin/com/nowayhome/housecheck/domain/DocumentIntakeEnums.kt`
- `src/main/kotlin/com/nowayhome/housecheck/persistence/DocumentIntakeEntities.kt`
- `src/main/kotlin/com/nowayhome/housecheck/persistence/DocumentIntakeRepositories.kt`
- `src/main/kotlin/com/nowayhome/housecheck/storage/DocumentIntakeDocumentStorage.kt`
- `src/main/resources/db/migration/V3__document_intake.sql`
- `src/test/kotlin/com/nowayhome/housecheck/api/DocumentIntakeControllerIntegrationTest.kt`
- `frontend/src/App.tsx`
- `frontend/src/api.ts`
- `frontend/src/documentIntake.ts`
- `frontend/src/documentIntake.test.ts`
- `frontend/src/format.ts`
- `frontend/src/styles.css`
- `frontend/src/types.ts`
- `frontend/src/validation.test.ts`
- `frontend/src/validation.ts`
- `doc/agent-work/house-document-auto-fill/03-developer-implementation.md`

## Risks Or Follow-ups

- The extraction adapter is intentionally fake. It returns deterministic canonical fields and fake mismatch hints for testability, but real OCR/provider integration still needs a production adapter behind the same port.
- Deposit mismatch is currently driven by fake extraction hints rather than a richer cross-source financial reconciliation model.
- The backend exposes typed application payload data but does not apply it into existing house-check entities automatically. Frontend still needs to drive the “compare and apply” UX.
- No document preview/download endpoint was added in this slice. The current backend scope supports storage, review evidence text, approval, payload fetch, and delete.
- Contract 기본 정보는 현재 backend에 update endpoint가 없어서 `checkId` 생성 후에는 화면 입력값 비교/보조용으로만 바뀐다. QA는 권장 흐름대로 `문서 세션 -> 승인 반영 -> 진단 시작` 순서를 우선 검증해야 한다.
- Live smoke는 generated temp fixtures와 fake extraction에 기반했다. 사용자가 제공한 루트의 실제 PDF 2개는 아직 manual QA 전용이며, UI/UX acceptance에서 별도 업로드 검증이 필요하다.
- PR review용 스크린샷 asset은 이번 요청의 허용 쓰기 경로가 `frontend/**`와 `03-developer-implementation.md`로 제한되어 추가하지 않았다.

## Handoff Notes

- UI/UX acceptance can now validate the end-to-end frontend flow on the existing workspace: `User ID 적용 -> 문서 세션 시작 -> 문서 업로드 -> 추출 검토 -> 승인한 필드 반영 -> 계약/등기 입력 확인`.
- Existing house-check APIs and tests still pass after this slice, and the browser-storage check remained within the allowed session-only keys.

## Rework Loop: UI_UX_ACCEPTANCE -> DEVELOPMENT

- UI/UX-01: 실패 문서 액션 문구를 실제 동작과 맞췄다. stored original 재처리 API가 없는 현재 backend contract에서는 `다시 처리` 대신 `파일 선택 후 다시 업로드`로 표시하고, 실패 메시지도 새 파일 선택 후 재업로드하라는 안내를 함께 보여준다.
- UI/UX-02: overwrite 비교에서 충돌 항목의 선택 상태를 명시적으로 표시했다. 체크 해제 상태는 `선택됨: 현재 값 유지`, 체크 상태는 `선택됨: 승인값으로 교체`로 보이고, 체크 규칙 설명을 함께 노출한다.

### UI/UX Rework Verification

```text
cd frontend && npm test
- PASS (3 files, 13 tests)

cd frontend && npm run build
- PASS
```

### UI/UX Rework Changed Files

- `frontend/src/App.tsx`
- `frontend/src/styles.css`
- `doc/agent-work/house-document-auto-fill/03-developer-implementation.md`

## Rework Loop: QA_REPORT -> DEVELOPMENT

- QA-01: 사용자 제공 실제 PDF 2종이 Spring multipart 기본 제한에 걸려 `HTTP 413`으로 거부되던 문제를 수정했다.
- Backend:
  - 문서 자동 입력 업로드 한도를 명시적인 20MB 정책으로 추가했다.
  - Spring multipart `max-file-size`와 `max-request-size`를 실제 PDF 수용 범위로 설정했다.
  - `MaxUploadSizeExceededException`을 JSON `413` 응답으로 변환하고 `DOCUMENT_INTAKE_FILE_TOO_LARGE` 코드를 추가했다.
  - 11,006,061 byte PDF 수용 테스트와 20MB 초과 JSON 413 테스트를 추가했다.
- Frontend:
  - 문서 자동 입력 업로드 사전 검증에 20MiB 제한을 추가했다.
  - 업로드 슬롯 helper copy에 지원 형식과 크기 제한을 표시했다.
  - 서버 `ApiError` 메시지와 bare `HTTP 413`을 사용자에게 명확한 파일 크기 안내로 표시하도록 했다.

### QA Rework Verification

```text
./gradlew test --tests 'com.nowayhome.housecheck.application.DocumentIntakeFilePolicyTest' --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerIntegrationTest' --tests 'com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest' --rerun-tasks
- PASS

cd frontend && npm test
- PASS (3 files, 17 tests)

cd frontend && npm run build
- PASS

Playwright browser QA with real local PDFs
- PASS
- Uploaded `/Users/jeongcool/me/no-way-home/6_부동산_등기사항_전부증명서.pdf`
- Uploaded `/Users/jeongcool/me/no-way-home/임대차계약서.pdf`
- Approved 20 extracted fields, applied approved values, and created house check `bd31b759-c0a1-4f45-b0ad-26297b8cac5d`

DB/filesystem evidence for document session `30f7e3a3-033a-491f-980a-334c739d2786`
- REGISTRY stored `11006061` bytes and reached `APPROVED`
- LEASE_CONTRACT stored `9679921` bytes and reached `APPROVED`
- Encrypted `.bin` files did not contain `%PDF`

./gradlew test
- PASS
```

### QA Rework Changed Files

- `src/main/kotlin/com/nowayhome/housecheck/api/HouseCheckExceptionHandler.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeFilePolicy.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/DocumentIntakeUploadSizePolicy.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/HouseCheckErrorCode.kt`
- `src/main/resources/application.yml`
- `src/test/kotlin/com/nowayhome/housecheck/api/DocumentIntakeControllerIntegrationTest.kt`
- `src/test/kotlin/com/nowayhome/housecheck/application/DocumentIntakeFilePolicyTest.kt`
- `frontend/src/App.tsx`
- `frontend/src/validation.test.ts`
- `frontend/src/validation.ts`
- `doc/agent-work/house-document-auto-fill/03-developer-implementation.md`
