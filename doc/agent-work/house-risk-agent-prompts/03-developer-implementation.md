# Developer Implementation: 주택 계약 위험도 진단 서비스

Status: READY_FOR_UI_UX_ACCEPTANCE

## Prerequisites

- Director planning approval is `APPROVED`: [x] yes / [ ] no

## Backend Work

- [x] Fixed QA-01 by preserving the approved market-price request contract. `estimatedJeonseValue`-only input is now treated as valid saved user input instead of being ignored in analysis.
- [x] Added conservative jeonse-only analysis semantics. When only `estimatedJeonseValue` exists, the report keeps the saved input visible and avoids `MARKET_PRICE_REQUIRED`, but market-value-dependent ratios and recovery simulation remain `NOT_AVAILABLE` with an explicit explanation.
- [x] Added `depositRisk.estimatedJeonseValue` to the report response so the saved jeonse-only input is visible end-to-end with `USER_ENTERED` source typing.
- [x] Fixed QA-02 by adding a practical Phase 1 encryption-at-rest boundary for `house_check_request.landlord_name` via a JPA attribute converter backed by local AES-GCM application crypto.
- [x] Switched filesystem document storage to encrypted bytes-at-rest before writing uploaded PDF payloads to disk.
- [x] Kept the encryption implementation local and deterministic for tests through an application-configured secret default plus a fixed test secret override. No external secret manager or network dependency was introduced.
- [x] Added automated coverage for the jeonse-only path and the sensitive-data storage boundary.

## Frontend Work

- [x] N/A for backend developer scope.

## UI/UX Checklist Result

- [x] No UI/UX plan mismatch was introduced in this rework loop.
- [x] The approved request contract remains valid: `POST /api/house-checks/{checkId}/market-price` accepts `estimatedMarketValue` or `estimatedJeonseValue`.
- [x] Analysis still runs with partial inputs and now distinguishes `saved jeonse-only input` from `missing market input`.
- [x] Sensitive fields required by the Director brief now have an application-level at-rest protection boundary.

## Tests Run

```text
./gradlew test --tests com.nowayhome.housecheck.domain.analysis.HouseRiskAnalysisComponentsTest --tests com.nowayhome.housecheck.security.HouseCheckCryptoServiceTest --tests com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest
- SUCCESS
- 11 tests completed, 0 failed
- Verified:
  - jeonse-only saved input does not produce MARKET_PRICE_REQUIRED
  - report exposes estimatedJeonseValue with USER_ENTERED source typing
  - landlord_name DB column value is not stored as plaintext
  - uploaded document bytes on disk do not contain the original PDF plaintext payload

./gradlew test
- SUCCESS
- 11 tests completed, 0 failed
```

## Changed Files

- `src/main/kotlin/com/nowayhome/housecheck/application/HouseCheckQueryService.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/HouseCheckResponses.kt`
- `src/main/kotlin/com/nowayhome/housecheck/application/HouseRiskAnalysisService.kt`
- `src/main/kotlin/com/nowayhome/housecheck/domain/analysis/HouseRiskAnalysisModels.kt`
- `src/main/kotlin/com/nowayhome/housecheck/domain/analysis/MarketPriceAssessmentService.kt`
- `src/main/kotlin/com/nowayhome/housecheck/domain/analysis/MarketPriceRiskAnalyzer.kt`
- `src/main/kotlin/com/nowayhome/housecheck/persistence/HouseCheckEntities.kt`
- `src/main/kotlin/com/nowayhome/housecheck/security/HouseCheckCryptoService.kt`
- `src/main/kotlin/com/nowayhome/housecheck/storage/HouseCheckDocumentStorage.kt`
- `src/main/resources/application.yml`
- `src/test/kotlin/com/nowayhome/housecheck/api/HouseCheckControllerIntegrationTest.kt`
- `src/test/kotlin/com/nowayhome/housecheck/domain/analysis/HouseRiskAnalysisComponentsTest.kt`
- `src/test/kotlin/com/nowayhome/housecheck/security/HouseCheckCryptoServiceTest.kt`
- `doc/agent-work/house-risk-agent-prompts/03-developer-implementation.md`
- `doc/agent-work/house-risk-agent-prompts/state.md`

## Risks Or Follow-ups

- The default encryption secret in `application.yml` is intentionally local-only. Non-local environments should override `HOUSECHECK_ENCRYPTION_SECRET` with an environment-specific secret before handling real sensitive data.
- This loop adds encryption for newly written landlord names and newly stored uploaded files. It does not backfill any plaintext rows or plaintext files that may already exist from earlier local runs.
- Phase 1 still has no document download/read API. The encryption boundary covers storage-at-rest for uploads in this slice, but a future retrieval API should explicitly decrypt through the same crypto service instead of reading bytes directly.

## Handoff Notes

- Backend rework for QA-01 and QA-02 is complete and verified.
- Work state can advance to `UI_UX_ACCEPTANCE`.
