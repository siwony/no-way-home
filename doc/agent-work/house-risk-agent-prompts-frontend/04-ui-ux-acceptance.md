# UI/UX Acceptance: 주택 계약 위험도 진단 프론트엔드

Status: CHANGES_REQUESTED

Decision: CHANGES_REQUESTED

## Review Summary

The frontend matches the approved single-route operational workspace well overall. The `User ID` boundary is explicit, the major create/upload/manual-findings/market/analyze/result actions are present, card-level loading and validation states are implemented, `ANALYSIS_NOT_READY` is separated from generic errors, and browser persistence is limited to `sessionStorage` for `User ID` and `checkId`.

However, the approved `ACCESS_DENIED` recovery flow is not fully satisfied yet. The denial state clears server-derived report/status content correctly, but the dedicated denial panel does not include the required direct recovery actions.

## Commands Run

```text
cd frontend && npm test
- success
- vitest: 1 file passed, 4 tests passed

cd frontend && npm run build
- success
- tsc no-emit checks passed, vite production build passed
```

## Checklist Review

- [x] 기본 route가 곧바로 진단 도구 화면으로 열리고 마케팅 hero 페이지가 없다.
- [x] `User ID` 적용 전에는 주요 API 호출 버튼이 비활성화되고, 적용 후 요청 헤더 경계가 일관되게 구현되어 있다.
- [x] 계약 생성, 등기/건축물대장 PDF 업로드, 등기/건축물대장 수기 확인 저장, 시세 저장, 분석 실행, 리포트/체크리스트 조회 흐름이 한 화면 안에 구성되어 있다.
- [x] 정상, 빈 상태, 로딩, validation, `ANALYSIS_NOT_READY`, 일반 오류 상태가 구분되어 있다.
- [x] 위험 안내 문구는 도구형 톤을 유지하고, 금지된 단정 표현을 프론트 copy에서 사용하지 않는다.
- [x] `ACCESS_DENIED` 발생 시 서버 기반 결과/상태/파일명 노출은 제거되고 영구 저장소에도 서버 민감 데이터가 남지 않는다.
- [ ] 접근 거부 패널 자체에 `User ID 다시 적용`과 `새 진단 시작` 회복 액션이 포함되어 있지 않다.

## Findings

- `frontend/src/App.tsx:1027`-`1041`의 접근 거부 패널은 제목, 설명, 현재 `User ID`, 현재 `checkId`는 보여 주지만, 승인된 UI/UX plan에서 요구한 `User ID 다시 적용`과 `새 진단 시작` 액션을 패널 내부에 제공하지 않는다. 현재는 상단 공통 헤더의 입력/버튼에 의존해야 해서, `ACCESS_DENIED` 전용 복구 흐름이 불완전하다.

## Change Requests

- 접근 거부 패널 안에 즉시 사용할 수 있는 회복 액션 2개를 추가한다: `User ID 다시 적용`과 `새 진단 시작`.
- 해당 패널만 보고도 다음 행동을 바로 실행할 수 있도록, 상단 헤더와 별도로 denial state 내부에서 복구 경로가 완결되게 한다.

## Decision Notes

Use one decision value: `APPROVED`, `CHANGES_REQUESTED`, or `BLOCKED`.
