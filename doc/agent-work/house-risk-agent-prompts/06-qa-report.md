# QA Report: 주택 계약 위험도 진단 서비스

Status: COMPLETE

Result: PASS

## Summary

Backend rework 이후 QA-01과 QA-02는 재현되지 않았다. 승인된 `estimatedJeonseValue` 단독 입력 계약이 분석/리포트까지 유지되고, 임대인 이름과 업로드 문서 바이트도 자동화된 증거 기준에서 평문 저장되지 않는다. 남아 있는 항목은 테스트 미실행 갭이며 현재 gate를 막는 결함은 없다.

## Tests Executed

```text
./gradlew test
- PASS
- BUILD SUCCESSFUL in 398ms
- Gradle test task returned from cache with no failures

./gradlew test --rerun-tasks
- PASS
- BUILD SUCCESSFUL in 5s

./gradlew test --tests com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest --tests com.nowayhome.housecheck.security.HouseCheckCryptoServiceTest --tests com.nowayhome.housecheck.domain.analysis.HouseRiskAnalysisComponentsTest --rerun-tasks
- PASS
- BUILD SUCCESSFUL in 8s

build/test-results/test/*.xml
- PASS
- HouseCheckControllerIntegrationTest: 4 tests, 0 failures
- HouseRiskAnalysisComponentsTest: 6 tests, 0 failures
- HouseCheckCryptoServiceTest: 1 test, 0 failures
- Total: 11 tests, 0 failures
```

## Executed Checks And Evidence

- Jeonse-only saved input is preserved through analysis and report. Evidence: [HouseCheckControllerIntegrationTest.kt](/Users/jeongcool/me/no-way-home/src/test/kotlin/com/nowayhome/housecheck/api/HouseCheckControllerIntegrationTest.kt:178) asserts `estimatedJeonseValue=65000000`, `sourceType=USER_ENTERED`, `jeonseRatio.calculationStatus=NOT_AVAILABLE`, absence of `MARKET_PRICE_REQUIRED`, and presence of `MARKET_VALUE_REQUIRED_FOR_RATIO`.
- The unit boundary also preserves jeonse-only snapshots without inventing a market value. Evidence: [HouseRiskAnalysisComponentsTest.kt](/Users/jeongcool/me/no-way-home/src/test/kotlin/com/nowayhome/housecheck/domain/analysis/HouseRiskAnalysisComponentsTest.kt:121).
- Saved market-price semantics match the approved contract. Evidence: [MarketPriceAssessmentService.kt](/Users/jeongcool/me/no-way-home/src/main/kotlin/com/nowayhome/housecheck/domain/analysis/MarketPriceAssessmentService.kt:17) keeps `estimatedJeonseValue` when market value is absent, and [MarketPriceRiskAnalyzer.kt](/Users/jeongcool/me/no-way-home/src/main/kotlin/com/nowayhome/housecheck/domain/analysis/MarketPriceRiskAnalyzer.kt:24) emits the limited-calculation reason instead of `MARKET_PRICE_REQUIRED`.
- Report DTO now exposes both `estimatedMarketValue` and `estimatedJeonseValue` as user-entered fields. Evidence: [HouseCheckQueryService.kt](/Users/jeongcool/me/no-way-home/src/main/kotlin/com/nowayhome/housecheck/application/HouseCheckQueryService.kt:85).
- Landlord name is encrypted before persistence. Evidence: [HouseCheckEntities.kt](/Users/jeongcool/me/no-way-home/src/main/kotlin/com/nowayhome/housecheck/persistence/HouseCheckEntities.kt:48) applies `EncryptedStringAttributeConverter`, and [HouseCheckCryptoService.kt](/Users/jeongcool/me/no-way-home/src/main/kotlin/com/nowayhome/housecheck/security/HouseCheckCryptoService.kt:41) performs AES-GCM text conversion.
- Uploaded document bytes are encrypted before filesystem write. Evidence: [HouseCheckDocumentStorage.kt](/Users/jeongcool/me/no-way-home/src/main/kotlin/com/nowayhome/housecheck/storage/HouseCheckDocumentStorage.kt:30) writes `encryptBytes(file.bytes)`, and [HouseCheckControllerIntegrationTest.kt](/Users/jeongcool/me/no-way-home/src/test/kotlin/com/nowayhome/housecheck/api/HouseCheckControllerIntegrationTest.kt:226) verifies stored bytes neither equal nor contain the original plaintext payload or `%PDF-1.4` header.
- Crypto round-trip and plaintext non-exposure are covered directly. Evidence: [HouseCheckCryptoServiceTest.kt](/Users/jeongcool/me/no-way-home/src/test/kotlin/com/nowayhome/housecheck/security/HouseCheckCryptoServiceTest.kt:11).

## Passed Cases

- Full Spring Boot/Kotlin test suite passed on rerun.
- Phase 1 create -> upload -> manual findings -> market price save -> analyze -> report/checklist flow remains green.
- Negative deposit validation still returns `INVALID_DEPOSIT_AMOUNT`.
- Different-owner checklist access still returns `ACCESS_DENIED`.
- QA-01 resolved: jeonse-only market input is no longer treated as missing market data.
- QA-02 resolved: landlord name and uploaded document bytes now have an application-level encryption-at-rest boundary with automated evidence.

## Defects

- None.

## Coverage Review Against tests.md

- Covered in current automated suite:
- 전세가율/총 위험 노출 비율 계산
- 보수적 회수 시뮬레이션 계산
- 위험도 병합 우선순위
- 시세 정보 누락/unsupported source 방어 일부
- 등기부등본 누락 시 사유 생성
- 진단 요청 생성
- 등기부등본 업로드
- 건축물대장 업로드
- 시세 정보 저장
- 위험도 분석 실행
- 리포트 조회
- 체크리스트 조회
- 본인 소유가 아닌 요청 접근 차단 1건
- 전세가-only 입력 경로
- landlord/document 암호화 저장 경계

- Not rerun or still partially covered:
- building risk 규칙 세부 단위 케이스
- 비-PDF 업로드 거절과 날짜 형식 오류
- 업로드/수동 입력/분석 실행 전체 권한 차단 매트릭스
- fake `MarketPriceProvider` 교체 가능성 통합 검증
- 인증정보/인증서 비밀번호/결제정보 미저장 검증
- 로그 마스킹 검증

## Untested Risks Or Blockers

- Blockers: 없음.
- Residual risks:
- 로그 마스킹은 이번 rerun에서 직접 검증하지 않았다.
- 권한 차단은 report/checklist 외 endpoint 조합을 전부 재검증하지 않았다.
- `tests.md`의 일부 방어 케이스는 아직 자동화 범위가 얇다. 다만 이번 QA loop의 실패 원인이었던 QA-01, QA-02와 직접 연결되지는 않는다.

## Result Notes

현재 QA 기준으로 Director final review로 넘겨도 된다. 이전 실패 결함은 해소됐고, 이번 rerun 범위 내에서 재현 가능한 blocker는 남지 않았다.

Use one result value: `PASS`, `FAIL`, or `BLOCKED`.
