## 개요

- Work ID: `{{WORK_ID}}`
- 기능명: {{TITLE}}
- 관련 문서:
  - `doc/agent-work/{{WORK_ID}}/00-director-brief.md`
  - `doc/agent-work/{{WORK_ID}}/01-ui-ux-plan.md`
  - `doc/agent-work/{{WORK_ID}}/08-pr-lifecycle.md`

## 작업 내용

- [ ] Director brief 작성
- [ ] UI/UX plan 작성 및 Director approval 완료
- [ ] Draft PR 생성
- [ ] Backend 구현
- [ ] Frontend 구현
- [ ] UI/UX acceptance 완료
- [ ] QA plan/report 완료
- [ ] Director final review 완료

## 리뷰 필요

- 승인된 scope와 실제 구현이 일치하는지 확인
- 실패 loop가 있었다면 `08-pr-lifecycle.md`와 각 agent work log 확인
- 테스트 결과와 잔여 risk 확인

## 스크린샷 (필요한 경우)

Frontend 변경이 없으면 `해당 없음`으로 둔다.

Frontend 화면 증거가 필요하면 이미지를 `doc/agent-work/{{WORK_ID}}/assets/`에 커밋하고 아래 형식으로 연결한다.

```md
![화면 설명](https://github.com/<owner>/<repo>/blob/<branch>/doc/agent-work/{{WORK_ID}}/assets/<file>.png?raw=1)
```
