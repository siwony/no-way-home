# UI/UX Acceptance: 등기부등본·임대차 계약서 자동 입력

Status: COMPLETED

Decision: CHANGES_REQUESTED

## Checklist Review

- [x] 기존 operational workspace 안에 `문서 자동 입력`과 `추출 검토 및 승인`이 카드 구조로 자연스럽게 배치되어 있다.
- [x] `등기부등본 PDF`, `임대차 계약서 PDF / 이미지` 고정 슬롯, 상태 pill, 파일 메타데이터, 삭제 액션이 구현되어 있다.
- [x] 추출 검토 카드에서 신뢰도, 문서 출처/페이지, 원문 근거, 필드별 `승인 / 수정 / 제외` 액션이 제공된다.
- [x] 문서 불일치 경고가 필드 목록 위에 먼저 노출되고, 관련 필드로 이동할 수 있다.
- [x] 승인 반영 전 비교 단계가 있고, 반영 후 기존 입력 카드에 `자동 입력 반영` / `사용자 수정됨` source note가 남는다.
- [x] 민감 정보 안내 문구, `지금은 수기 입력으로 계속`, `User ID 다시 적용`, `새 진단 시작` 등 수동 복귀/권한 복구 흐름이 존재한다.
- [ ] 실패 문서의 `다시 처리` 액션이 계획된 의미대로 동작한다.
- [ ] 사용자 제공 실제 PDF 2개에 대한 local-only QA 업로드 검증이 완료되었다.

## Findings

- Source-level 기준으로 핵심 흐름은 계획과 대체로 맞다. 단일 워크스페이스 유지, 전용 업로드 슬롯, 추출 검토 카드, 불일치 경고, 승인 후 source note, ACCESS_DENIED 시 문서 세션/반영값 정리까지는 확인됐다.
- Live visual acceptance는 이번 세션에서 in-app browser가 unavailable이라 수행하지 못했다. 이번 판단은 `frontend/src/App.tsx`, [`frontend/src/documentIntake.ts`](/Users/jeongcool/me/no-way-home/frontend/src/documentIntake.ts:1), [`frontend/src/types.ts`](/Users/jeongcool/me/no-way-home/frontend/src/types.ts:1), [`frontend/src/styles.css`](/Users/jeongcool/me/no-way-home/frontend/src/styles.css:1) 기준 source review다.

## Change Requests

- `다시 처리` UX를 실제 동작과 맞춰야 한다. 현재 실패 문서에서 버튼 라벨은 `다시 처리`지만 기존 업로드 핸들러를 그대로 타기 때문에 새 파일을 다시 선택하지 않으면 진행되지 않는다. 계획상 retry/reprocess 액션이어야 하므로, stored original 재처리로 동작시키거나 라벨/안내를 `다시 업로드`로 명확히 바꿔야 한다. 근거: [`frontend/src/App.tsx`](/Users/jeongcool/me/no-way-home/frontend/src/App.tsx:1188), [`frontend/src/App.tsx`](/Users/jeongcool/me/no-way-home/frontend/src/App.tsx:1244), [`frontend/src/App.tsx`](/Users/jeongcool/me/no-way-home/frontend/src/App.tsx:737), [`frontend/src/validation.ts`](/Users/jeongcool/me/no-way-home/frontend/src/validation.ts:54)
- overwrite 비교는 존재하지만 충돌 항목에서 `현재 값 유지`와 `승인값으로 교체`의 양쪽 선택지가 명시적으로 보이도록 다듬는 편이 좋다. 현재는 체크박스 하나와 `승인값으로 교체` 라벨만 있어, 미선택이 곧 현재 값 유지라는 규칙을 사용자가 추론해야 한다. 계획의 “explicit compare/apply before overwrite” 기준으로는 선택 결과를 더 직접적으로 보여줘야 한다. 근거: [`frontend/src/App.tsx`](/Users/jeongcool/me/no-way-home/frontend/src/App.tsx:1546)

## Decision Notes

- Decision은 `CHANGES_REQUESTED`다. 전체 구조와 핵심 검토 흐름은 승인 가능한 수준에 가깝지만, 실패 복구 액션과 overwrite 비교 명확성은 production-grade auto-fill UX 기준에서 아직 보완이 필요하다.
- 사용자 제공 실제 PDF 2개는 여전히 local-only manual QA 범위로 남아 있다. 이번 acceptance에서는 업로드하거나 커밋하지 않았고, QA 단계에서 별도 검증이 계속 필요하다.
