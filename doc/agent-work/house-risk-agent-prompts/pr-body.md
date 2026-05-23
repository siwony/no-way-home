## 개요

- Work ID: `house-risk-agent-prompts`
- 기능명: 주택 계약 위험도 진단 서비스
- 관련 문서:
  - `doc/agent-work/house-risk-agent-prompts/00-director-brief.md`
  - `doc/agent-work/house-risk-agent-prompts/01-ui-ux-plan.md`
  - `doc/agent-work/house-risk-agent-prompts/08-pr-lifecycle.md`

전월세 계약 전 위험 신호를 확인할 수 있는 backend Phase 1 API를 구현한다.

## 작업 내용

- [x] Director brief 작성
- [x] UI/UX plan 작성 및 Director approval 완료
- [ ] Draft PR 생성
- [x] Backend 구현
- [x] UI/UX acceptance 완료
- [x] QA plan/report 완료
- [x] Director final review 완료

## 리뷰 필요

- backend Phase 1 scope와 실제 구현이 일치하는지 확인
- QA loop에서 수정된 `estimatedJeonseValue` 단독 입력 처리 확인
- landlord name 및 uploaded PDF 암호화-at-rest 경계 확인
- Draft PR 생성 전 `main`이 아닌 feature branch로 이동 필요

## 스크린샷 (필요한 경우)

해당 없음
