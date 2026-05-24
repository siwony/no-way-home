# QA Plan: 등기부등본·임대차 계약서 자동 입력

Status: COMPLETED

## Test Scope

- 문서 intake 세션 생성, 고정 슬롯 업로드, 추출 검토, 승인/수정/제외, 승인 반영, 기존 house check 생성/저장 흐름의 실제 연계 검증
- 문서/필드 상태 전이, 불일치 경고, source note, overwrite 비교, 접근 경계, 브라우저 저장소 최소화 검증
- mock fixture 기반 브라우저 E2E와 사용자 제공 실제 PDF 2종의 local-only 수용성 검증

## User Use Cases

- [x] `User ID` 적용 후 문서 intake 세션을 생성할 수 있다.
- [x] 등기부등본 PDF와 임대차 계약서 PDF/이미지를 각 전용 슬롯에 업로드할 수 있다.
- [x] 업로드 후 문서 상태가 `검토 필요`로 전이되고 추출 검토 카드가 열린다.
- [x] 대표 필드에 대해 `승인`, `수정`, `제외`를 수행할 수 있다.
- [x] mismatch warning, evidence, confidence, review status가 화면에 표시된다.
- [x] 승인한 필드 반영 시 compare/apply 요약과 충돌 선택 UI가 표시된다.
- [x] 승인한 계약/등기 값이 기존 입력 카드에 반영되고 source note가 남는다.
- [x] 반영된 계약 값으로 house check를 생성하고 등기 findings를 저장할 수 있다.
- [x] 실문서 PDF 2종도 업로드 수용되고 fake extraction 기반 검토 흐름으로 진입한다.

## Exception Cases

- [x] 승인된 필드가 없을 때 반영 차단 메시지가 노출된다.
- [x] 잘못된 파일 형식은 backend validation으로 거부된다.
- [x] 실패/삭제/수기 복귀 메시지가 기존 UX 계약과 어긋나지 않는다.
- [x] overwrite 비교에서 충돌 항목의 기본 선택과 문구가 혼동 없이 보인다.

## Permission And Security Cases

- [x] 다른 `X-User-Id` 또는 다른 사용자 세션으로 기존 문서 세션 접근 시 `ACCESS_DENIED` 경계 동작이 발생한다.
- [x] 접근 경계 전환 시 문서 파일명, 추출값, source note, apply preview가 화면에 남지 않는다.
- [x] `localStorage`/`sessionStorage`에 문서 원문, 추출값, 파일명, payload가 남지 않는다.
- [x] 실문서 업로드 후에도 원본 PDF는 로컬 QA 범위에서만 사용되고 repo에 복사/기록되지 않는다.

## Regression Risks

- [x] `frontend` 단위 테스트와 production build가 통과한다.
- [x] `DocumentIntakeControllerIntegrationTest`와 `HouseCheckControllerIntegrationTest`가 재실행 기준으로 통과한다.
- [x] 문서 반영 전 기존 house check 데이터가 변경되지 않고, 반영 후에도 사용자가 계속 수정 가능하다.
- [x] 문서 intake가 기존 house check 생성/등기 findings 저장/분석 이전 단계 흐름을 깨지 않는다.

## Test Data

- Mock/generated:
  - registry PDF fixture
  - lease PDF or image fixture
  - owner/address/deposit mismatch를 유도하는 deterministic fake extraction 입력
- Local-only real PDFs:
  - `/Users/jeongcool/me/no-way-home/6_부동산_등기사항_전부증명서.pdf`
  - `/Users/jeongcool/me/no-way-home/임대차계약서.pdf`

## Automation Plan

- Regression:
  - `cd frontend && npm test`
  - `cd frontend && npm run build`
  - `./gradlew test --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerIntegrationTest' --tests 'com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest' --rerun-tasks`
- Runtime:
  - `docker compose up -d postgres`
  - backend `./gradlew bootRun`
  - frontend `cd frontend && npm run dev -- --host 127.0.0.1 --port 4173`
- Browser E2E:
  - Playwright-driven local browser flow with mock/generated fixtures
  - local-only manual/automated upload validation for the two provided real PDFs
- Evidence:
  - capture stable screenshots for review/apply UI under `doc/agent-work/house-document-auto-fill/assets/`
