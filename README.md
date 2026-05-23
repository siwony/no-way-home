# No Way Home

Kotlin + Spring Boot 기반 애플리케이션입니다. 초기 실행 환경은 `doc/init/tech-stack.md`의 지침을 따른다.

## Stack

- BellSoft Liberica JDK 25 LTS
- Kotlin 2.3.21
- Spring Boot 4.0.6
- PostgreSQL 18
- Flyway
- Docker Compose v2

## Local Setup

환경 변수 예시는 `.env.example`에 있다.

```sh
cp .env.example .env
```

애플리케이션과 PostgreSQL을 함께 실행한다.

```sh
docker compose up --build
```

상태 확인:

```sh
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/status
```

## Development

Gradle wrapper를 우선 사용한다.

```sh
./gradlew test
./gradlew build
./gradlew bootRun
```

로컬 JVM이 25가 아니어도 Gradle toolchain이 BellSoft JDK 25를 사용하도록 설정되어 있다.

컨테이너를 종료한다.

```sh
docker compose down
```
