# Constraints

## Service Quality

- 실제 서비스용 기능으로 설계한다. 단순 demo parser나 문자열 포함 여부만으로 위험 신호를 확정하지 않는다.
- 문서 처리 결과는 원문 근거와 신뢰도를 함께 제공한다.
- 사용자 승인 없이 자동 추출값을 최종 분석 입력으로 확정하지 않는다.

## Security And Privacy

- 원본 문서는 민감 문서로 보고 암호화 저장한다.
- 추출 텍스트도 민감 데이터로 취급한다.
- 주민등록번호, 계좌번호, 전화번호 등 필수 범위 밖 개인정보는 저장하지 않거나 마스킹한다.
- 임차인 개인정보는 기본 저장 대상에서 제외한다.
- 감사 로그에는 문서 원문, 주민등록번호, 계좌번호, 인증정보를 남기지 않는다.
- 인증서, 공동인증서 비밀번호, 인터넷등기소 로그인 정보, 결제 정보는 저장하지 않는다.

## Architecture

- OCR, PDF text extraction, document classification, field extraction, mismatch detection, risk mapping은 분리된 포트/어댑터 또는 서비스로 둔다.
- 핵심 도메인 테스트는 외부 OCR/provider 네트워크 접근 없이 fixture와 fake adapter로 검증 가능해야 한다.
- 외부 provider 도입 시 provider response는 adapter boundary에서 내부 canonical field model로 변환한다.
- 문서 처리 job은 실패/재시도/타임아웃/중복 업로드를 처리해야 한다.

## Compliance And Operations

- 원본 문서 retention 정책을 구현 가능한 상태로 모델링한다.
- 사용자 삭제 요청에 대응할 수 있도록 document와 extraction artifact 삭제 경로를 둔다.
- provider 장애와 추출 실패율을 관찰할 수 있는 운영 로그/메트릭 hook을 둔다.
- 실제 법률 판단, 계약 체결 가능성 확정, 보증금 회수 가능성 확정 문구는 금지한다.
