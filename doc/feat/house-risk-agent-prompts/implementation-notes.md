# Implementation Notes

## Stack Assumption

기술 스택과 로컬 실행 환경은 `doc/init/tech-stack.md`를 따른다. 이 문서는 주택 계약 위험도 진단 기능의 도메인, DB, 서비스, API 구현 메모만 다룬다.

## Database

필수 테이블:

- `house_check_request`: 사용자의 진단 요청
- `registry_snapshot`: 등기부등본 조회/업로드 결과
- `registry_right`: 갑구/을구 권리 항목
- `building_ledger_snapshot`: 건축물대장 조회/업로드 결과
- `market_price_snapshot`: 시세/실거래가 정보
- `house_risk_report`: 최종 위험도 분석 결과
- `house_risk_reason`: 위험 사유 상세

각 테이블은 주요 컬럼 설명, 인덱스, JSONB 컬럼 사용 여부, 암호화 대상을 명확히 한다. 권리 항목은 말소 여부와 접수일자를 저장한다. 시세 정보는 출처와 기준일을 저장한다.

PostgreSQL 기준 설계 원칙:

- 기본 키는 `uuid` 또는 `bigserial` 중 하나로 통일한다.
- enum 저장은 문자열 값을 우선한다.
- 반정형 분석 원문은 필요한 경우 `jsonb`로 저장한다.
- 조회 조건이 되는 `request_id`, `risk_level`, `created_at`, `issued_at`에는 인덱스를 둔다.
- schema migration은 초기 스택에서 선택한 도구를 사용한다.
- 운영 데이터 암호화는 애플리케이션 레벨 암호화와 DB 권한 분리를 함께 고려한다.

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

## External Skill Integration

`https://github.com/NomaDamas/k-skill`의 skill은 직접 도메인 로직에 섞지 말고 포트/어댑터 뒤에 둔다. 외부 호출 실패, 설치 누락, 네트워크 차단 시에도 핵심 수동 입력 기반 진단이 동작해야 한다.

권장 포트:

- `MarketPriceProvider`: 국토교통부 실거래가/전월세 데이터를 조회한다. `real-estate-search` skill 또는 동일 HTTP 계약의 어댑터로 구현한다.
- `RegistryDocumentAcquisitionGuide`: 등기부등본 발급 보조 안내를 제공한다. `iros-registry-automation` skill의 보안 경계를 반영하되, 서버가 인터넷등기소 로그인/인증/결제를 대행하지 않는다.
- `ReferenceListingProvider`: 공개 매물/호가 같은 보조 자료가 필요할 때만 둔다. 위험도 계산 필수 경로가 아니다.

`MarketPriceProvider` 구현 메모:

- `region-code` 조회로 법정동 코드를 확인한 뒤 주택 유형과 거래 유형에 맞는 endpoint를 호출한다.
- `APT`는 `apartment`, `OFFICETEL`은 `officetel`, `VILLA`와 `MULTI_HOUSEHOLD`는 `villa`, `MULTI_FAMILY`는 `single-house`를 기본 매핑으로 검토한다. `UNKNOWN`은 자동 조회하지 않고 사용자 확인 필요 사유를 남긴다.
- 매매 추정에는 `trade`, 전세/월세 비교에는 `rent` 데이터를 사용한다.
- 저장 시 `source`, `source_url`, `lawd_cd`, `deal_ymd`, `asset_type`, `deal_type`, `sample_count`, `median_price_10k` 또는 `median_deposit_10k`를 보존한다.
- 실거래 표본이 부족하면 추정 시세를 단정하지 말고 계산 불가 또는 낮은 신뢰도 사유를 추가한다.

`iros-registry-automation` 적용 메모:

- 사용자가 직접 로그인, 인증, 결제한 뒤 발급한 등기부등본 PDF 업로드를 기본 UX로 둔다.
- 장바구니 담기나 파일 저장 보조를 문서화할 수는 있지만, 애플리케이션 서버가 사용자의 인터넷등기소 인증정보, 공동인증서 비밀번호, 카드정보를 수집하지 않는다.
- 발급 대상 주소 목록, PDF, 로그, Excel 산출물은 민감정보로 보고 저장소와 테스트 fixture에 넣지 않는다.

사용하지 말아야 할 경로:

- `gongsijiga-search`의 개별공시지가를 시세나 매매가로 간주하지 않는다.
- `daangn-realty-search`의 공개 매물/호가를 국토교통부 실거래가와 같은 신뢰도로 계산하지 않는다.

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
