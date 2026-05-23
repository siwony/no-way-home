# Init: 기술 스택 및 로컬 실행 환경

## Goal

Kotlin + Spring Boot 애플리케이션을 Docker Compose 기반으로 실행할 수 있는 초기 스택을 구성한다. DB는 PostgreSQL을 사용하고, Java 런타임은 BellSoft Liberica JDK 최신 LTS를 사용한다.

## Baseline

구현 시작 시 공식 문서를 기준으로 최신 LTS 또는 stable 지원 버전을 다시 확인한다. 2026-05-24 기준 baseline:

| Component | Baseline |
|---|---|
| Java | BellSoft Liberica JDK 25 LTS |
| Kotlin | 2.3.21 |
| Spring Boot | 4.0.6 stable |
| PostgreSQL | 18.4 |
| Docker Compose | Docker Compose v2 CLI |

Kotlin과 Spring Boot는 “LTS” 표기가 제품별로 다르므로 최신 stable/support 상태를 확인한다. Java는 BellSoft Liberica JDK의 최신 LTS 릴리스를 사용하고, PostgreSQL은 지원 기간이 있는 최신 주 버전을 우선한다.

## Expected Output

- Gradle Kotlin DSL 기반 Spring Boot 프로젝트
- `compose.yaml`
- app service용 Dockerfile
- PostgreSQL service
- `.env.example`
- Spring datasource 환경 변수 연결
- schema migration 도구 설정
- 로컬 실행/테스트 명령 문서화

## Docker Compose Requirements

루트에 `compose.yaml`을 둔다. 최소 service는 다음과 같다.

- `app`: Spring Boot 애플리케이션
- `postgres`: PostgreSQL 데이터베이스

Compose 요구사항:

- `app` Dockerfile은 BellSoft Liberica JDK 25 LTS 기반 이미지를 사용한다.
- 기본 후보 이미지는 `bellsoft/liberica-openjdk-debian:25`이며, 구현 시 BellSoft 공식 컨테이너 태그에서 최신 권장 태그를 확인한다.
- `postgres`는 `postgres:18` 계열 이미지를 사용한다.
- 데이터는 named volume으로 보존한다.
- `app`은 `postgres` healthcheck 이후 기동되도록 설정한다.
- DB 접속 정보는 환경 변수로 주입하고 비밀값은 커밋하지 않는다.
- 로컬 기본 DB명, 사용자, 포트는 `.env.example`에만 안전한 값으로 문서화한다.
- 애플리케이션 설정은 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`를 지원한다.
- 로컬 실행 명령은 `docker compose up --build`를 기준으로 한다.

## PostgreSQL Requirements

- schema migration은 Flyway 또는 Liquibase 중 하나를 사용한다.
- 기본 키 전략은 프로젝트에서 하나로 통일한다.
- enum 저장은 문자열 값을 우선한다.
- 반정형 데이터가 필요하면 `jsonb`를 사용한다.
- 초기 migration이 통합 테스트와 Compose 환경에서 모두 실행되어야 한다.

## Verification

- `docker compose config`가 성공한다.
- `docker compose up --build`로 app과 postgres가 함께 실행된다.
- app이 PostgreSQL datasource에 연결된다.
- migration이 정상 실행된다.
- 테스트 또는 health endpoint로 애플리케이션 기동을 확인한다.

## Version References

구현 시 다음 공식 문서를 확인해 baseline을 갱신한다.

- BellSoft Liberica JDK: `https://docs.bell-sw.com/liberica-jdk/latest/`
- BellSoft Liberica container images: `https://bell-sw.com/libericajdk-containers/`
- Kotlin FAQ: `https://kotlinlang.org/docs/faq.html`
- Spring Boot documentation: `https://docs.spring.io/spring-boot/index.html`
- PostgreSQL versioning: `https://www.postgresql.org/support/versioning/`
