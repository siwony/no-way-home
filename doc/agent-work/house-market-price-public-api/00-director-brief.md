# Director Brief: 공공 실거래가 시세 자동 조회

Status: READY_FOR_UI_UX

## Goal

계약 기본 정보의 주소와 주택 유형을 기준으로 공공 실거래가 API를 XML-only로 조회해, 사용자가 검토 후 시세 입력에 적용할 수 있는 후보값을 제공한다. 기존 수동 시세 입력과 분석 흐름은 유지한다.

## Background

현재 서비스는 `MarketPriceProvider` 포트와 수동 `POST /api/house-checks/{checkId}/market-price` 저장 흐름을 갖고 있다. 이번 작업은 KB시세가 아니라 공공데이터 기반 실거래가 조회를 실제 서비스용 provider로 연결하는 단계다.

공공 API는 반드시 XML로 호출한다. JSON, JSONP, 브라우저 직접 호출, frontend key 노출은 금지한다.

## Scope

### Include

- `POST /api/house-checks/{checkId}/market-price/lookup` 조회 endpoint 추가
- 도로명주소 검색 API XML 호출로 주소/건물명/행정구역 코드 해석
- 행정안전부 법정동코드 API XML 호출로 실거래가용 시군구 코드 산출
- 국토교통부 아파트/오피스텔/연립다세대/단독다가구 매매 및 전월세 실거래가 XML provider 구현
- 최근 N개월 표본의 중앙값 기반 후보 시세 산출
- frontend 시세 입력 카드에 조회, preview, 적용 흐름 추가
- mock XML fixture 기반 backend/frontend/e2e 테스트와 PR 증거 스크린샷

### Exclude

- KB시세 연동, KB부동산 scraping, 비공식 API 호출
- 조회 결과 자동 저장 또는 기존 수동 입력 자동 덮어쓰기
- 공시지가를 시세/매매가로 사용하는 계산
- frontend에서 공공 API key를 보관하거나 직접 호출하는 구현

## Required Documents

- `doc/feat/house-risk-agent-prompts/*`
- `doc/agent-harness/README.md`
- `doc/agent-harness/workflow.md`
- `doc/agent-harness/codex-subagents.md`

## Acceptance Criteria

- 공공 API 호출은 모두 XML 요청/응답 경로만 사용한다.
- API key가 없거나 provider가 꺼져 있으면 lookup은 명확한 실패 메시지를 반환하고 수동 입력은 계속 가능하다.
- 표본이 부족하거나 주소 매칭이 불명확하면 추정값을 확정하지 않고 warning과 reason을 반환한다.
- 사용자가 `적용`을 누르기 전에는 기존 시세 입력값을 바꾸지 않는다.
- 적용 후 기존 저장 endpoint로 `MLIT_REAL_TRANSACTION` 출처를 보존해 저장할 수 있다.
- 분석 리포트는 저장된 공공 실거래가 시세를 사용해 전세가율과 총 위험 노출 비율을 계산한다.
- QA는 mock XML로 E2E를 통과시키고 PR 본문에 화면 증거를 남긴다.

## Constraints

- 외부 공공 API는 서버에서만 호출한다.
- XML parser는 XXE, external DTD/entity, XInclude를 비활성화한다.
- CI/test는 live data.go.kr 호출에 의존하지 않는다.
- 수동 시세 입력 회귀를 만들지 않는다.
- 기존 문서 자동 입력 기능과 PR #2의 동작을 되돌리지 않는다.

## Open Questions

- 없음. KB는 공식 제휴 스펙 확보 전까지 범위 밖으로 고정한다.
