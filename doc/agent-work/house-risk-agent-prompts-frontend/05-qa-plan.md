# QA Plan: 주택 계약 위험도 진단 프론트엔드

Status: COMPLETE

## Test Scope

- QA-01 재작업 범위 소스 리뷰: `frontend/src/App.tsx`, `frontend/src/validation.ts`, `frontend/src/validation.test.ts`
- Required frontend quality gates: `cd frontend && npm test`, `cd frontend && npm run build`
- Built frontend browser smoke for restored-session banner, result reload, `ACCESS_DENIED`, and recovery actions

## User Use Cases

- [x] 저장된 `User ID`와 `checkId`로 재진입 시 neutral session banner가 맞게 보이는지 확인
- [x] 저장된 `checkId` 기준 결과 다시 불러오기 후 기존 결과 렌더링이 되는지 확인
- [x] 다른 `User ID`를 같은 `checkId`에 적용하면 boundary reset 경고와 서버 데이터 제거가 즉시 적용되는지 확인
- [x] `ACCESS_DENIED` 발생 후 denial panel 안에서 `User ID 다시 적용`과 `새 진단 시작` 회복 액션이 계속 동작하는지 확인

## Exception Cases

- [x] restored-session banner 문구 helper가 빈 세션, `User ID`만 있는 세션, `User ID + checkId` 세션을 모두 구분하는지 확인
- [x] `ACCESS_DENIED` 발생 시 서버 유래 데이터 제거와 전용 패널 확인
- [x] 회복 재적용 후 denial panel이 남지 않고 다시 일반 상태로 복귀하는지 확인

## Permission And Security Cases

- [x] 보고서/체크리스트 재조회 요청이 `X-User-Id` 경계에 따라 `200` 또는 `403 ACCESS_DENIED`로 분기되는지 확인
- [x] 회복 검증 중에도 브라우저 저장소 키 범위가 `User ID`, `checkId`에 머무는지 확인

## Regression Risks

- QA-01 수정이 초기 배너 문구만 고치고 기존 `ACCESS_DENIED` recovery 흐름을 깨뜨리지 않았는지 확인
- 저장소 기반 session resume와 경계 재적용 로직이 서로 다른 global message를 남기지 않는지 확인

## Test Data

- 프론트엔드 preview: `cd frontend && npm run preview -- --host 127.0.0.1 --port 4174`
- 저장 세션: `house-risk-agent-prompts.user-id=owner-a`, `house-risk-agent-prompts.check-id=check-1`
- Playwright mock 응답: `owner-a`는 report/checklist `200`, `owner-b`는 동일 `checkId`에 `403 ACCESS_DENIED`

## Automation Plan

- 추가 QA 전용 테스트 파일은 만들지 않는다.
- 기존 Vitest 검증과 생산 빌드 결과를 기록한다.
- Playwright 기반 브라우저 smoke로 restored-session banner와 access-denied recovery를 검증한다.
