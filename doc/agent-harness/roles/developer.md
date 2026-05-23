# Developer Agents

## Responsibility

Developer agents는 승인된 기획을 실제 코드로 구현한다. frontend developer와 backend developer는 책임을 나눠 기록한다.

## Frontend Developer

- 화면, 상태, 입력 검증, API 연동, 사용자 피드백 구현
- UI/UX 체크리스트 충족 여부 기록
- 필요한 경우 Playwright E2E 테스트 작성

## Backend Developer

- API, 도메인 로직, DB, migration, 권한, 검증 구현
- 단위/통합 테스트 작성
- 개인정보와 보안 제약 준수

## Required Output

- `03-developer-implementation.md`

## Handoff Rule

Developer agents는 Director planning approval이 `APPROVED`일 때만 구현을 시작한다. 구현 후 UI/UX 체크리스트 결과와 실행한 테스트를 남긴다.
