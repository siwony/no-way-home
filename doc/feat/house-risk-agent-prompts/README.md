# Feature: 주택 계약 위험도 진단 서비스

이 폴더는 `doc/feat/feature-template.md` 구조에 맞춘 기능 구현 문서입니다. AI agent는 이 폴더의 문서를 읽고 주택 계약 위험도 진단 기능을 구현합니다.

## 문서 구조

- `goal.md`: 기능 목표와 사용자 가치
- `scope.md`: 포함 범위와 제외 범위
- `expected-behavior.md`: 입력, 분석, 리포트, API 동작
- `implementation-notes.md`: DB, 도메인 모델, 서비스, MVP 구현 메모
- `tests.md`: 단위/통합/E2E 테스트 기준
- `constraints.md`: 표현, 보안, 개인정보, 구현 제약

## Agent 작업 순서

1. `goal.md`와 `scope.md`로 구현 범위를 확정한다.
2. `expected-behavior.md`를 기준으로 사용자/API 동작을 구현한다.
3. 기술 스택이 아직 초기화되지 않았다면 `doc/init/tech-stack.md`를 먼저 수행한다.
4. `implementation-notes.md`에 따라 Kotlin 코드, DB, API를 설계한다.
5. `tests.md`의 체크리스트를 테스트로 옮긴다.
6. `constraints.md`를 최종 검수 기준으로 사용한다.

## 핵심 원칙

- 위험도는 법률 판단이 아니라 위험 신호 탐지 결과로 표현한다.
- “안전하다”처럼 단정하지 않고 “현재 확인된 자료 기준 특이 위험은 확인되지 않았습니다”처럼 표현한다.
- 임대인 국세/지방세 체납 조회는 범위에서 제외한다.
- 개인정보는 최소 수집하고 민감 문서와 임대인 이름은 암호화 저장을 전제로 한다.
- 기술 스택과 로컬 실행 환경은 `doc/init/tech-stack.md`를 따른다.
