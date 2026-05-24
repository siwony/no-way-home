# QA Plan: 공공 실거래가 시세 자동 조회

Status: READY

## Test Scope

- 공공 실거래가 조회 backend API, XML-only 외부 호출 adapter, 저장 metadata persistence
- 기존 수동 시세 저장과 분석 리포트의 회귀
- frontend 시세 입력 카드의 조회, preview, 적용, 명시적 저장 흐름
- PR 증거 스크린샷 생성

## User Use Cases

- [ ] 사용자가 계약 기본 정보를 저장한 뒤 `공공 실거래가 조회`를 실행한다.
- [ ] 조회 결과는 preview로 표시되고, 적용 전에는 입력값을 덮어쓰지 않는다.
- [ ] `조회 결과 적용` 후 시세 입력 field와 출처 metadata가 채워진다.
- [ ] 사용자가 `시세 저장`을 눌러야 최종 저장된다.

## Exception Cases

- [ ] provider가 비활성화되면 수동 입력 fallback이 유지된다.
- [ ] 주소를 법정동 코드로 변환하지 못하면 적용 불가 preview와 경고를 반환한다.
- [ ] 매매 또는 전세 표본이 부족하면 기준값을 확정하지 않고 경고를 반환한다.
- [ ] 공공 API 호출 실패는 domain error로 변환된다.

## Permission And Security Cases

- [ ] `X-User-Id` owner guard가 lookup endpoint에도 적용된다.
- [ ] 외부 API는 JSON parameter 없이 XML 응답만 요청한다.
- [ ] XML parser는 DOCTYPE, 외부 entity, 외부 DTD/schema 접근을 비활성화한다.
- [ ] API key는 환경변수로만 설정하고 repository에 secret을 저장하지 않는다.

## Regression Risks

- [ ] 기존 `/api/house-checks/{checkId}/market-price` 수동 저장 payload 호환성
- [ ] 분석 실행 시 저장되지 않은 provider 결과를 암묵적으로 사용하지 않는지 여부
- [ ] report value source가 수동 입력과 계산값을 올바르게 구분하는지 여부
- [ ] frontend validation/test/build 회귀

## Test Data

- mock Juso XML: 서울특별시 마포구 양화로 1 테스트아파트
- mock legal region XML: `11440`
- mock MLIT 매매 XML: 2억, 2.2억, 2.4억
- mock MLIT 전세 XML: 1억, 1.2억, 1.4억
- frontend screenshot mock response: 위와 동일한 lookup 결과

## Automation Plan

- `./gradlew test`
- `cd frontend && npm test`
- `cd frontend && npm run build`
- Playwright mock route로 시세 조회 preview와 적용 결과 screenshot 저장
