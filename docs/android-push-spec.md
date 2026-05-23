# Fresh Kitchen — 푸시 알림 연동 명세 (Android)

## 1. 개요

서버는 Firebase Cloud Messaging(FCM)을 통해 사용자의 안드로이드 디바이스에 푸시 알림을 발송한다.
첫 번째 알림 종류는 **유통기한 임박 알림**이며, 매일 오전 9시(KST)에 사용자별 임박 식재료를 묶어 발송한다.

알림이 도착·표시·탭 처리되는 모든 시점은 **클라이언트(앱) 책임**이며, 서버는 토큰 관리와 발송까지만 담당한다.

---

## 2. 전체 플로우

```
[앱]                                      [서버]                          [FCM]
  │  로그인 (dev-login/oauth)               │                                │
  │ ─────────────────────────────────────► │                                │
  │  accessToken                           │                                │
  │ ◄───────────────────────────────────── │                                │
  │                                        │                                │
  │  FirebaseMessaging.getToken()          │                                │
  │  → fcmToken                            │                                │
  │                                        │                                │
  │  POST /api/v1/users/me/fcm-tokens      │                                │
  │  { tokenValue, deviceType:"ANDROID" }  │                                │
  │ ─────────────────────────────────────► │                                │
  │  201 Created                           │                                │
  │ ◄───────────────────────────────────── │                                │
  │                                        │                                │
  │                                        │  매일 09:00 KST 스케줄러         │
  │                                        │  expiring ingredient 조회       │
  │                                        │  사용자별 메시지 빌드            │
  │                                        │  sendEach(token, message)      │
  │                                        │ ─────────────────────────────► │
  │                                        │  성공/실패 응답                 │
  │                                        │ ◄───────────────────────────── │
  │                                        │  무효 토큰 DB 삭제              │
  │                                        │                                │
  │  알림 도착 (notification + data)         │                                │
  │ ◄──────────────────────────────────────────────────────────────────── │
  │  사용자 탭                                                              │
  │  PendingIntent 실행                                                     │
  │  → MainActivity onNewIntent()                                          │
  │  → data.type 기준 화면 라우팅                                            │
```

---

## 3. FCM 토큰 등록 API

### 3.1 등록 시점 (둘 다 필요)

| 시점 | 이유 |
|---|---|
| **로그인 성공 직후** | 첫 로그인 시 토큰 등록 보장 |
| **`FirebaseMessagingService.onNewToken()` 콜백** | 앱 재설치/데이터 삭제/자동 갱신 시 새 토큰 동기화 |

서버가 업서트(같은 토큰 들어오면 user/deviceType 재할당)로 동작하므로 같은 페이로드 여러 번 호출해도 안전.

### 3.2 요청

```http
POST /api/v1/users/me/fcm-tokens
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "tokenValue": "fxxxxxxx...:APA91b...",
  "deviceType": "ANDROID"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `tokenValue` | string | ✅ | `FirebaseMessaging.getInstance().token`으로 받은 값 |
| `deviceType` | enum | ✅ | `"ANDROID"` (`IOS`/`WEB`도 정의돼 있으나 현재 안드로이드만) |

### 3.3 응답

```http
HTTP/1.1 201 Created

{ "status": 201, "code": "COMMON-201", "message": "Success", "data": null }
```

---

## 4. 푸시 메시지 페이로드 스펙

FCM 메시지에는 `notification`(시스템 알림 UI용)과 `data`(앱 라우팅용) 두 블록이 함께 들어간다.

### 4.1 Notification 블록 (표시용)

| 필드 | 예시 |
|---|---|
| `title` | `"유통기한 임박 알림"` |
| `body` | `"'시금치'의 유통기한이 오늘이에요. (외 2개 식재료)"` |

`body` 패턴:
- 1개: `"'{name}'의 유통기한이 {phrase}."`
- 2개+: `"'{name}'의 유통기한이 {phrase}. (외 {N}개 식재료)"`

`{phrase}` 규칙 (가장 임박한 식재료 기준):
- 잔여 0일 → `"오늘이에요"`
- 잔여 1일 → `"내일이에요"`
- 잔여 2일 이상 → `"{N}일 남았어요"`

### 4.2 Data 블록 (라우팅용)

| 키 | 타입 | 예시 | 설명 |
|---|---|---|---|
| `type` | string | `"INGREDIENT_EXPIRING"` | 알림 종류 식별자. 화면 라우팅의 기준 |
| `ingredientIds` | string | `"12,34,56"` | 콤마 구분 식재료 ID. 만료일 빠른 순으로 정렬됨 |
| `asOf` | string | `"2026-05-20"` | 발송 기준 날짜 (yyyy-MM-dd) |

> FCM `data` 페이로드는 **모든 값이 string**이어야 한다 (정수/배열 직접 지원 안 됨).
> `ingredientIds`는 클라이언트에서 `split(",")` 후 `toLong()`으로 파싱.

---

## 5. 알림 종류별 라우팅 규칙

| `type` | 이동 화면 | 추가 처리 |
|---|---|---|
| `INGREDIENT_EXPIRING` | 식재료 목록 화면 | `ingredientIds` 항목 강조 표시 (옵션) |

> 향후 알림 종류 추가 시 이 표에 행 추가. 미정의 `type`이 오면 홈 화면으로 기본 라우팅 권장.

---

## 6. 안드로이드 측 구현 요구사항

### 6.1 의존성

```kotlin
implementation(platform("com.google.firebase:firebase-bom:33.x.x"))
implementation("com.google.firebase:firebase-messaging-ktx")
```

### 6.2 토큰 발급 + 등록

```kotlin
// 로그인 성공 후
FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
    api.registerFcmToken(
        tokenValue = fcmToken,
        deviceType = "ANDROID"
    )
}
```

### 6.3 토큰 갱신 콜백

```kotlin
class FreshKitchenMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (TokenStore.isLoggedIn()) {
            api.registerFcmToken(token, "ANDROID")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // 포그라운드 수신 시 직접 NotificationManager로 알림 표시
        showNotification(message)
    }
}
```

```xml
<!-- AndroidManifest.xml -->
<service
    android:name=".FreshKitchenMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

### 6.4 알림 탭 시 라우팅

```kotlin
private fun showNotification(message: RemoteMessage) {
    val type = message.data["type"]
    val ingredientIds = message.data["ingredientIds"]

    val intent = Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra("notificationType", type)
        putExtra("ingredientIds", ingredientIds)
    }
    val pendingIntent = PendingIntent.getActivity(
        this, 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(message.notification?.title)
        .setContentText(message.notification?.body)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
}
```

```kotlin
// MainActivity
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleNotificationIntent(intent)
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleNotificationIntent(intent)
}

private fun handleNotificationIntent(intent: Intent) {
    when (intent.getStringExtra("notificationType")) {
        "INGREDIENT_EXPIRING" -> {
            val ids = intent.getStringExtra("ingredientIds")
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
                .orEmpty()
            navigateToIngredientList(highlightIds = ids)
        }
        else -> navigateToHome()
    }
}
```

### 6.5 알림 권한 (Android 13+)

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    val permission = Manifest.permission.POST_NOTIFICATIONS
    if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
        requestPermissions(arrayOf(permission), REQ_NOTIFICATION)
    }
}
```

### 6.6 알림 채널 등록 (Android 8+)

앱 최초 실행 시 1회 등록 필요.

```kotlin
val channel = NotificationChannel(
    CHANNEL_ID,
    "유통기한 알림",
    NotificationManager.IMPORTANCE_DEFAULT
).apply {
    description = "유통기한 임박 식재료 알림"
}
getSystemService<NotificationManager>()?.createNotificationChannel(channel)
```

---

## 7. 동작 상태별 처리

| 앱 상태 | 알림 표시 주체 | onMessageReceived 호출 |
|---|---|---|
| **포그라운드** | 앱이 직접 NotificationManager로 표시 | ✅ |
| **백그라운드** | 시스템이 `notification` 블록 기준 자동 표시 | ❌ (data 필요 시 PendingIntent extras로 접근) |
| **종료** | 시스템이 자동 표시 | ❌ |

> 포그라운드에서 알림을 띄우지 않으면 사용자가 알 수 없으므로 6.4의 `showNotification` 호출은 필수.

---

## 8. 운영 정책

| 항목 | 값 | 변경 방법 |
|---|---|---|
| 발송 시각 | 매일 09:00 KST | `FCM_EXPIRING_NOTIFICATION_CRON` |
| 조회 범위 | 오늘부터 N일 후 만료 식재료 | `FCM_EXPIRING_NOTIFICATION_DAYS_AHEAD` |
| 그룹핑 | 사용자당 1건 발송 (식재료가 여러 개여도) | — |
| 무효 토큰 처리 | FCM이 `UNREGISTERED`/`INVALID_ARGUMENT` 반환 시 DB에서 자동 삭제 | — |

---

## 9. 테스트 환경 트리거

개발 중 cron 기다리지 않고 즉시 발송하려면:

```http
POST /api/v1/dev/notifications/expiring
Authorization: Bearer {accessToken}
```

응답에 발송 결과·메시지 본문·식재료 미리보기까지 포함되어 디버깅 가능.

응답 예시:
```json
{
  "status": 200,
  "data": {
    "from": "2026-05-20",
    "until": "2026-05-27",
    "users": 1,
    "usersWithoutTokens": 0,
    "success": 1,
    "failure": 0,
    "invalid": 0,
    "messages": [
      {
        "userId": 1,
        "title": "유통기한 임박 알림",
        "body": "'시금치'의 유통기한이 오늘이에요. (외 2개 식재료)",
        "tokenCount": 1,
        "status": "SENT",
        "ingredients": [
          { "ingredientId": 5, "name": "시금치", "expiresAt": "2026-05-20", "daysLeft": 0 },
          { "ingredientId": 6, "name": "우유",   "expiresAt": "2026-05-23", "daysLeft": 3 },
          { "ingredientId": 7, "name": "계란",   "expiresAt": "2026-05-27", "daysLeft": 7 }
        ]
      }
    ]
  }
}
```

---

## 10. 변경 이력

| 날짜 | 버전 | 내용 |
|---|---|---|
| 2026-05-20 | 1.0 | 최초 작성 — INGREDIENT_EXPIRING |