# SNS API 서버

![Kotlin](https://img.shields.io/badge/kotlin-1.9.20-blue.svg)
![Ktor](https://img.shields.io/badge/ktor-2.3.6-orange.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14-blue.svg)
![MongoDB](https://img.shields.io/badge/MongoDB-5.0-green.svg)

**Ktor 기반의 소셜 네트워크 서비스 RESTful API 서버**

## 프로젝트 개요

본 프로젝트는 Kotlin과 Ktor 프레임워크를 활용한 SNS 앱 백엔드 API 서버입니다. 사용자 관리, 게시글, 댓글, 좋아요, 팔로우, 실시간 채팅, 파일 업로드 등 SNS의 핵심 기능을 RESTful API로 제공합니다.

## 주요 기능

### 사용자 관리
- 회원 가입 및 로그인 (JWT 인증)
- 소셜 로그인 지원 (Google, Kakao, Naver)
- 프로필 정보 조회 및 수정
- 팔로우/언팔로우
- 사용자 검색 및 최근 검색 기록 관리

### 게시글
- 게시글 작성, 조회, 수정, 삭제
- 이미지 첨부 기능
- 좋아요 기능
- 저장(북마크) 기능
- 내 게시글/저장한 게시글 목록 조회

### 댓글
- 댓글 작성 및 삭제
- 대댓글(답글) 기능
- 댓글에 멘션 기능

### 파일 관리
- 이미지 파일 업로드 (MongoDB GridFS)
- 파일 다운로드 및 삭제

### 실시간 채팅
- WebSocket 기반 실시간 채팅
- 채팅방 생성 및 관리
- 채팅 메시지 저장 및 조회
- 읽음 표시

### 알림
- FCM(Firebase Cloud Messaging) 푸시 알림
- 활동 알림 (좋아요, 댓글, 팔로우 등)
- 알림 목록 조회 및 읽음 처리

## 주요 특징

### 비동기 프로그래밍
- Kotlin Coroutines를 활용한 비동기 로직 구현
- 효율적인 데이터베이스 트랜잭션 처리

### 실시간 기능
- WebSocket을 이용한 실시간 양방향 통신 구현
- FCM을 통한 푸시 알림 시스템

### 보안
- JWT 기반 인증 시스템
- BCrypt를 이용한 안전한 비밀번호 해싱
- CORS 설정을 통한 API 보안

### 확장성
- 모듈화된 구조로 새로운 기능 추가 용이
- Koin을 통한 의존성 주입으로 유지보수 및 테스트 용이성 확보

### 에러 처리
- 명확한 에러 코드와 메시지 체계
- 전역 예외 처리 구현

## 기술 스택

### 백엔드 프레임워크
- **[Ktor](https://ktor.io/)**: 경량화된 Kotlin 웹 프레임워크
- **[Koin](https://insert-koin.io/)**: 의존성 주입 프레임워크

### 데이터베이스
- **[PostgreSQL](https://www.postgresql.org/)**: 관계형 데이터베이스
- **[Exposed](https://github.com/JetBrains/Exposed)**: Kotlin SQL 프레임워크 (ORM)
- **[MongoDB](https://www.mongodb.com/)**: 파일 저장 및 채팅 메시지 저장
- **[HikariCP](https://github.com/brettwooldridge/HikariCP)**: JDBC 커넥션 풀

### 인증 및 보안
- **[JWT](https://jwt.io/)**: 토큰 기반 인증
- **[BCrypt](https://github.com/patrickfav/bcrypt)**: 비밀번호 암호화

### 파일 관리
- **[MongoDB GridFS](https://www.mongodb.com/ko-kr/docs/manual/core/gridfs/)**: 파일 저장 시스템

### 실시간 기능
- **[WebSockets](https://ktor.io/docs/websocket.html)**: 실시간 채팅
- **[Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)**: 푸시 알림

### 기타
- **[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)**: JSON 직렬화/역직렬화
- **[Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines)**: 비동기 프로그래밍
- **[Swagger/OpenAPI](https://swagger.io/)**: API 문서화

## 아키텍처

본 프로젝트는 다음과 같은 구조로 설계되었습니다:

```
com.ninezero
├── exception/        # 예외 처리 클래스
├── models/           # 데이터 모델
│   ├── dto/          # 데이터 전송 객체
│   ├── error/        # 에러 코드 및 메시지
│   ├── mappers/      # 모델 변환 매퍼
│   └── tables/       # 데이터베이스 테이블 정의
├── plugins/          # Ktor 플러그인 설정
├── routes/           # API 라우팅
├── services/         # 비즈니스 로직
│   └── social/       # 소셜 로그인 관련 서비스
└── utils/            # 유틸리티 함수
```

## API

### 사용자 API
- `POST /api/users` - 회원 가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/social-login` - 소셜 로그인
- `GET /api/users/{id}` - 프로필 조회
- `PUT /api/users/{id}` - 프로필 수정
- `POST /api/users/{id}/follow` - 팔로우
- `DELETE /api/users/{id}/follow` - 언팔로우
- `GET /api/users/search` - 사용자 검색

### 게시글 API
- `POST /api/boards` - 게시글 작성
- `GET /api/boards` - 게시글 목록 조회
- `GET /api/boards/my` - 내 게시글 조회
- `GET /api/boards/saved` - 저장한 게시글 조회
- `PUT /api/boards/{id}` - 게시글 수정
- `DELETE /api/boards/{id}` - 게시글 삭제
- `POST /api/boards/{id}/like` - 좋아요
- `DELETE /api/boards/{id}/like` - 좋아요 취소
- `POST /api/boards/{id}/save` - 게시글 저장
- `DELETE /api/boards/{id}/save` - 저장 취소

### 댓글 API
- `POST /api/boards/{id}/comments` - 댓글 작성
- `GET /api/boards/{id}/comments` - 댓글 목록 조회
- `GET /api/boards/{id}/comments/{parentId}/replies` - 대댓글 조회
- `DELETE /api/boards/{id}/comments/{commentId}` - 댓글 삭제

### 파일 API
- `POST /api/files/upload` - 파일 업로드
- `GET /api/files/{id}` - 파일 다운로드

### 채팅 API
- `POST /api/chat/rooms` - 채팅방 생성
- `GET /api/chat/rooms` - 채팅방 목록 조회
- `GET /api/chat/rooms/{roomId}/messages` - 채팅 메시지 조회
- `WS /api/chat/ws` - WebSocket 연결
- `DELETE /api/chat/rooms/{roomId}` - 채팅방 나가기

### 알림 API
- `POST /api/notifications/token` - 디바이스 토큰 등록
- `GET /api/notifications` - 알림 목록 조회
- `PATCH /api/notifications/{id}` - 알림 읽음 처리
- `DELETE /api/notifications/{id}` - 알림 삭제

## 설치 및 실행

### 필수 요구사항
```
- JDK 11 이상
- PostgreSQL 12 이상
- MongoDB 4.4 이상
- Firebase Admin SDK 설정
```

### 필수 환경 변수
```
JWT_SECRET_KEY=your_jwt_secret_key
MONGODB_URI=mongodb://username:password@host:port
DB_URL=jdbc:postgresql://host:port/dbname
DB_USER=your_db_username
DB_PASSWORD=your_db_password
```

### 빌드 및 실행

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`              | 테스트 실행                                                        |
| `./gradlew build`             | 빌드                                                     |
| `buildFatJar`                 | 실행 가능한 JAR 파일 생성 |
| `run`                         | 서버 실행                                                       |

성공적으로 서버가 시작되면 다음과 같은 출력이 표시됩니다:
```
INFO  Application - Application started in 0.303 seconds.
INFO  Application - Responding at http://0.0.0.0:8080
```

## API 문서

API 문서는 SwaggerUI를 통해 제공됩니다:
```
http://localhost:8080/api-docs
```
