# AI 콜백 시스템 (OCI Streaming + WebSocket)

## 개요

이 시스템은 AI 서비스의 비동기 콜백을 OCI Streaming(Kafka)을 통해 받아서 모든 Pod에 브로드캐스팅하고, 해당 WebSocket 세션이 있는 Pod에서만 클라이언트에게 메시지를 전송하는 구조입니다.

**기존 AnalysisService와 통합되어 레거시 방식과 새로운 Kafka 기반 방식을 모두 지원합니다.**

## 시스템 구조

```
AI Service → OCI Streaming (Kafka) → All Pods → WebSocket Client
                                        ↓
                                   (세션 확인)
                                        ↓
                                   해당 Pod만 응답
```

## 주요 컴포넌트

### 1. DTO 클래스
- `AiCallbackDto`: AI 콜백 데이터 구조
- `CallbackType`: 콜백 타입 enum (QUESTION, ANALYSIS_COMPLETE, DIGEST_COMPLETE, ERROR)

### 2. 서비스 클래스
- `AiCallbackConsumerService`: Kafka에서 콜백을 수신하고 처리
- `AiCallbackProducerService`: Kafka로 콜백을 전송 (테스트용)
- `AiCallbackDataService`: 콜백 데이터 저장 및 관리
- **`AnalysisService`**: 기존 분석 서비스와 통합 (권장)

### 3. WebSocket 핸들러
- `DiaryWebSocketHandler`: WebSocket 세션 관리 및 메시지 전송

### 4. 설정 클래스
- `KafkaConfig`: Kafka Consumer/Producer 설정

## 사용 방법

### 1. 분석 세션 준비 (권장)

**기존 AnalysisService 사용:**
```java
@Autowired
private AnalysisService analysisService;

// 세션 준비
analysisService.prepareAnalysisSession(diaryId);
```

**또는 REST API:**
```bash
POST /core/analysis/prepare-session/{diaryId}
```

### 2. AI 분석 콜백 처리

**Kafka 기반 (권장 - 멀티 Pod 환경):**
```bash
POST /core/analysis/callback/{diaryId}
Content-Type: application/json

{
    "images": [...],
    "questions": [...],
    "overallDaySummary": "..."
}
```

**직접 WebSocket (레거시 지원):**
```bash
POST /core/analysis/callback-direct/{diaryId}
```

### 3. WebSocket 연결 및 구독

클라이언트에서 WebSocket 연결:
```javascript
const socket = new SockJS('/core/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // 특정 일기 ID에 대한 구독
    stompClient.subscribe('/topic/diary/{diaryId}', function(message) {
        const response = JSON.parse(message.body);
        console.log('받은 메시지:', response);
    });
});
```

## API 엔드포인트

### Analysis 서비스 (기존 + 신규)

#### 세션 준비
```bash
POST /core/analysis/prepare-session/{diaryId}
```

#### 분석 처리 (기존)
```bash
POST /core/analysis
```

#### Kafka 기반 콜백 (권장)
```bash
POST /core/analysis/callback/{diaryId}
```

#### 직접 WebSocket 콜백 (레거시)
```bash
POST /core/analysis/callback-direct/{diaryId}
```

### Image 서비스 (통합 완료)

#### 이미지 업로드 세션 생성
```bash
POST /core/images/session
```

#### 이미지 분석 세션 준비 (자동으로 AI 세션도 준비)
```bash
GET /core/images/session/{diaryId}
# → READ PAR 생성 → OCI Queue 발행 → AI 분석 세션 준비
```

### 테스트 API

#### Analysis 세션 준비 (권장)
```bash
POST /core/test/ai-callback/prepare-analysis-session/{diaryId}
```

#### 기본 세션 준비
```bash
POST /core/test/ai-callback/prepare-session/{diaryId}
```

#### 기타 콜백 테스트
```bash
# 질문 콜백
POST /core/test/ai-callback/send-question/{diaryId}

# 분석 완료 콜백
POST /core/test/ai-callback/send-analysis-complete/{diaryId}

# 다이제스트 완료 콜백
POST /core/test/ai-callback/send-digest-complete/{diaryId}

# 에러 콜백
POST /core/test/ai-callback/send-error/{diaryId}

# 세션 상태 확인
GET /core/test/ai-callback/session-status/{diaryId}
```

## 메시지 타입

WebSocket으로 전송되는 메시지 구조:
```json
{
    "type": "QUESTION|ANALYSIS_COMPLETE|DIGEST_COMPLETE|ERROR",
    "content": "메시지 내용 (optional)"
}
```

## 동작 흐름 (권장)

### 전체 프로세스 (이미지 업로드부터 분석까지)

1. **이미지 업로드**: `POST /core/images/session` → 업로드 PAR 생성
2. **클라이언트**: 생성된 PAR로 이미지 업로드
3. **분석 준비**: `GET /core/images/session/{diaryId}` → READ PAR 생성 + OCI Queue 발행 + **AI 세션 자동 준비**
4. **AI 분석**: AI 서비스가 Queue에서 메시지 수신 후 분석 시작
5. **Kafka 콜백**: AI 서비스에서 `/core/analysis/callback/{diaryId}`로 결과 전송
6. **브로드캐스팅**: Kafka를 통해 모든 Pod에 콜백 전송
7. **세션 확인**: 각 Pod에서 해당 diaryId의 WebSocket 세션 존재 여부 확인
8. **메시지 전송**: 세션이 있는 Pod에서만 클라이언트에게 메시지 전송
9. **세션 정리**: 처리 완료 후 자동 세션 정리

### 수동 세션 준비 (필요시)

1. **세션 준비**: `analysisService.prepareAnalysisSession(diaryId)` 호출
2. **AI 처리 시작**: AI 서비스에서 분석 시작
3. **Kafka 콜백**: AI 서비스에서 `/core/analysis/callback/{diaryId}`로 결과 전송
4. **브로드캐스팅**: Kafka를 통해 모든 Pod에 콜백 전송
5. **세션 확인**: 각 Pod에서 해당 diaryId의 WebSocket 세션 존재 여부 확인
6. **메시지 전송**: 세션이 있는 Pod에서만 클라이언트에게 메시지 전송
7. **세션 정리**: 처리 완료 후 자동 세션 정리

## 설정

### application.yml
```yaml
spring:
  kafka:
    bootstrap-servers: "${OCI_STREAM_BROKERS}"
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: OCI
      sasl.jaas.config: |
        org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required
        username="${OCI_STREAM_POOL_OCID}"
        password="instance_principal";
    consumer:
      group-id: diary-callback-consumers
      auto-offset-reset: earliest

app:
  kafka:
    topic:
      ai-callback: "ai-callback"
```

## 기존 코드와의 호환성

### 통합된 서비스들

**AnalysisService 메서드:**
- `processAnalysis()`: 기존 분석 처리 로직 (변경 없음)
- `handleAnalysisCallback()`: **새로운 Kafka 기반 콜백 처리 (권장)**
- `handleAnalysisCallbackDirect()`: **레거시 직접 WebSocket 처리**
- `prepareAnalysisSession()`: **세션 준비 (신규)**

**ImageController 통합:**
- `GET /core/images/session/{diaryId}`: **자동으로 AI 분석 세션도 준비** (신규)
- 기존 이미지 업로드 프로세스와 완전 통합
- WebSocket 세션 준비가 자동화됨

### 마이그레이션 가이드

**이미지 업로드 + 분석 프로세스:**

**Before (기존):**
```java
// 1. 이미지 업로드
POST /core/images/session → 업로드

// 2. 분석 준비
GET /core/images/session/{diaryId}  // 직접 webSocketHandler.prepareSession()

// 3. AI 콜백
POST /core/analysis/callback/{diaryId}  // 기존 방식
```

**After (현재 - 자동 통합):**
```java
// 1. 이미지 업로드
POST /core/images/session → 업로드

// 2. 분석 준비 (자동으로 AI 세션도 준비됨)
GET /core/images/session/{diaryId}  // analysisService.prepareAnalysisSession() 자동 호출

// 3. AI 콜백 (동일한 엔드포인트, 새로운 Kafka 기반 처리)
POST /core/analysis/callback/{diaryId}  // Kafka 기반 브로드캐스팅
```

**수동 분석만 하는 경우:**

**Before (기존):**
```java
// 직접 WebSocket 전송
POST /core/analysis/callback/{diaryId}  // 기존 방식
```

**After (권장):**
```java
// 1. 세션 준비
analysisService.prepareAnalysisSession(diaryId);

// 2. Kafka 기반 콜백 (동일한 엔드포인트)
POST /core/analysis/callback/{diaryId}  // 새로운 Kafka 기반 처리

// 3. 레거시 지원 (필요시)
POST /core/analysis/callback-direct/{diaryId}
```

## 주의사항

1. **세션 준비**: AI 처리 시작 전에 반드시 `prepareAnalysisSession()` 호출 필요
2. **메모리 관리**: 콜백 히스토리는 메모리에 저장되므로 주기적 정리 필요
3. **에러 처리**: Kafka 메시지 처리 실패 시에도 acknowledge하여 무한 재시도 방지
4. **동시성**: ConcurrentHashMap 사용으로 멀티스레드 환경에서 안전
5. **호환성**: 기존 방식(`callback-direct`)과 새로운 방식(`callback`) 모두 지원

## 확장 가능성

1. **Redis 연동**: 메모리 기반 세션 관리를 Redis로 확장
2. **DB 저장**: 콜백 히스토리를 데이터베이스에 영구 저장
3. **메트릭스**: 콜백 처리 성능 모니터링 추가
4. **재시도 로직**: 실패한 콜백에 대한 재시도 메커니즘 추가 