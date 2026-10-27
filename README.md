# Trading Alert Receiver

Ứng dụng Android nhận cảnh báo giao dịch qua Firebase Cloud Messaging (FCM), tự phát âm báo theo thiết lập trong app, hiển thị ảnh và lưu 50 thông báo gần nhất bằng Room.

> Repository này chỉ chứa Android frontend. Backend là server/repository riêng và phải cung cấp API theo contract bên dưới. Không đặt Firebase service account, private key hoặc webhook secret trong app.

## Bắt đầu nhanh

Yêu cầu:

- JDK 17.
- Android SDK 36.
- Android Studio hỗ trợ Android Gradle Plugin 9.0.0.
- Thiết bị Android 7.0 (API 24) trở lên.

Build và kiểm tra:

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug lintDebug
```

APK debug được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Package của ứng dụng là `com.tradingalert.receiver`.

## Kết nối app với backend

App không dùng `google-services.json` và không đóng cứng URL backend trong APK.

1. Deploy backend lên một URL HTTPS công khai.
2. Mở app và bấm biểu tượng Cloud trên thanh công cụ.
3. Nhập URL gốc của backend, ví dụ `https://api.example.com`.
4. App gọi `GET https://api.example.com/config`.
5. Chỉ khi response hợp lệ, app mới lưu URL, khởi tạo Firebase và subscribe topic mà backend trả về.
6. Cấp quyền thông báo. Trạng thái `LIVE` xác nhận app đã lấy được FCM token và subscribe thành công.

URL được lưu cục bộ trên thiết bị. App cache Firebase client config để Firebase có thể khởi tạo lại khi process chạy nền.

## Luồng dữ liệu

```text
TradingView ──webhook──> Backend ──FCM HTTP v1──> Firebase ──push──> Android app
                                                                  │
                                                                  ├─ hiển thị notification
                                                                  ├─ phát âm báo riêng
                                                                  ├─ tải ảnh HTTPS
                                                                  └─ lưu Room database
```

Frontend chịu trách nhiệm nhận và hiển thị thông báo, lịch sử, ngôn ngữ, âm lượng, thời lượng chuông và âm thanh hệ thống. Backend chịu trách nhiệm nhận webhook, xác thực, chống gửi trùng và gửi FCM.

## API backend

Backend có thể chạy trên Cloudflare Worker hoặc nền tảng khác, miễn cung cấp các endpoint sau:

```text
GET  /config
POST /webhooks/tradingview
```

Với backend `https://api.example.com`:

```text
URL nhập trong app:       https://api.example.com
Config URL:               https://api.example.com/config
TradingView Webhook URL:  https://api.example.com/webhooks/tradingview
```

### Trả cấu hình cho Android

Request:

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
    "projectId": "firebase-project-id",
    "applicationId": "1:1234567890:android:abcdef123456",
    "apiKey": "firebase-android-api-key",
    "gcmSenderId": "1234567890",
    "storageBucket": "firebase-project-id.firebasestorage.app"
  }
}
```

Quy tắc validation của app:

- Endpoint phải dùng HTTPS.
- `androidPackageName` phải là `com.tradingalert.receiver`.
- `projectId`, `applicationId`, `apiKey`, `gcmSenderId` và `topic` không được trống.
- `storageBucket` có thể để trống.
- Topic phải khớp `^[a-zA-Z0-9-_.~%]{1,900}$`.
- Response tối đa 8 KiB.
- Timeout kết nối và đọc là 5 giây.

Đây là Firebase client config công khai. Endpoint không được trả service-account JSON, `client_email`, `private_key`, OAuth token hoặc webhook secret.

### Nhận webhook TradingView

TradingView gửi JSON đến `POST /webhooks/tradingview`:

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

Backend phải:

- Xác thực `WEBHOOK_SECRET`.
- Chống gửi trùng bằng `eventId`.
- Trả `202 Accepted` sau khi sự kiện hợp lệ được nhận xử lý.
- Gửi tới đúng topic mà backend trả về từ `/config`.
- Giữ service account và webhook secret trong secret manager.

## Gửi FCM đúng định dạng

Backend gọi Firebase HTTP v1 bằng service account:

```http
POST https://fcm.googleapis.com/v1/projects/FIREBASE_PROJECT_ID/messages:send
Authorization: Bearer OAUTH2_ACCESS_TOKEN
Content-Type: application/json
```

Nên gửi **data-only message** với priority `HIGH`:

```json
{
  "message": {
    "topic": "trading_alerts",
    "data": {
      "title": "BTCUSDT • BUY",
      "body": "@70500 • × 0.01",
      "reference": "order-42",
      "image": "https://cdn.example.com/trading-alert.jpg"
    },
    "android": {
      "priority": "HIGH",
      "ttl": "60s"
    }
  }
}
```

Mọi giá trị trong `data` phải là chuỗi. App đọc các trường:

| Trường | Bắt buộc | Mục đích |
| --- | --- | --- |
| `title` | Không | Tiêu đề; app dùng tiêu đề mặc định nếu thiếu |
| `body` | Không | Nội dung; app dùng nội dung mặc định nếu thiếu |
| `reference` | Không | Mã giao dịch hoặc event ID |
| `image` | Không | URL ảnh HTTPS |
| `image_url`, `imageUrl`, `picture` | Không | Tên thay thế cho `image` |

Không nên thêm object `notification` cho luồng production. Data-only message giúp `AlertMessagingService` tự hiển thị notification, lưu Room và áp dụng âm lượng/thời lượng chuông riêng của app. Firebase Console có thể dùng để kiểm tra nhanh, nhưng notification do Console gửi không đại diện đầy đủ cho hành vi data-only của backend.

Ảnh chỉ được tải qua HTTPS, tối đa 12 MiB và được scale về kích thước tối đa 1024 px trước khi hiển thị.

## Dữ liệu và âm thanh trên thiết bị

- Lịch sử được lưu trong Room database và tự giữ tối đa 50 bản ghi mới nhất.
- Cấu hình backend và Firebase client config được lưu trong private SharedPreferences của app.
- FCM token không hiển thị công khai trên UI.
- Âm lượng và thời lượng chuông chỉ áp dụng cho cảnh báo do app tự xử lý.
- Khi thông báo mới đến liên tục, app dừng âm đang phát trước khi phát âm của thông báo mới.
- Người dùng có thể chọn âm thanh tại Android Notification Channel settings.

Nếu người dùng bấm **Force stop** trong Android Settings, hệ thống có thể ngừng giao FCM cho đến khi app được mở lại.

## Test tích hợp

### Kiểm tra Android

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug lintDebug
```

Cài bản debug lên thiết bị:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r -t app\build\outputs\apk\debug\app-debug.apk
```

### Kiểm tra backend

Kiểm tra `/config` trước khi nhập URL vào app:

```powershell
Invoke-RestMethod -Uri "https://api.example.com/config"
```

Checklist hoàn thành:

1. App chấp nhận URL backend, subscribe topic do `/config` trả về và hiển thị `LIVE`.
2. Webhook sai secret bị từ chối.
3. Webhook hợp lệ trả `202`.
4. Thiết bị nhận được tiêu đề, nội dung, âm thanh và ảnh.
5. Thông báo mới xuất hiện trong màn lịch sử.
6. Âm lượng và thời lượng chuông đúng với thiết lập trong app.
7. App vẫn nhận khi ở nền hoặc process bị hệ thống dọn.
8. Một `eventId` không tạo nhiều thông báo ngoài ý muốn.

## Build release

```powershell
.\gradlew.bat assembleRelease
```

Bản release vẫn nhận FCM bình thường. Không thể dùng `adb shell run-as` để đọc dữ liệu private của release vì release không debuggable. Hãy kiểm tra bằng màn lịch sử trong app hoặc log/monitoring của backend.

## Tài liệu chính thức

- [Firebase HTTP v1](https://firebase.google.com/docs/cloud-messaging/send/v1-api)
- [Nhận FCM trên Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)
- [TradingView webhook alerts](https://www.tradingview.com/support/solutions/43000529348-how-to-configure-webhook-alerts/)
