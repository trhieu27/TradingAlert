# Trading Alert Receiver

Ứng dụng Android nhận cảnh báo giao dịch qua Firebase Cloud Messaging (FCM), hiển thị âm thanh/ảnh và lưu 50 thông báo gần nhất trong Room database.

> App không dùng `google-services.json`. Firebase Android client config và topic được lấy từ backend qua `GET /config`. Service-account private key chỉ được lưu trên backend.

Backend là server và repository riêng, không nằm trong project Android này. Phần dưới chỉ mô tả API contract mà backend cần cung cấp.

## Luồng hoạt động

```text
TradingView → Cloudflare Worker → Firebase FCM → Android app
```

1. App gọi `GET /config` để lấy Firebase client config và topic.
2. App khởi tạo Firebase, lấy FCM token và subscribe topic.
3. TradingView gửi webhook đến backend.
4. Backend gửi FCM HTTP v1 đến topic.
5. App hiển thị và lưu thông báo.

Lần cài mới phải mở app ít nhất một lần để tải cấu hình. Sau đó app sử dụng bản cache khi process được khởi động lại.

## Cấu hình Android frontend

Android không chứa URL backend cố định. Sau khi cài app:

1. Bấm biểu tượng Cloud trên thanh công cụ.
2. Nhập URL gốc backend, ví dụ `https://api.example.com`.
3. App kiểm tra `GET /config`, lưu URL cục bộ rồi khởi tạo Firebase và subscribe topic.

Frontend chỉ nhận/lưu thông báo và quản lý âm thanh. Frontend không giữ service account, webhook secret hoặc gọi API gửi FCM.

## Build ứng dụng

Build và kiểm tra:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat assembleRelease
```

Package Android bắt buộc:

```text
com.tradingalert.receiver
```

## API backend

### Các URL cần cung cấp

Với backend có URL `https://api.example.com`, các URL hoàn chỉnh là:

```text
Backend base URL:        https://api.example.com
Android config URL:      https://api.example.com/config
TradingView Webhook URL: https://api.example.com/webhooks/tradingview
```

- Android chỉ gọi `GET /config`.
- Trong TradingView, dán URL `/webhooks/tradingview` đầy đủ vào ô **Webhook URL**.
- Backend nhận webhook rồi tự gọi Firebase HTTP v1. App không gọi API gửi thông báo.
- Backend có thể chạy trên Cloudflare Worker hoặc server khác miễn tuân thủ contract này.

Backend thực hiện ba việc:

1. Trả Firebase Android client config và topic qua `GET /config`.
2. Nhận webhook giao dịch, xác thực và chống gửi trùng.
3. Dùng service account gửi FCM HTTP v1 tới đúng topic.

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

Ánh xạ từ Firebase client JSON:

| Response `/config` | Trường trong Firebase client JSON |
| --- | --- |
| `firebase.projectId` | `project_info.project_id` |
| `firebase.applicationId` | `client[].client_info.mobilesdk_app_id` |
| `firebase.apiKey` | `client[].api_key[].current_key` |
| `firebase.gcmSenderId` | `project_info.project_number` |
| `firebase.storageBucket` | `project_info.storage_bucket` |
| `firebase.androidPackageName` | `client[].client_info.android_client_info.package_name` |

Backend phải chọn phần tử `client[]` có package `com.tradingalert.receiver`. Nếu thiếu trường bắt buộc hoặc không tìm thấy package, trả lỗi `503` thay vì trả cấu hình không đầy đủ.

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

Response đề xuất sau khi validate và đưa sự kiện vào hàng đợi xử lý:

```http
HTTP/1.1 202 Accepted
Content-Type: application/json
```

```json
{
  "accepted": true,
  "eventId": "BTCUSDT-20261027T121600Z-order-42"
}
```

Status tối thiểu cần hỗ trợ:

| Status | Khi sử dụng |
| --- | --- |
| `202` | Payload hợp lệ và đã nhận xử lý |
| `400` | JSON hoặc dữ liệu giao dịch không hợp lệ |
| `401` | Sai hoặc thiếu webhook secret |
| `409` | `eventId` đã được xử lý, nếu backend chọn báo lỗi trùng |
| `500` | Lỗi nội bộ không xác định |

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

Backend phải gửi tới cùng topic đang trả về từ `GET /config`. Không ghi topic ở hai nơi độc lập.

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

Firebase Android client config và service-account config là hai loại dữ liệu khác nhau:

- Client config được trả qua `/config` để app tạo FCM token.
- Service-account email/private key chỉ dùng nội bộ backend để lấy OAuth token và gọi FCM HTTP v1.

## Kiểm tra tích hợp

Backend được xem là hoàn thành khi:

1. `GET /config` trả đúng Firebase project, package và topic qua HTTPS.
2. Cài app, mở một lần, cấp quyền và app hiển thị `LIVE`.
3. Webhook hợp lệ trả `202`; webhook sai secret bị từ chối.
4. Một `eventId` không tạo nhiều notification ngoài ý muốn.
5. Thiết bị nhận được tiêu đề, nội dung, âm thanh và ảnh.
6. Thông báo xuất hiện trong lịch sử Room của app.
7. App vẫn nhận khi chạy nền.
8. Backend không log hoặc trả về private key, OAuth token, FCM token hay webhook secret.

Nếu người dùng bấm **Force stop** trong Android Settings, Android có thể ngừng giao FCM cho đến khi app được mở lại.

## Tài liệu chính thức

- [Firebase HTTP v1](https://firebase.google.com/docs/cloud-messaging/send/v1-api)
- [Nhận FCM trên Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)
- [TradingView webhook alerts](https://www.tradingview.com/support/solutions/43000529348-how-to-configure-webhook-alerts/)
