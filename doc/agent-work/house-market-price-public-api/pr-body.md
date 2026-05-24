## 개요

- Work ID: `house-market-price-public-api`
- 기능명: 공공 실거래가 시세 자동 조회
- 관련 문서:
  - `doc/agent-work/house-market-price-public-api/00-director-brief.md`
  - `doc/agent-work/house-market-price-public-api/01-ui-ux-plan.md`
  - `doc/agent-work/house-market-price-public-api/08-pr-lifecycle.md`

## 작업 내용

- [x] Director brief 작성
- [x] UI/UX plan 작성 및 Director approval 완료
- [x] Draft PR 생성
- [ ] Backend 구현
- [ ] Frontend 구현
- [ ] UI/UX acceptance 완료
- [ ] QA plan/report 완료
- [ ] Director final review 완료

## 리뷰 필요

- 모든 공공 API 호출이 XML-only인지 확인
- KB시세 또는 비공식 scraping이 들어가지 않았는지 확인
- 조회 결과가 자동 저장/자동 덮어쓰기 되지 않는지 확인
- provider 비활성화, API key 누락, 표본 부족 때 수동 입력 fallback이 유지되는지 확인
- 테스트 결과와 잔여 risk 확인

## 스크린샷 (필요한 경우)

구현 후 `doc/agent-work/house-market-price-public-api/assets/`에 Playwright 증거 이미지를 추가한다.
