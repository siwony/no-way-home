# Implementation Notes

## Stack

Kotlin + Spring Boot 백엔드와 MariaDB를 기준으로 구현한다. 코딩 스타일과 책임 분리는 `doc/kotlin-coding-convention.md`를 따른다.

## Database

필수 테이블:

- `house_check_request`: 사용자의 진단 요청
- `registry_snapshot`: 등기부등본 조회/업로드 결과
- `registry_right`: 갑구/을구 권리 항목
- `building_ledger_snapshot`: 건축물대장 조회/업로드 결과
- `market_price_snapshot`: 시세/실거래가 정보
- `house_risk_report`: 최종 위험도 분석 결과
- `house_risk_reason`: 위험 사유 상세

각 테이블은 주요 컬럼 설명, 인덱스, JSON 컬럼 사용 여부, 암호화 대상을 명확히 한다. 권리 항목은 말소 여부와 접수일자를 저장한다. 시세 정보는 출처와 기준일을 저장한다.

## Domain Model

필수 enum:

- `ContractType`
- `HousingType`
- `RiskLevel`
- `RegistrySection`
- `RegistryRightType`
- `BuildingUsageRiskType`

필수 data class:

- `HouseCheckRequest`
- `RegistrySnapshot`
- `RegistryRight`
- `BuildingLedgerSnapshot`
- `MarketPriceSnapshot`
- `HouseRiskReport`
- `HouseRiskReason`
- `RiskAnalysisInput`
- `RiskAnalysisResult`

금액은 `Long`, 날짜/시간은 `LocalDate`와 `LocalDateTime`을 사용한다. enum은 DB 저장값과 API 응답값을 함께 고려한다. 위험 사유는 코드와 사용자 메시지를 분리한다.

## Service Design

필수 클래스:

- `HouseRiskAnalysisService`
- `RegistryRiskAnalyzer`
- `BuildingRiskAnalyzer`
- `MarketPriceRiskAnalyzer`
- `RecoverySimulationService`

단순 if문 나열을 피하고 항목별 analyzer로 분리한다. 각 analyzer는 `HouseRiskReason` 목록을 반환한다. `HouseRiskAnalysisService`는 사유를 병합하고 `CRITICAL > DANGER > CAUTION > SAFE` 우선순위로 최종 등급을 결정한다.

시세 정보가 없으면 계산 불가 사유를 `CAUTION`으로 추가한다. 등기부등본 정보가 없으면 핵심 판정 불가 사유를 `DANGER` 또는 `CAUTION`으로 추가한다. 보수적 회수 시뮬레이션은 별도 서비스로 분리한다.

## MVP Phases

| Phase | 구현 기능 | 완료 기준 |
|---|---|---|
| 1 | 수동 입력 기반 진단, 등기부등본 PDF 업로드, 사람이 입력한 권리 정보 기반 계산 | 간단 리포트 생성 가능 |
| 2 | 등기부등본 OCR 또는 파싱, 권리 항목 자동 추출, 갑구/을구 분류 | 등기 위험 사유 자동 생성 |
| 3 | 건축물대장 업로드/파싱, 위반건축물 여부와 용도 분석 | 건축물 위험 사유 자동 생성 |
| 4 | 시세/실거래가 연동, 전세가율과 총 위험 노출 비율 자동 계산 | 보증금 위험 분석 가능 |
| 5 | 등기/건축물/시세 자동 조회, 리포트 고도화, 계약 직전 재확인 알림 | 외부 연동 기반 리포트 제공 |

## Warning Messages

위험 문구는 법적 단정이 아니라 위험 안내 형태로 작성한다.

나쁜 예:

```text
이 집은 위험합니다.
```

좋은 예:

```text
현재 확인된 자료 기준으로 보증금 회수 위험이 높을 수 있습니다.
```
