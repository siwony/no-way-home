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

## External Skill 활용

구현 전에 `https://github.com/NomaDamas/k-skill` 저장소의 관련 skill을 확인하고, 설치되어 있거나 사용 가능한 경우 다음 범위 안에서 활용한다.

- `real-estate-search`: 국토교통부 실거래가/전월세 신고 데이터 조회에 사용한다. 시세/전세가율/총 위험 노출 비율 계산의 외부 데이터 후보로 우선 검토한다.
- `iros-registry-automation`: 인터넷등기소 등기부등본 발급 준비와 장바구니/저장 보조 흐름에만 사용한다. 로그인, 인증, 결제는 사용자가 직접 처리해야 하며 에이전트나 서버가 인증정보를 입력하거나 저장하지 않는다.
- `gongsijiga-search`: 개별공시지가 조회용이다. 공시지가는 시세나 실거래가가 아니므로 보증금 위험 계산의 추정 시세로 사용하지 않는다.
- `daangn-realty-search`: 공개 매물/호가 참고용으로만 사용할 수 있다. 국토교통부 실거래가와 섞어 계산하지 않고, 출처와 한계를 별도로 표시한다.

skill이 설치되어 있지 않거나 외부 호출이 막힌 환경에서는 직접 의존을 강제하지 않는다. 대신 `implementation-notes.md`의 포트/어댑터 구조를 따라 외부 조회 구현을 교체 가능하게 만들고, 테스트에서는 fake provider를 사용한다.

## 핵심 원칙

- 위험도는 법률 판단이 아니라 위험 신호 탐지 결과로 표현한다.
- “안전하다”처럼 단정하지 않고 “현재 확인된 자료 기준 특이 위험은 확인되지 않았습니다”처럼 표현한다.
- 임대인 국세/지방세 체납 조회는 범위에서 제외한다.
- 개인정보는 최소 수집하고 민감 문서와 임대인 이름은 암호화 저장을 전제로 한다.
- 기술 스택과 로컬 실행 환경은 `doc/init/tech-stack.md`를 따른다.
