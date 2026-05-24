# Developer Implementation: 공공 실거래가 시세 자동 조회

Status: COMPLETED

## Prerequisites

- Director planning approval is `APPROVED`: [x] yes / [ ] no

## Backend Work

- [x] `POST /api/house-checks/{checkId}/market-price/lookup` endpoint added with owner guard.
- [x] `MarketPriceProvider` expanded to return sale, jeonse, sample, lawd, query month, source, and warning metadata.
- [x] MLIT/Juso public API integration implemented with XML-only request parameters and XML Accept header.
- [x] Safe XML parser added with DOCTYPE, external entity, DTD, and schema access disabled.
- [x] Market price metadata persisted with Flyway migration `V4__market_price_public_api_metadata.sql`.
- [x] Analysis now uses only explicitly saved market price snapshots, preserving preview/apply/save-only semantics.
- [x] Provider, API keys, endpoint URLs, timeout, history months, and min sample count are environment-configurable.

## Frontend Work

- [x] 시세 입력 카드에 공공 실거래가 조회, preview, 경고, 적용 흐름을 추가했다.
- [x] 조회 결과 적용 전에는 기존 수동 입력값을 덮어쓰지 않도록 분리했다.
- [x] 수동 편집 시 public source metadata를 `USER_ENTERED`로 초기화한다.
- [x] 저장 payload에 public source metadata를 포함한다.

## UI/UX Checklist Result

- [x] 조회/preview/적용/명시적 저장 흐름 확인
- [x] provider 비활성 또는 적용 불가 결과가 수동 입력 fallback을 막지 않음
- [x] PR evidence screenshot generated: `doc/agent-work/house-market-price-public-api/assets/market-price-lookup-preview.png`

## Tests Run

```text
./gradlew test
cd frontend && npm test
cd frontend && npm run build
npx -y -p playwright@latest node --input-type=module <market-price screenshot script>
git diff --check
./gradlew tasks --all | rg -i "ktlint" || true
```

## Changed Files

- Backend: controller, command/query services, market price provider/config/XML clients/parser, persistence entity, Flyway migration, application config
- Frontend: `App.tsx`, `api.ts`, `types.ts`, validation test
- Tests: XML-only market price integration test, market price analysis component test update
- Harness: QA plan/report/final review/PR lifecycle, PR screenshot asset

## Risks Or Follow-ups

- Actual public API behavior can differ by endpoint field names and regional data availability; mock XML tests cover the contract implemented here.
- `MLIT_REAL_TRANSACTION` metadata is persisted from the selected lookup result payload. A future hardening pass can add server-side lookup result tokens if source attestation becomes required.

## Handoff Notes

- QA result is PASS and Director final review can proceed.
