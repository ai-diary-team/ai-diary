# AI Diary

[![CI](https://github.com/chanwoong00/ai-diary/actions/workflows/ci.yml/badge.svg)](https://github.com/chanwoong00/ai-diary/actions/workflows/ci.yml)

> 하루를 글로 남기면, AI가 마음을 함께 돌아봐 주는 개인 다이어리 서비스입니다.

바쁜 하루를 보내다 보면 내가 어떤 기분으로 지냈는지 놓치기 쉽습니다. AI Diary는 사용자가 일기를 작성하면 Gemini 기반 AI가 글 속 감정을 분석하고, 짧은 공감 피드백과 감정 변화 그래프를 보여줍니다.

> 이 저장소는 팀 프로젝트로 만들었습니다. 로그인부터 AI 분석, 테스트, Docker와 AWS 자동 배포까지 하나의 서비스가 동작하는 전체 과정을 담고 있습니다.

## 실행 링크

- [AI Diary 바로가기](http://3.36.118.113/)
- 서버 비용 관리를 위해 EC2 인스턴스는 필요할 때만 실행합니다. 서버가 중지된 경우에는 링크에 접속할 수 없습니다.

## 어떤 서비스인가요?

1. 사용자가 오늘 있었던 일과 마음을 일기로 작성합니다.
2. AI가 일기의 감정을 분석합니다.
3. 기쁨, 슬픔, 분노, 불안, 평온의 점수와 짧은 피드백을 확인합니다.
4. 일기가 쌓이면 최근 마음의 변화를 그래프로 돌아볼 수 있습니다.
5. 연속 기록 일수와 1년 전 오늘의 기록도 확인할 수 있습니다.

```text
오늘의 일기 작성
      ↓
AI가 감정과 피드백 분석
      ↓
오늘의 마음 확인
      ↓
시간이 쌓일수록 감정 흐름 돌아보기
```

## 주요 기능

### 계정과 나만의 기록

- 이메일 회원가입과 로그인
- 내 일기는 나만 조회할 수 있도록 로그인 인증 적용
- 일기 작성, 목록 보기, 상세 보기, 삭제
- 삭제한 일기는 화면과 감정 통계에서 제외

### AI 감정 분석

- Gemini 기반 AI가 일기 본문을 분석
- 대표 감정과 감정 강도(1~5점) 제공
- 기쁨·슬픔·분노·불안·평온 5가지 감정 점수 제공
- 일기 내용에 맞춘 짧은 공감 피드백 제공

### 기록 돌아보기

- 최근 7일 또는 30일 감정 변화 그래프
- 기간별 대표 감정과 평균 감정 점수
- 달력에서 날짜별 일기 확인
- 제목·본문 키워드 검색과 감정별 필터
- 연속 일기 작성일(streak) 확인
- 1년 전 오늘의 일기 돌아보기

## 서비스는 어떻게 동작하나요?

```mermaid
flowchart LR
    U[사용자] --> F[웹 화면]
    F --> B[AI Diary 백엔드]
    B --> DB[(일기와 분석 결과 저장)]
    B --> A[AI 분석 서버]
    A --> G[Gemini AI]
```

- **웹 화면**: 사용자가 일기를 쓰고 분석 결과를 보는 화면입니다.
- **AI Diary 백엔드**: 로그인, 일기 저장, 조회처럼 서비스의 중심 역할을 합니다.
- **데이터베이스**: 일기와 감정 분석 결과를 저장합니다.
- **AI 분석 서버**: 일기 내용을 Gemini AI에 전달하고 분석 결과를 받습니다.

## 기술 스택

| 구분 | 사용 기술 | 역할 |
| --- | --- | --- |
| Frontend | React, TypeScript, Vite | 사용자가 보는 웹 화면 |
| Backend | Java 17, Spring Boot | 로그인, 일기, 감정 데이터 API |
| Database | MySQL | 사용자·일기·감정 분석 결과 저장 |
| AI | Gemini API, FastAPI, Python | 감정 분석과 공감 피드백 생성 |
| Authentication | JWT, BCrypt | 로그인 상태 확인과 비밀번호 암호화 |
| Documentation | Swagger / OpenAPI | API 사용 설명서와 테스트 화면 |
| CI/CD | GitHub Actions, Self-hosted Runner | 자동 테스트·웹 빌드 확인과 EC2 자동 배포 |
| Deployment | AWS EC2, Docker Compose, Nginx | 컨테이너 기반 서비스 배포와 웹 요청 연결 |

## API 문서

백엔드 서버를 실행한 뒤 아래 주소에서 API 목록과 요청·응답 예시를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger는 프론트엔드와 백엔드가 주고받는 약속(API)을 한곳에서 확인하고 직접 테스트하는 화면입니다. 로그인 API로 받은 JWT를 `Authorize`에 입력하면, 로그인해야 사용할 수 있는 일기 API도 테스트할 수 있습니다.

## 주요 API

모든 일기·감정·개인 기록 API는 로그인 토큰이 필요합니다.

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 및 인증 토큰 발급 |
| POST | `/api/diaries` | 일기 작성 |
| GET | `/api/diaries?page=0&size=10` | 일기 목록 조회 |
| GET | `/api/diaries?keyword=카페&emotion=JOY` | 키워드·감정 조건으로 일기 검색 |
| GET | `/api/diaries/{diaryId}` | 일기 상세 조회 |
| DELETE | `/api/diaries/{diaryId}` | 일기 삭제(복구 가능한 소프트 삭제) |
| GET | `/api/diaries/on-this-day` | 정확히 1년 전 오늘의 일기 조회 |
| POST | `/api/diaries/{diaryId}/analyses` | AI 감정 분석 실행 |
| GET | `/api/diaries/{diaryId}/analyses/latest` | 최신 감정 분석 결과 조회 |
| GET | `/api/emotions/trends?days=7` | 최근 7일 또는 30일 감정 변화 조회 |
| GET | `/api/users/me/streak` | 연속 일기 작성일 조회 |

## 구현하며 고민한 점

### 일기 저장과 AI 분석을 분리했습니다

AI 분석에는 몇 초가 걸리거나 일시적으로 실패할 수 있습니다. 그래서 일기는 먼저 저장하고, AI 분석은 별도 요청으로 실행하도록 만들었습니다. AI 분석에 문제가 생겨도 사용자의 일기가 사라지지 않습니다.

### 삭제한 일기는 통계에서 제외합니다

일기는 바로 DB에서 지우지 않고 삭제된 시각을 기록합니다. 이를 소프트 삭제라고 합니다. 사용자가 삭제한 일기가 감정 그래프나 연속 기록 계산에 계속 포함되지 않도록 모든 조회 조건에 반영했습니다.

### 감정은 대표값 하나보다 5가지 점수를 함께 저장합니다

대표 감정만 저장하면 "기쁨" 또는 "불안"처럼 한 가지 결과만 남습니다. AI Diary는 5가지 감정 점수를 함께 저장해 날짜별 변화 그래프, 기간 평균, 대표 감정을 보여줄 수 있도록 설계했습니다.

### 연속 기록은 오늘 또는 어제부터 계산합니다

오늘 아직 일기를 쓰지 않았더라도 어제까지 연속 기록했다면, 기록이 바로 끊겼다고 보지 않습니다. 오늘 또는 어제부터 하루씩 거꾸로 확인해 연속 일수를 계산합니다. 같은 날 일기를 여러 번 작성해도 하루로 계산하며, 삭제한 일기는 제외합니다.

### 검색은 최신 AI 분석 결과를 기준으로 합니다

같은 일기를 다시 분석할 수 있기 때문에 분석 결과가 여러 개 쌓일 수 있습니다. 감정 필터에서는 가장 최근 분석 결과를 기준으로 일기를 찾도록 구현했습니다.

## 로컬에서 실행하기

### 가장 간단한 방법: Docker로 API 서버 실행하기

Docker Desktop이 설치되어 있다면 DB, 백엔드, AI 분석 서버를 한 번에 실행할 수 있습니다.

```bash
cp .env.example .env
cp gemini-experiment/.env.example gemini-experiment/.env
```

생성된 두 환경설정 파일에 DB 비밀번호, JWT Secret, Gemini API Key를 입력한 뒤 실행합니다. 이 파일에는 비밀번호와 API 키가 들어가므로 GitHub에 올리면 안 됩니다.

```bash
docker compose up --build -d
```

정상 실행 여부는 아래 명령으로 확인합니다.

```bash
curl http://localhost:8080/api/health
```

`{"status":"ok"}`가 나오면 성공입니다. 로컬 Docker Compose는 DB·백엔드·AI 서버를 실행하며, 웹 화면은 아래 4단계처럼 별도로 실행합니다.

### 준비물

- Java 17
- MySQL
- Node.js 20 이상
- Python 3.9 이상
- Gemini API Key

### 직접 실행하는 방법

Docker를 사용하지 않는 경우 아래 순서대로 실행합니다.

### 1. 데이터베이스 만들기

```sql
CREATE DATABASE aidiary
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'aidiary_app'@'localhost' IDENTIFIED BY 'YOUR_DATABASE_PASSWORD';
GRANT ALL PRIVILEGES ON aidiary.* TO 'aidiary_app'@'localhost';
FLUSH PRIVILEGES;
```

### 2. AI 분석 서버 실행하기

`gemini-experiment/.env` 파일을 만들고 API 키를 설정합니다.

```env
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
GEMINI_MODEL=gemini-3.5-flash-lite
```

```bash
cd gemini-experiment
python3 -m pip install -r requirements.txt
python3 -m uvicorn app:app --port 8000
```

### 3. Spring Boot 백엔드 실행하기

IntelliJ 실행 설정 또는 터미널 환경변수에 아래 값을 설정합니다.

```text
DB_USERNAME=aidiary_app
DB_PASSWORD=YOUR_DATABASE_PASSWORD
JWT_SECRET=LONG_RANDOM_SECRET
GEMINI_API_BASE_URL=http://127.0.0.1:8000
```

```bash
./gradlew bootRun
```

백엔드는 `http://localhost:8080`에서 실행됩니다.

### 4. 웹 화면 실행하기

```bash
cd frontend
npm ci
npm run dev
```

웹 화면은 `http://localhost:5173`에서 실행됩니다.

## AWS EC2 배포와 자동 배포

로컬에서만 동작하던 서비스를 AWS EC2에 Docker Compose로 배포했습니다. 배포 환경에서는 프론트엔드, 백엔드, AI 분석 서버, 데이터베이스를 각각 컨테이너로 실행합니다.

```mermaid
flowchart LR
    U[사용자 브라우저] -->|HTTP 80| N[Nginx / React]
    N -->|/api 요청 전달| B[Spring Boot]
    B --> DB[(MySQL)]
    B --> A[FastAPI AI 서버]
    A --> G[Gemini API]
```

- 외부에는 **HTTP 80번 포트만** 공개합니다.
- MySQL, Spring Boot, FastAPI는 Docker 네트워크 안에서만 서로 통신합니다.
- Nginx가 React 화면을 제공하고, `/api` 요청만 Spring Boot로 전달합니다.
- 비밀번호, JWT 키, Gemini API 키는 서버의 `.env` 파일에만 두며 GitHub에 커밋하지 않습니다.

### EC2에서 서비스 실행하기

EC2에 SSH로 접속한 뒤 아래 명령으로 최신 코드를 받아 컨테이너를 빌드·실행합니다.

```bash
cd ~/ai-diary
git pull origin main
docker compose -f docker-compose.prod.yml up -d --build
```

서비스 상태와 백엔드 연결은 아래처럼 확인합니다.

```bash
docker compose -f docker-compose.prod.yml ps
curl http://localhost/api/health
```

서버를 사용하지 않을 때는 EC2 인스턴스를 **중지**해 컴퓨팅 비용을 줄입니다. 다시 시작하면 공인 IP가 변경될 수 있으므로 AWS 콘솔에서 새 주소를 확인합니다.

### main에 머지하면 자동으로 배포됩니다

GitHub Actions가 Pull Request와 push를 확인합니다. 특히 `main` 브랜치에 코드가 합쳐지면 EC2에 설치한 GitHub Actions runner가 다음 과정을 자동으로 수행합니다.

```text
main 브랜치 머지
      ↓
백엔드 테스트 · 프론트엔드 빌드 확인
      ↓
EC2에서 최신 코드 받기
      ↓
Docker 컨테이너 다시 빌드·실행
      ↓
/api/health 응답 확인
```

배포 과정 중 문제가 생기면 GitHub Actions 실행 화면에서 어느 단계에서 멈췄는지 확인할 수 있습니다.

## 코드 구조

```text
.
├── frontend/                    # 사용자가 보는 React 웹 화면
├── gemini-experiment/           # Gemini를 호출하는 FastAPI 서버
├── src/main/java/com/aidiary/
│   ├── domain/
│   │   ├── user/                # 회원가입, 로그인, 연속 기록
│   │   ├── diary/               # 일기 작성·조회·검색·삭제
│   │   └── emotion/             # AI 분석 결과와 감정 트렌드
│   └── global/                  # JWT, 보안, 예외 처리, Swagger 설정
└── .github/workflows/ci.yml     # 자동 빌드 검사
```

## 테스트와 코드 품질 확인

GitHub Actions는 `main`과 `feature/**` 브랜치에 코드가 올라오거나 Pull Request를 만들 때 자동으로 실행됩니다.

```text
CI
├── Backend: 회원가입·로그인·일기·감정 분석 통합 테스트
├── Frontend: TypeScript 검사 및 웹 빌드
└── main 머지 시: EC2 자동 배포와 헬스체크
```

백엔드 테스트에서는 실제 Gemini API를 호출하지 않고 가짜 응답을 사용합니다. 따라서 테스트 비용과 외부 API 상태에 영향을 받지 않으면서, 감정 분석 결과 저장과 오류 처리를 검증할 수 있습니다.

## 역할 분담

| 담당 | 주요 업무 |
| --- | --- |
| 찬웅 | Gemini 프롬프트 설계, FastAPI AI 서버, AI 응답 품질 검증 |
| 민정 | Spring Boot API·DB, JWT 인증, 감정 트렌드, 프론트 연동, CI·배포 준비 |

## 현재 상태와 향후 개선 방향

완료한 항목입니다.

- 회원가입·로그인, JWT 인증, 일기 CRUD, 검색, 감정 분석과 트렌드 구현
- Swagger API 문서화
- 백엔드 통합 테스트와 GitHub Actions CI 구성
- Docker Compose 기반 로컬·EC2 실행 환경 구성
- `main` 머지 시 EC2로 자동 배포되는 CD 구성

더 발전시키고 싶은 항목입니다.

- 도메인 연결과 HTTPS 적용
- 사용자 동의를 받는 AI 분석 설정 추가
- 주간 감정 리포트 생성 기능
- AI 분석을 비동기로 처리해 사용자가 기다리는 시간 줄이기
- 공개 데이터셋 기반 경량 감정 분류 모델과 Gemini 결과 비교

## 보안과 개인정보

- Gemini API Key, DB 비밀번호, JWT Secret은 저장소에 올리지 않습니다.
- 사용자 일기와 계정 정보는 로그인한 본인만 조회할 수 있도록 인증을 적용했습니다.
- AI 감정 분석을 위해 일기 본문이 Gemini API로 전달될 수 있으므로, 실제 서비스 배포 전에는 사용자에게 이를 명확히 안내하고 동의 설정을 추가할 예정입니다.
