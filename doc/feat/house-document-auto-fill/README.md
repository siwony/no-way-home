# 등기부등본·임대차 계약서 자동 입력

## Goal

사용자가 등기부등본과 임대차 계약서를 등록하면 서비스가 문서를 구조화해서 계약 기본 정보, 등기 권리 정보, 임대인/주소/보증금/월세/계약일 같은 핵심 필드를 자동 입력한다.

이 기능은 기존 주택 계약 위험도 진단을 MVP 수준의 수기 입력 도구에서 실제 서비스용 문서 기반 진단 흐름으로 확장한다.

## User Problem

- 사용자는 등기부등본과 계약서의 법적/행정 문구를 직접 해석하기 어렵다.
- 문서 내용을 수기로 옮기면 주소, 금액, 임대인명, 계약일, 권리관계 같은 핵심 값이 누락되거나 잘못 입력될 수 있다.
- 실제 서비스에서는 자동 추출 결과를 그대로 믿게 만들면 안 되고, 추출 신뢰도와 검토 필요 항목을 분명히 보여줘야 한다.

## Product Principles

- 자동 입력은 사용자의 최종 확인 전까지 확정 데이터가 아니다.
- 문서 원본, 추출 텍스트, 추출 필드, 사용자 수정 내역을 구분해서 감사 가능하게 저장한다.
- 주민등록번호, 계좌번호, 전화번호 등 불필요한 개인정보는 수집·저장하지 않거나 마스킹한다.
- 인증서, 공동인증서 비밀번호, 결제 카드 정보, 인터넷등기소 로그인 정보는 저장하지 않는다.
- 외부 OCR/문서 분석/등기 확인 공급자는 교체 가능한 어댑터로 둔다.

## Target Documents

- 등기부등본 PDF
- 임대차 계약서 PDF 또는 이미지
- 후속 확장 후보: 건축물대장, 중개대상물 확인설명서, 특약 별지

## Expected Outcome

- 계약 기본 정보 자동 입력 초안
- 등기부등본 권리관계 자동 추출 초안
- 계약서와 등기부등본의 주소/소유자/임대인/금액 불일치 감지
- 필드별 신뢰도와 사용자가 확인해야 할 항목
- 원본 문서 보관, 추출 결과, 사용자 승인 상태가 분리된 상태 모델

## Harness Work

- Work ID: `house-document-auto-fill`
- Work log: `doc/agent-work/house-document-auto-fill/`
- Expected branch: `feat/house-document-auto-fill`
- Dependency: existing house risk backend/frontend work from PR #1. This branch starts stacked on that work and should be rebased onto `main` after PR #1 merges.
