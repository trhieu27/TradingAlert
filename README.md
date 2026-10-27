# Trading Alert Receiver

Ứng dụng Android nhận cảnh báo giao dịch từ Firebase Cloud Messaging (FCM), hiển thị ảnh, phát âm báo theo thiết lập riêng của app và lưu 50 thông báo gần nhất bằng Room.

Repository này **chỉ chứa Android frontend**. Backend được triển khai riêng (Cloudflare Worker hoặc server bất kỳ) và phải tuân theo API contract trong tài liệu này.

## Kiến trúc

```text
TradingView
    │ POST /webhooks/tradingview
    ▼
Backend riêng ── FCM HTTP v1 ──> Firebase ──> Android app
    ▲                                      ├─ hiện notification và ảnh
    │ GET /config                          ├─ phát âm báo riêng
    └──────────────── Android app          └─ lưu lịch sử vào Room
```

- Android chỉ gọi `GET /config` để lấy Firebase client config và topic.
- TradingView gọi `POST /webhooks/tradingview`; Android không gọi endpoint này.
- Backend xác thực webhook, chuyển dữ liệu thành FCM data message và gửi tới topic.
- App subscribe topic do backend trả về. Topic không được đóng cứng trong app.
- Không đưa Firebase service account, private key hoặc webhook secret vào APK hay Git.

## Yêu cầu Android

- JDK 17.
- Android SDK 36.
- Android Studio hỗ trợ Android Gradle Plugin 9.0.0.
- Gradle 9.6.
- Android 7.0 (API 24) trở lên.

Package cố định của app:

```text
com.tradingalert.receiver
```

Build và kiểm tra:

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug lintDebug
```

APK debug:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Kết nối app với backend

App không cần `app/google-services.json`. Firebase được khởi tạo lúc chạy bằng client config lấy từ backend.

1. Deploy backend tại một URL HTTPS công khai, ví dụ `https://alerts.example.com`.
2. Ở lần mở đầu tiên, app hiển thị màn hướng dẫn kết nối backend. Người dùng có thể cấu hình ngay hoặc chọn **Bỏ qua** để vào màn chính.
3. Nhập URL gốc `https://alerts.example.com`; không nhập `/config` hoặc đường dẫn webhook. Nếu đã bỏ qua hoặc muốn đổi backend, bấm biểu tượng Cloud trên thanh công cụ.
4. App tự gọi `GET https://alerts.example.com/config`.
5. Khi response hợp lệ, app lưu URL, cache config, khởi tạo Firebase và subscribe topic được trả về.
6. Cấp quyền thông báo. Trạng thái `LIVE` cho biết app đã có FCM token và subscribe topic thành công.

URL và Firebase client config được lưu trong vùng dữ liệu riêng của app để Firebase có thể khởi tạo lại khi process chạy nền.
Khi chọn **Bỏ qua**, app ghi nhận onboarding đã hoàn tất và không tự hiện lại ở lần mở sau; xóa dữ liệu ứng dụng sẽ đặt lại trạng thái này.

## Contract backend

Backend phải cung cấp hai endpoint:

```text
GET  /config
POST /webhooks/tradingview
```

Với URL backend `https://alerts.example.com`:

```text
URL nhập trong app:       https://alerts.example.com
Config URL của app:       https://alerts.example.com/config
Webhook URL TradingView:  https://alerts.example.com/webhooks/tradingview
```

### GET /config

Android gửi:

```http
GET /config HTTP/1.1
Accept: application/json
```

Backend trả `200 OK` và `Content-Type: application/json`:

```json
{
  "topic": "trading_alerts",
  "firebase": {
    "androidPackageName": "com.tradingalert.receiver",
    "projectId": "your-firebase-project-id",
    "applicationId": "1:1234567890:android:abcdef123456",
    "apiKey": "your-firebase-android-api-key",
    "gcmSenderId": "1234567890",
    "storageBucket": "your-firebase-project-id.firebasestorage.app"
  }
}
```

Các giá trị trong `firebase` lấy từ Android app đã đăng ký trong Firebase project. Android app đó bắt buộc có package name `com.tradingalert.receiver`.

Nếu backend đọc từ file cấu hình Firebase Android, ánh xạ các trường như sau:

| Response `/config` | Nguồn Firebase Android |
| --- | --- |
| `androidPackageName` | `client[].client_info.android_client_info.package_name` |
| `projectId` | `project_info.project_id` |
| `applicationId` | `client[].client_info.mobilesdk_app_id` |
| `apiKey` | `client[].api_key[].current_key` |
| `gcmSenderId` | `project_info.project_number` |
| `storageBucket` | `project_info.storage_bucket` |

Backend phải chọn đúng phần tử `client[]` có package name `com.tradingalert.receiver`.

App kiểm tra response như sau:

- App chỉ chấp nhận URL gốc HTTPS; hậu tố `/config` được tự loại bỏ, còn credentials, query, fragment và các endpoint path khác sẽ bị từ chối.
- `topic`, `projectId`, `applicationId`, `apiKey` và `gcmSenderId` không được trống.
- `androidPackageName` phải là `com.tradingalert.receiver`.
- `storageBucket` có thể rỗng.
- Topic phải khớp `^[a-zA-Z0-9-_.~%]{1,900}$`.
- Response tối đa 8.192 ký tự; timeout kết nối và đọc là 5 giây.

Firebase client config không phải thông tin bí mật, nhưng endpoint tuyệt đối không được trả service-account JSON, `client_email`, `private_key`, OAuth access token hoặc webhook secret.

### POST /webhooks/tradingview

Đây là endpoint dành cho TradingView, không phải cho Android. Một message mẫu để cấu hình trong TradingView:

```json
{
  "secret": "YOUR_WEBHOOK_SECRET",
  "eventId": "{{ticker}}-{{timenow}}-{{strategy.order.id}}",
  "symbol": "{{ticker}}",
  "action": "{{strategy.order.action}}",
  "price": "{{strategy.order.price}}",
  "quantity": "{{strategy.order.contracts}}",
  "time": "{{timenow}}",
  "image": "https://cdn.example.com/trading-alert.jpg"
}
```

Backend cần thực hiện theo thứ tự:

1. Chỉ chấp nhận `POST` và JSON hợp lệ.
2. Xác thực webhook secret bằng giá trị lưu trong secret manager.
3. Validate các trường cần thiết và giới hạn request theo khả năng của hạ tầng backend.
4. Chống xử lý trùng bằng `eventId` hoặc ID tương đương.
5. Chuyển sự kiện thành FCM data message.
6. Gửi tới **cùng topic** đang trả trong `GET /config`.
7. Trả `202 Accepted` khi sự kiện đã được nhận để xử lý; trả mã `4xx` khi request không hợp lệ hoặc không được phép.

TradingView hủy webhook nếu backend xử lý quá 3 giây. Với Cloudflare Worker, nên xác thực và trả response nhanh, sau đó gửi FCM trong tác vụ nền bằng `ctx.waitUntil(...)`. Webhook có thể thất bại ngoài ý muốn, vì vậy backend cần log kết quả và giữ cơ chế chống trùng.

Không đóng cứng IP TradingView hoặc kích thước body theo code Android. Nếu backend muốn allowlist IP, rate limit hay giới hạn body, hãy cấu hình tại backend theo tài liệu hiện hành của TradingView và giới hạn thực tế của nền tảng triển khai.

## Gửi FCM từ backend

Backend cần Firebase Admin service account của đúng Firebase project. Với Cloudflare Worker, lưu service-account email, private key, project ID và webhook secret bằng Worker Secrets; không commit chúng vào source.

Backend tạo OAuth2 access token từ service account rồi gọi Firebase HTTP v1:

```http
POST https://fcm.googleapis.com/v1/projects/FIREBASE_PROJECT_ID/messages:send
Authorization: Bearer OAUTH2_ACCESS_TOKEN
Content-Type: application/json
```

Gửi **data-only message** với priority `HIGH`:

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

`message.topic` phải giống `topic` trong `GET /config`. Mọi giá trị trong `data` phải là chuỗi.

App hỗ trợ các trường:

| Trường | Bắt buộc | Giới hạn trong app | Mục đích |
| --- | --- | --- | --- |
| `title` | Không | 120 ký tự | Tiêu đề thông báo |
| `body` | Không | 500 ký tự | Nội dung thông báo |
| `reference` | Không | 100 ký tự | Mã giao dịch hoặc event ID |
| `image` | Không | URL dài tối đa 2.048 ký tự | Ảnh HTTPS |

`image_url`, `imageUrl` và `picture` cũng được chấp nhận như tên thay thế cho `image`.

Không thêm object `notification` cho luồng production. Data-only message giúp `AlertMessagingService` luôn tự hiển thị notification, lưu Room, tải ảnh và áp dụng âm lượng/thời lượng chuông của app. Firebase Console phù hợp để kiểm tra nhanh nhưng không đại diện đầy đủ cho data-only message do backend gửi.

## Hành vi trên thiết bị

- Lịch sử lưu bằng Room và giữ tối đa 50 bản ghi mới nhất.
- Ảnh chỉ được tải qua HTTPS, tối đa 12 MiB và scale về tối đa 1.024 px.
- Người dùng chọn âm tại Android Notification Channel settings.
- App điều chỉnh âm lượng từ 0–100% và thời lượng chuông từ 1–30 giây.
- Khi cảnh báo mới đến liên tục, âm đang phát được dừng trước khi phát âm của cảnh báo mới.
- App hỗ trợ tiếng Việt mặc định và tiếng Anh.
- FCM topic không lưu lại thông báo cũ. Thiết bị chỉ nhận message gửi sau khi subscribe thành công.
- Nếu người dùng **Force stop**, Android có thể chặn FCM cho tới khi app được mở lại.

## Kiểm tra end-to-end

1. Gọi `GET /config` và kiểm tra JSON:

   ```powershell
   Invoke-RestMethod -Uri "https://alerts.example.com/config"
   ```

2. Nhập URL gốc backend trong app, cấp quyền thông báo và chờ trạng thái `LIVE`.
3. Gửi một webhook hợp lệ tới backend bằng TradingView hoặc công cụ HTTP.
4. Kiểm tra log backend: webhook hợp lệ, FCM HTTP v1 trả thành công và topic đúng.
5. Kiểm tra thiết bị ở cả foreground, background và sau khi process bị hệ thống dọn.
6. Xác nhận notification có tiêu đề, nội dung, ảnh, âm thanh và xuất hiện trong lịch sử.
7. Gửi lại cùng `eventId` để xác nhận backend không tạo cảnh báo trùng.

Nếu app hiển thị `LIVE` nhưng không nhận được cảnh báo, kiểm tra trước tiên rằng topic backend gửi và topic trả từ `/config` hoàn toàn giống nhau, Firebase service account thuộc đúng project, FCM HTTP v1 không báo lỗi và app chưa bị Force stop.

## Build release

```powershell
.\gradlew.bat assembleRelease
```

Bản release vẫn nhận FCM bình thường. `adb shell run-as` không đọc được dữ liệu private của release vì release không debuggable; hãy kiểm tra bằng màn lịch sử trong app và log/monitoring của backend.

Project hiện chưa khai báo release signing config, vì vậy `assembleRelease` tạo `app/build/outputs/apk/release/app-release-unsigned.apk`. Cần cấu hình keystore/signing riêng trước khi phát hành hoặc cài bản release lên thiết bị.

## Tài liệu tham khảo

- [Firebase HTTP v1](https://firebase.google.com/docs/cloud-messaging/send/v1-api)
- [Nhận FCM trên Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)
- [TradingView webhook alerts](https://www.tradingview.com/support/solutions/43000529348-how-to-configure-webhook-alerts/)
