## 개요

- Work ID: `house-document-auto-fill`
- 기능명: 등기부등본·임대차 계약서 자동 입력
- 관련 문서:
  - `doc/agent-work/house-document-auto-fill/00-director-brief.md`
  - `doc/agent-work/house-document-auto-fill/01-ui-ux-plan.md`
  - `doc/agent-work/house-document-auto-fill/08-pr-lifecycle.md`

등기부등본과 임대차 계약서를 등록하면 문서에서 핵심 정보를 추출하고, 사용자의 검토/승인 후 기존 주택 계약 위험도 진단 입력값에 자동 반영하는 실제 서비스용 문서 intake 기능을 추가한다.

## 작업 내용

- [x] Director brief 작성
- [x] UI/UX plan 작성 및 Director approval 완료
- [x] Draft PR 생성
- [x] Backend 구현
- [x] Frontend 구현
- [x] UI/UX acceptance 완료
- [x] QA plan/report 완료
- [ ] Director final review 완료

## 리뷰 필요

- “직접 등기부등본 확인”의 첫 구현 범위가 인증/결제 대행이 아니라 사용자가 확보한 문서의 구조화와 검토 지원으로 제한되는지 확인
- 문서 원본/추출 결과/승인 입력값의 보안 경계가 충분한지 확인
- 외부 OCR/provider가 도메인 로직과 결합되지 않는지 확인
- 사용자 승인 전 자동 입력값이 최종 분석값으로 확정되지 않는지 확인
- 사용자 제공 PDF 두 개는 local-only QA 입력으로만 사용하고 커밋하지 않는지 확인
- Draft PR: https://github.com/siwony/no-way-home/pull/2
- Backend slice status: `BACKEND_READY_FOR_FRONTEND`
- Backend verification:
  - `./gradlew test --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerIntegrationTest' --tests 'com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest' --rerun-tasks` passed
- Frontend slice status: `READY_FOR_UI_UX_ACCEPTANCE`
- Frontend verification:
  - `cd frontend && npm test` passed, 3 files / 13 tests
  - `cd frontend && npm run build` passed
- UI/UX acceptance loop:
  - `CHANGES_REQUESTED`
  - failed-document `다시 처리` wording must match actual upload behavior
  - overwrite comparison must show explicit `현재 값 유지` vs `승인값으로 교체`
- UI/UX requested rework:
  - failed-document action now says `파일 선택 후 다시 업로드`
  - conflict rows now show `선택됨: 현재 값 유지` or `선택됨: 승인값으로 교체`
  - `cd frontend && npm test` passed, 3 files / 13 tests
  - `cd frontend && npm run build` passed
- UI/UX re-acceptance: `APPROVED`
- QA report:
  - Mock fixture browser E2E passed through document session creation, registry/lease uploads, field review, compare/apply, form population, house check creation, registry findings save, stale-session `ACCESS_DENIED`, and browser storage hygiene.
  - Initial blocking result: `FAIL` because the two local-only user-provided PDFs were rejected before persistence with `HTTP 413` / `MaxUploadSizeExceededException`.
  - Rework added 20MB backend upload policy, Spring multipart limits, JSON `413`, frontend preflight validation, and clearer upload errors.
  - Final result: `PASS`; the two local-only PDFs uploaded through the UI, 20 extracted fields were approved, approved values were applied, and house check `bd31b759-c0a1-4f45-b0ad-26297b8cac5d` was created.
  - Real PDF storage evidence: registry `11006061` bytes and lease `9679921` bytes reached `APPROVED`; encrypted `.bin` files did not contain `%PDF`.
- Final verification:
  - `cd frontend && npm test` passed, 3 files / 17 tests
  - `cd frontend && npm run build` passed
  - `./gradlew test --tests 'com.nowayhome.housecheck.application.DocumentIntakeFilePolicyTest' --tests 'com.nowayhome.housecheck.api.DocumentIntakeControllerIntegrationTest' --tests 'com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest' --rerun-tasks` passed
  - `./gradlew test` passed

## 스크린샷 (필요한 경우)

- Mock fixture extraction review: https://github.com/siwony/no-way-home/blob/feat/house-document-auto-fill/doc/agent-work/house-document-auto-fill/assets/qa-document-review-mock.png
- Mock fixture compare/apply preview: https://github.com/siwony/no-way-home/blob/feat/house-document-auto-fill/doc/agent-work/house-document-auto-fill/assets/qa-apply-preview-mock.png
