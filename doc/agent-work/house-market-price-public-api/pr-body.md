## 개요

- Work ID: `house-market-price-public-api`
- 기능명: 공공 실거래가 시세 자동 조회
- 관련 문서:
  - `doc/agent-work/house-market-price-public-api/00-director-brief.md`
  - `doc/agent-work/house-market-price-public-api/01-ui-ux-plan.md`
  - `doc/agent-work/house-market-price-public-api/08-pr-lifecycle.md`

## 작업 내용

- [x] Director brief 작성
- [x] UI/UX plan 작성 및 Director approval 완료
- [x] Draft PR 생성
- [x] Backend 구현
- [x] Frontend 구현
- [x] UI/UX acceptance 완료
- [x] QA plan/report 완료
- [x] Director final review 완료

### Backend

- `POST /api/house-checks/{checkId}/market-price/lookup` 추가
- Juso 주소검색, 법정동 코드, 국토교통부 실거래가를 XML-only로 호출
- 매매/순수 전세 표본 중앙값, 표본 수, 법정동 코드, 조회 월 범위, 경고 반환
- 선택한 조회 결과를 저장할 수 있도록 시세 snapshot metadata 확장
- 분석은 저장된 시세 snapshot만 사용하도록 유지
- 실제 public API 부분 실패 시 가능한 표본은 유지하고 실패 endpoint는 warning으로 반환

### Frontend

- 시세 입력 카드에 공공 실거래가 조회, preview, 적용 버튼 추가
- 조회 결과는 적용 전 기존 수동 입력값을 덮어쓰지 않음
- 적용 후에도 기존 `시세 저장`으로만 최종 저장
- 수동 편집 시 source metadata를 `USER_ENTERED`로 초기화

## 리뷰 필요

- 모든 공공 API 호출이 XML-only인지 확인
- KB시세 또는 비공식 scraping이 들어가지 않았는지 확인
- 조회 결과가 자동 저장/자동 덮어쓰기 되지 않는지 확인
- provider 비활성화, API key 누락, 표본 부족 때 수동 입력 fallback이 유지되는지 확인
- 테스트 결과와 잔여 risk 확인

## 테스트

```text
./gradlew test
BUILD SUCCESSFUL

cd frontend && npm test
Test Files 3 passed (3)
Tests 17 passed (17)

cd frontend && npm run build
vite build completed successfully

npx -y -p playwright@latest node --input-type=module <market-price screenshot script>
doc/agent-work/house-market-price-public-api/assets/market-price-lookup-preview.png 195635 bytes

git diff --check
no output

MARKET_PRICE_PROVIDER=mlit SERVER_PORT=18080 ./gradlew bootRun
POST /api/house-checks
POST /api/house-checks/{checkId}/market-price/lookup
POST /api/house-checks/{checkId}/market-price
lookup confidence AVAILABLE
sourceKind MLIT_REAL_TRANSACTION
lawdCode 11440
estimatedMarketValue 1220000000
estimatedJeonseValue 590000000
marketSampleCount 2116
jeonseSampleCount 6158
warningCount 0
marketPriceStatus SAVED

실제 외부 API 직접 점검
Juso XML 200
StanReginCd XML 200
AptTrade XML 200
AptRent XML 200
```

## 스크린샷 (필요한 경우)

![공공 실거래가 조회 preview](https://github.com/siwony/no-way-home/blob/feat/house-market-price-public-api/doc/agent-work/house-market-price-public-api/assets/market-price-lookup-preview.png?raw=1)
