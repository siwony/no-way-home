# Developer Implementation: 등기부등본·임대차 계약서 자동 입력

Status: BACKEND_READY_FOR_FRONTEND

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

- [ ] Not owned in this backend pass.

## UI/UX Checklist Result

- [x] Backend returns document states, extracted field evidence, review status, mismatch warnings, and approved-form payload in shapes the frontend can render directly.
- [x] Backend keeps house check data unchanged before frontend/user approval is applied through existing forms.
- [ ] Frontend-only items remain for card layout, source chips, overwrite comparison UI, retry messaging, and browser storage verification.

## Tests Run

```text
./gradlew test --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerIntegrationTest' --tests 'com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest'
- PASS

./gradlew test --tests 'com.nowayhome.housecheck.*'
- PASS
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

## Risks Or Follow-ups

- The extraction adapter is intentionally fake. It returns deterministic canonical fields and fake mismatch hints for testability, but real OCR/provider integration still needs a production adapter behind the same port.
- Deposit mismatch is currently driven by fake extraction hints rather than a richer cross-source financial reconciliation model.
- The backend exposes typed application payload data but does not apply it into existing house-check entities automatically. Frontend still needs to drive the “compare and apply” UX.
- No document preview/download endpoint was added in this slice. The current backend scope supports storage, review evidence text, approval, payload fetch, and delete.

## Handoff Notes

- Frontend can start from `POST /api/document-intakes`, upload registry and lease files to the document-specific endpoints, render the returned field/warning snapshot, call field review updates, then fetch `GET /api/document-intakes/{sessionId}/application-payload` to prefill the existing forms.
- Existing house-check APIs and tests still pass after this slice.
