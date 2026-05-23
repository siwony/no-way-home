# QA Plan: 주택 계약 위험도 진단 프론트엔드

Status: COMPLETE

## Test Scope

- Frontend source review against the approved UI/UX checklist in `frontend/src/App.tsx`, `frontend/src/api.ts`, `frontend/src/validation.ts`, and `frontend/src/validation.test.ts`
- Required frontend quality gates: `cd frontend && npm test`, `cd frontend && npm run build`
- Live source-backend API verification for create, two PDF uploads, two manual findings saves, market-price save, analyze, report, checklist, `ACCESS_DENIED`, and `ANALYSIS_NOT_READY`
- Partial browser smoke on the source frontend for `User ID` gating, create-form validation, not-ready messaging, report/checklist rendering, access-denied recovery actions, and browser storage limits

## User Use Cases

- [x] `User ID` 적용 전 주요 액션 비활성화 확인
- [x] 계약 기본 정보 생성 경로 확인
- [x] 등기 PDF 업로드 경로 확인
- [x] 건축물대장 PDF 업로드 경로 확인
- [x] 등기 수기 확인 저장 경로 확인
- [x] 건축물대장 수기 확인 저장 경로 확인
- [x] `estimatedJeonseValue` 단독 시세 저장 경로 확인
- [x] 분석 실행 후 리포트/체크리스트 조회 경로 확인
- [x] 다른 `User ID`로 같은 `checkId` 접근 시 회복 액션 포함 접근 거부 흐름 확인

## Exception Cases

- [x] 생성 폼 필수값 누락 클라이언트 검증 확인
- [x] 시세 입력 둘 다 비어 있을 때 검증 확인
- [x] `ANALYSIS_NOT_READY` 분리 상태 확인
- [x] `ACCESS_DENIED` 발생 시 서버 유래 데이터 제거와 전용 패널 확인
- [x] 세션 보존(`User ID`, `checkId`) 상태에서 초기 안내 문구 일관성 확인

## Permission And Security Cases

- [x] 모든 API 요청이 `X-User-Id` 경계를 기준으로 동작하는지 확인
- [x] 다른 `User ID`에서 리포트 접근 시 `403 ACCESS_DENIED` 확인
- [x] 브라우저 영구 저장소가 `sessionStorage`의 `User ID`, `checkId`로 제한되는지 확인
- [x] 임대인명, 파일명, 리포트 payload가 `localStorage`/`sessionStorage`에 남지 않는지 확인

## Regression Risks

- 결과 재조회가 `report`와 `checklist`를 함께 가져오는 구조라서 권한/준비중 오류 시 UI 분기가 깨지지 않는지 확인
- `ACCESS_DENIED` 패널 rework 이후 `User ID 다시 적용`, `새 진단 시작`이 기존 경계 초기화 로직과 충돌하지 않는지 확인
- 세션 보존 convenience가 추가된 상태라 새로고침/재진입 시 배너, 액션 활성화, 저장소 값이 서로 모순되지 않는지 확인

## Test Data

- 소스 백엔드: `SERVER_PORT=8081 ./gradlew bootRun`
- 소스 프론트엔드: `VITE_BACKEND_ORIGIN=http://127.0.0.1:8081 npm run dev -- --host 127.0.0.1 --port 5174`
- 사용자 ID: 실행 시점 임시 `qa-owner-*`
- 문서 업로드 샘플: 임시 `%PDF-1.4` 파일 2개
- 시세 입력 샘플: `estimatedJeonseValue=65000000`, `sourceLabel=전세 참고 시세`, `referenceDate=2026-05-24`

## Automation Plan

- 추가 QA 전용 테스트 파일은 만들지 않는다.
- 기존 Vitest 검증과 생산 빌드 결과를 기록한다.
- 소스 백엔드에 대해 Node 기반 API smoke를 실행한다.
- Playwright 기반 브라우저 smoke로 사용자 가시 상태를 검증한다.
