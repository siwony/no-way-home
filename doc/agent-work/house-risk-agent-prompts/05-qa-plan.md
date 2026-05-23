# QA Plan: 주택 계약 위험도 진단 서비스

Status: COMPLETE

## Rerun Scope

- Backend rework 이후 QA-01, QA-02가 실제로 해소됐는지 재검증한다.
- 최소 기준으로 전체 `./gradlew test`를 다시 실행하고, 전세가-only 입력과 민감정보 암호화 저장 경계를 집중 확인한다.
- `doc/feat/house-risk-agent-prompts/tests.md` 기준에서 이번 재검증이 커버한 범위와 남은 비차단 갭을 분리 기록한다.

## Executed Checklist

- [x] UI/UX acceptance가 `APPROVED` 상태인지 확인했다.
- [x] `03-developer-implementation.md`의 수정 범위와 주장된 테스트 결과를 대조했다.
- [x] 전체 회귀로 `./gradlew test`를 실행했다.
- [x] fresh evidence 확보를 위해 `./gradlew test --rerun-tasks`를 실행했다.
- [x] `HouseCheckControllerIntegrationTest` rerun으로 `estimatedJeonseValue` 단독 저장 -> 분석 -> 리포트 경로를 재검증했다.
- [x] 같은 통합 테스트에서 `house_check_request.landlord_name` raw column이 평문과 다름을 확인했다.
- [x] 같은 통합 테스트에서 업로드된 registry 파일 저장 바이트에 원본 PDF 평문과 `%PDF-1.4` 헤더가 직접 남지 않음을 확인했다.
- [x] `HouseCheckCryptoServiceTest`로 문자열/바이너리 암복호화 round-trip과 평문 비노출을 확인했다.
- [x] `HouseRiskAnalysisComponentsTest`로 jeonse-only snapshot이 `MARKET_PRICE_REQUIRED`를 만들지 않는 단위 경계를 확인했다.
- [x] 결과를 기준으로 QA PASS/FAIL를 다시 판정하고 state를 갱신한다.

## Evidence Targets

- jeonse-only 저장 입력은 승인된 계약대로 저장된 사용자 입력으로 남아야 한다.
- jeonse-only일 때 `depositRisk.estimatedJeonseValue`는 응답에 노출돼야 한다.
- jeonse-only일 때 `MARKET_PRICE_REQUIRED` 대신 매매 시세 의존 계산 제한 사유만 남아야 한다.
- 임대인 이름은 JPA 저장 컬럼에서 평문으로 직접 읽히면 안 된다.
- 업로드 문서 바이트는 파일시스템 저장 시 원본 평문 payload와 직접 일치하면 안 된다.

## Coverage Notes

- 이번 rerun에서 직접 커버:
- 생성, 업로드, 수동 입력, 시세 저장, 분석, 리포트/체크리스트 조회 기본 흐름
- 음수 보증금 검증
- 타 사용자 체크리스트 접근 차단
- 전세가-only 분석 경계
- landlord name / document bytes 암호화 저장 경계

- 이번 rerun에서 재실행하지 않은 비차단 항목:
- 비-PDF 업로드 거절
- 날짜 형식 오류
- 업로드/수동 입력/분석 실행에 대한 전체 권한 차단 조합
- 로그 마스킹
- fake `MarketPriceProvider` 교체 가능성 통합 검증
