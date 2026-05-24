# QA Report: 공공 실거래가 시세 자동 조회

Status: COMPLETED

Result: PASS

## Summary

공공 실거래가 조회는 mock XML public API로 backend E2E 성격의 Spring Boot 통합 테스트를 통과했다. Frontend 단위 테스트, production build, Playwright screenshot flow, diff whitespace check도 통과했다. ktlint task는 Gradle task 목록에 없어서 실행 대상이 아니었다.

## Tests Executed

```text
./gradlew test
> BUILD SUCCESSFUL

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

## Failed Cases

- 없음

## Defects

| ID | Severity | Scenario | Expected | Actual | Repro Steps |
|---|---|---|---|---|---|
|  |  |  |  |  |  |

## Result Notes

Result value: `PASS`.
