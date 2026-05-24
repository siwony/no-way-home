# UI/UX Acceptance: 주택 계약 위험도 진단 프론트엔드

Status: APPROVED

Decision: APPROVED

## Review Summary

이번 live integration rework는 사용자에게 보이는 화면 구조나 카피를 바꾸는 수정이 아니다. 승인된 단일 route 작업형 흐름, `User ID` 경계 노출, 계약 기본 정보 생성 이후 같은 화면에서 업로드/수기 확인/시세/분석/결과로 이어지는 사용자 흐름은 그대로 유지된다. `frontend/src/App.tsx`와 `frontend/src/styles.css` 기준으로도 이번 루프에서 추가된 새 UI 상태나 회귀는 확인되지 않았다.

수정의 실질적 UX 효과는 local dev/preview에서 `POST /api/house-checks`가 잘못된 upstream으로 빠져 404가 나는 경우를 줄였다는 점이다. `frontend/vite.backend.ts`와 `frontend/vite.config.ts`는 dev와 preview 모두에서 동일한 backend 탐지 규칙을 사용하고, `service=no-way-home` 응답을 기준으로 `:8080`과 `:8081` 후보를 판별한 뒤 proxy target을 잡는다. 따라서 사용자가 계약 기본 정보 제출 직후 generic failure에 멈춰 서는 가능성은 기존보다 낮아졌다.

## Checklist Status

- [x] 계약 기본 정보 제출 이후의 visible UI flow는 변경되지 않았다.
- [x] `ACCESS_DENIED`, `ANALYSIS_NOT_READY`, 빈 상태, 로딩, validation 분리는 기존 승인 상태를 유지한다.
- [x] dev와 preview가 같은 backend 감지 규칙을 사용해 local integration mismatch로 인한 create-step dead end 위험을 낮춘다.

## Residual QA Risk

- QA는 `npm run dev`와 `npm run preview` 각각에서, `:8080`에 다른 프로세스가 있고 실제 backend가 `:8081`에 있을 때 `진단 시작`이 정상적으로 생성 단계로 넘어가는지 확인해야 한다.
- QA는 backend가 후보 포트 외 위치에 있을 때 `VITE_BACKEND_ORIGIN` 또는 `VITE_BACKEND_CANDIDATES` 설정으로 복구 가능한지, 그리고 미설정 시 사용자가 여전히 generic create failure로만 보이게 되는지 확인할 필요가 있다.

## Changed Files

- `doc/agent-work/house-risk-agent-prompts-frontend/04-ui-ux-acceptance.md`

## Open Questions Or Requested Changes

- 없음
