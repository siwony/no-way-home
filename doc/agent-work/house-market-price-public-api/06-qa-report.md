# QA Report: 공공 실거래가 시세 자동 조회

Status: COMPLETED

Result: PASS

## Summary

공공 실거래가 조회는 mock XML public API로 backend E2E 성격의 Spring Boot 통합 테스트를 통과했다. 등록된 실제 service key를 사용한 QA도 수행했고, 국토교통부 아파트 매매 XML API는 정상 응답을 받아 lookup/save 흐름이 통과했다. 일부 보조 API 403은 서비스 중단이 아니라 경고와 fallback으로 처리하도록 보강했다.

## Tests Executed

```text
./gradlew test
> BUILD SUCCESSFUL

MARKET_PRICE_PROVIDER=mlit SERVER_PORT=18080 ./gradlew bootRun
POST /api/house-checks
POST /api/house-checks/{checkId}/market-price/lookup
POST /api/house-checks/{checkId}/market-price
> lookup confidence AVAILABLE
> sourceKind MLIT_REAL_TRANSACTION
> lawdCode 11440
> marketPriceStatus SAVED

실제 외부 API 직접 점검
> Juso XML: 200
> StanReginCd XML: 403
> AptTrade XML: 200
> AptRent XML: 403

cd frontend && npm test
> Test Files 3 passed (3)
> Tests 17 passed (17)

cd frontend && npm run build
> vite build completed successfully

npx -y -p playwright@latest node --input-type=module <market-price screenshot script>
> doc/agent-work/house-market-price-public-api/assets/market-price-lookup-preview.png 195635 bytes

git diff --check
> no output

./gradlew tasks --all | rg -i "ktlint" || true
> no ktlint task configured
```

## Passed Cases

- XML-only Juso lookup uses `resultType=xml`.
- XML-only legal region lookup uses `type=xml`.
- MLIT 실거래가 calls include XML Accept header and no JSON query parameter.
- Lookup response returns estimated market value, estimated jeonse value, lawd code, deal month range, source kind, and confidence.
- Selected lookup result saves source kind and metadata to `market_price_snapshot`.
- 기존 수동 시세 validation and save flow remains compatible.
- Frontend lookup preview, apply, and save flow renders and produces PR screenshot evidence.
- 실제 service key 환경에서 `서울특별시 마포구 양화로 1` 도로명 주소와 `서울특별시 마포구 합정동 472` 지번 fallback으로 lookup 성공.
- 실제 국토교통부 아파트 매매 XML 응답으로 `estimatedMarketValue=1220000000`, `marketSampleCount=2116`, `confidence=AVAILABLE` 확인.
- 실제 lookup 결과를 저장해 `marketPriceStatus=SAVED` 확인.
- 실제 StanReginCd/AptRent 403 응답은 경고로 반환되고 매매 시세 조회 성공을 막지 않음.

## Failed Cases

- 없음

## Defects

| ID | Severity | Scenario | Expected | Actual | Repro Steps |
|---|---|---|---|---|---|
|  |  |  |  |  |  |

## Result Notes

Result value: `PASS`.
