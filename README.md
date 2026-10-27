# Trading Alert Receiver

Ứng dụng Android nhận cảnh báo giao dịch qua Firebase Cloud Messaging (FCM), hiển thị âm thanh/ảnh và lưu 50 thông báo gần nhất trong Room database.

> App không dùng `google-services.json`. Firebase Android client config và topic được lấy từ backend qua `GET /config`. Service-account private key chỉ được lưu ở backend.

## Luồng hoạt động

```text
TradingView → Cloudflare Worker/backend → Firebase FCM → Android app
```

1. App gọi `GET /config` để lấy Firebase client config và topic.
2. App khởi tạo Firebase, lấy FCM token và subscribe topic.
3. TradingView gửi webhook đến backend.
4. Backend gửi FCM HTTP v1 đến topic.
5. App hiển thị và lưu thông báo.

Lần cài mới phải mở app ít nhất một lần để tải cấu hình. Sau đó app sử dụng bản cache khi process được khởi động lại.

## Build ứng dụng

Đặt URL backend trong `gradle.properties`:

```properties
WORKER_BASE_URL=https://alerts.example.workers.dev
```

Build và kiểm tra:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat assembleRelease
```

Package Android bắt buộc:

```text
com.tradingalert.receiver
```

## Contract backend

Backend có thể dùng Cloudflare Worker hoặc nền tảng khác, nhưng phải hỗ trợ contract dưới đây.

### Lấy cấu hình Firebase

```http
GET /config
Accept: application/json
```

Response `200 OK`:

```json
{
  "topic": "trading_alerts",
  "firebase": {
    "androidPackageName": "com.tradingalert.receiver",
    "projectId": "example-project",
    "applicationId": "1:1234567890:android:abcdef123456",
    "apiKey": "example-android-api-key",
    "gcmSenderId": "1234567890",
    "storageBucket": "example-project.firebasestorage.app"
  }
}
```

Yêu cầu:

- Endpoint phải dùng HTTPS.
- `androidPackageName` phải bằng `com.tradingalert.receiver`.
- `projectId`, `applicationId`, `apiKey`, `gcmSenderId` và `topic` không được trống.
- `storageBucket` có thể để trống.
- Topic phải khớp `^[a-zA-Z0-9-_.~%]{1,900}$`.
- Response tối đa 8 KiB; timeout phía app là 5 giây.
- Khi topic thay đổi, app tự subscribe topic mới và bỏ topic cũ.

Các giá trị trên là Firebase client config công khai. Không trả `client_email`, `private_key`, service-account JSON hoặc OAuth token về app.

### Nhận webhook TradingView

Endpoint đề xuất:

```http
POST /webhooks/tradingview
Content-Type: application/json
```

Payload mẫu:

```json
{
  "secret": "WEBHOOK_SECRET_VALUE",
  "eventId": "{{ticker}}-{{timenow}}-{{strategy.order.id}}",
  "symbol": "{{ticker}}",
  "action": "{{strategy.order.action}}",
  "price": "{{strategy.order.price}}",
  "quantity": "{{strategy.order.contracts}}",
  "time": "{{timenow}}",
  "image": "https://cdn.example.com/trading-alert.jpg"
}
```

Backend phải xác thực webhook secret, chống gửi trùng bằng `eventId` và phản hồi nhanh. IP allowlist và giới hạn body là cấu hình vận hành của backend, không phải hằng số của app.

### Gửi FCM

Gửi bằng Firebase HTTP v1:

```http
POST https://fcm.googleapis.com/v1/projects/FIREBASE_PROJECT_ID/messages:send
Authorization: Bearer OAUTH2_ACCESS_TOKEN
Content-Type: application/json
```

Payload khuyến nghị:

```json
{
  "message": {
    "topic": "trading_alerts",
    "notification": {
      "title": "BTCUSDT • BUY",
      "body": "@70500 • × 0.01",
      "image": "https://cdn.example.com/trading-alert.jpg"
    },
    "data": {
      "title": "BTCUSDT • BUY",
      "body": "@70500 • × 0.01",
      "reference": "order-42",
      "image": "https://cdn.example.com/trading-alert.jpg"
    },
    "android": {
      "priority": "HIGH",
      "ttl": "60s",
      "notification": {
        "channel_id": "trading_alerts_v5",
        "sound": "default"
      }
    }
  }
}
```

Mọi giá trị trong `data` phải là chuỗi. App hỗ trợ `title`, `body`, `reference` và các key ảnh `image`, `image_url`, `imageUrl`, `picture`.

`trading_alerts_v5` là Android notification channel, không phải FCM topic.

## Cấu hình backend

| Biến | Mục đích |
| --- | --- |
| `FIREBASE_PROJECT_ID` | Firebase project dùng để gửi FCM |
| `FIREBASE_CLIENT_EMAIL` | Service-account email |
| `FIREBASE_PRIVATE_KEY` | Service-account private key |
| `FIREBASE_ANDROID_APP_ID` | Android Firebase application ID |
| `FIREBASE_ANDROID_API_KEY` | Android Firebase client API key |
| `FIREBASE_SENDER_ID` | Firebase project number / sender ID |
| `FIREBASE_STORAGE_BUCKET` | Storage bucket, có thể để trống |
| `ANDROID_PACKAGE_NAME` | Phải là `com.tradingalert.receiver` |
| `FCM_TOPIC` | Topic trả về app và dùng để gửi FCM |
| `WEBHOOK_SECRET` | Xác thực webhook TradingView |

`FIREBASE_CLIENT_EMAIL`, `FIREBASE_PRIVATE_KEY` và `WEBHOOK_SECRET` phải được lưu bằng secret manager, không commit vào Git.

Mã Cloudflare Worker trong [`worker/`](worker/) chỉ là triển khai tham khảo để người làm backend hoàn thiện sau.

## Kiểm tra tích hợp

1. `GET /config` trả đúng Firebase project, package và topic.
2. Cài app, mở một lần và cấp quyền thông báo.
3. App hiển thị trạng thái `LIVE`.
4. Gửi webhook thử.
5. Kiểm tra notification, âm thanh, ảnh và lịch sử trong app.
6. Thử lại khi app chạy nền.

Nếu người dùng bấm **Force stop** trong Android Settings, Android có thể ngừng giao FCM cho đến khi app được mở lại.

## Tài liệu chính thức

- [Firebase HTTP v1](https://firebase.google.com/docs/cloud-messaging/send/v1-api)
- [Nhận FCM trên Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)
- [Cloudflare Workers secrets](https://developers.cloudflare.com/workers/configuration/secrets/)
- [TradingView webhook alerts](https://www.tradingview.com/support/solutions/43000529348-how-to-configure-webhook-alerts/)
