# 🩺 Pulse Check (Pulse-Check)

Pulse Check는 대규모 로그 파일을 효율적으로 탐색하고, 실시간으로 모니터링하며, 특정 조건에 따라 필터링 및 분석하기 위해 설계된 **Spring Boot 기반 중앙 집중형 로그 관리 도구**입니다.

---

## 🚀 주요 기능 (Key Features)

### 1. 실시간 로그 모니터링 (Tail Logs)

- **Server-Sent Events (SSE)**를 활용하여 서버의 로그를 웹 브라우저나 클라이언트에서 실시간으로 스트리밍합니다.
- 다중 연결 관리 모드를 통해 여러 로그의 변화를 동시에 추적할 수 있습니다.

### 2. 고급 로그 필터링 및 검색 (Analysis)

- **키워드 기반 검색**: 여러 키워드를 입력하여 `AND` 또는 `OR` 조건으로 로그를 필터링합니다.
- **기간 설정**: 특정 시간 범위(`from` ~ `to`) 내의 로그만 추출할 수 있습니다.
- **성능 분석**: 실행 시간(`ms`) 필터를 통해 특정 시간 이상 소요된 요청(Slow Log)만 선별하여 분석할 수 있습니다.
- **마스킹 처리**: 개인정보나 중요한 데이터가 포함된 로그를 실시간으로 마스킹하여 보안을 유지합니다.

### 3. 로그 컨텍스트 확인 (Context View)

- 특정 로그 라인을 기준으로 전후 일정 영역(`n` 라인)의 로그를 함께 조회하여 에러 발생 전후의 상황을 정확히 파악할 수 있습니다.

### 4. 로그 탐색 및 관리 (File Explorer)

- 설정된 루트 디렉토리(`/temp/logs/` 등)를 기준으로 서버 내의 로그 파일 구조를 직관적으로 탐색합니다.

---

## 🛠 기술 스택 (Tech Stack)

| 구분                | 기술                         |
| :------------------ | :--------------------------- |
| **Language**        | Java 8                       |
| **Framework**       | Spring Boot 2.7.18           |
| **Security**        | Spring Security (Basic Auth) |
| **Database**        | H2 Database (File-based)     |
| **Template Engine** | Thymeleaf                    |
| **Build Tool**      | Gradle                       |

---

## ⚙️ 설정 방법 (Configuration)

`src/main/resources/application.yml` 파일에서 주요 설정을 관리할 수 있습니다.

```yaml
pulsecheck:
  default-log-path: /temp/logs/      # 로그 탐색 기본 경로
  max-result-lines: 10000            # 최대 결과 라인 수

  security:
    username: *                  # API 인증 계정
    password: *               # API 인증 비밀번호

  sse:
    max-connections: 10             # 최대 SSE 동시 연결 수
    timeout-minutes: 30              # SSE 타임아웃 시간

  patterns:
    timestamp: "((?:^|\\s)\\d{2}:\\d{2}:\\d{2}...)" # 로그 타임스탬프 형식 (Regex)
```

---

## 📖 사용 방법 (How to Use)

### 1. 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션은 기본적으로 `8080` 포트에서 실행됩니다.

### 2. API 엔드포인트

#### 🔍 로그 필터링 검색

- **URL**: `GET /api/log`
- **Parameters**:
  - `path`: 로그 파일 경로 (필수)
  - `keywords`: 콤마로 구분된 검색어
  - `keywordMode`: `AND` 또는 `OR` (기본값: `AND`)
  - `minMs`: 최소 처리 시간 (ms)
  - `from` / `to`: 검색 기간
  - `masking`: 마스킹 적용 여부 (`true`/`false`)

#### 📡 실시간 로그 스트리밍 (SSE)

- **URL**: `GET /api/tail`
- **Parameters**:
  - `path`: 로그 파일 경로 (필수)
  - `masking`: 마스킹 적용 여부 (`true`/`false`)

#### 📄 로그 컨텍스트 조회

- **URL**: `GET /api/log/context`
- **Parameters**:
  - `path`: 로그 파일 경로
  - `line`: 대상 라인 번호
  - `context`: 전후 조회 라인 수 (기본값: 20)

---

## 🔒 보안 (Security)

본 시스템은 중요 로그를 다루므로 **Spring Security**를 통한 기본 인증(Basic Auth)이 적용되어 있습니다.
운영 환경에 배포 시 반드시 `application.yml`의 `security.password`를 변경하십시오.

---

## 📈 라이선스 (License)

이 프로젝트는 개인 및 학습용으로 제작되었으며, 상업적 이용 시 소스코드 내 보안 로직을 재검토하시기 바랍니다.
