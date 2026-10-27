# Trading Alert Receiver

Ứng dụng Android nhận tín hiệu từ Firebase Cloud Messaging (FCM), hiển thị thông báo ưu tiên cao và phát âm thanh ngắn khi ứng dụng đang mở hoặc chạy nền.

## Tính năng

- Nhận FCM `data message` bằng `FirebaseMessagingService`.
- Hiển thị notification ưu tiên cao với tiếng chime riêng của ứng dụng và rung.
- Xin quyền thông báo trên Android 13 trở lên.
- Hiển thị và sao chép FCM registration token.
- Gửi thông báo thử trực tiếp trên thiết bị.
- Mở nhanh cài đặt notification channel để chọn âm thanh.
- Lưu và hiển thị thông báo nhận gần nhất.
- Giao diện Jetpack Compose và hỗ trợ dark mode.

## Thông tin project

| Thành phần | Giá trị |
|---|---|
| Tên ứng dụng | Trading Alert Receiver |
| Package / Application ID | `com.tradingalert.receiver` |
| Ngôn ngữ | Kotlin |
| Giao diện | Jetpack Compose + Material 3 |
| Min SDK | 24 — Android 7.0 |
| Target / Compile SDK | 36 |
| Android Gradle Plugin | 9.0.0 |
| Gradle Wrapper | 9.6.0 |
| Java | 17 trở lên |

## Yêu cầu

- Android Studio có hỗ trợ AGP 9.0.0.
- Android SDK Platform 36.
- JDK 17 hoặc mới hơn.
- Điện thoại có Google Play services hoặc emulator dùng Google APIs.
- Một Firebase project đã bật Cloud Messaging.

## Cấu hình Firebase

### 1. Đăng ký Android app

1. Mở [Firebase Console](https://console.firebase.google.com/).
2. Tạo hoặc chọn Firebase project.
3. Thêm Android app với package chính xác:

```text
com.tradingalert.receiver
```

4. Tải file `google-services.json`.
5. Đặt file tại:

```text
D:\app\app\google-services.json
```

`google-services.json` và service-account key đã được `.gitignore`; không commit các file này lên Git.

Project vẫn build được khi chưa có `google-services.json`, nhưng lúc đó chỉ có thể thử notification cục bộ và không thể nhận FCM thật.

### 2. Firebase test và Firebase khách hàng

Để test bằng Firebase của bạn, đăng ký package trên trong Firebase của bạn và dùng file `google-services.json` tương ứng.

Khi chuyển sang Firebase của khách hàng:

1. Đăng ký cùng package `com.tradingalert.receiver` trong Firebase của khách hàng.
2. Thay `app/google-services.json` bằng file của khách hàng.
3. Xóa ứng dụng test khỏi điện thoại hoặc xóa dữ liệu ứng dụng.
4. Clean, build và cài lại app.
5. Lấy FCM token mới trong màn hình chính.

FCM token và thông tin xác thực gửi tin nhắn của hai Firebase project không dùng thay thế cho nhau.

## Build và kiểm thử

Các lệnh dưới đây chạy trong PowerShell tại `D:\app`.

### Build APK debug

```powershell
.\gradlew.bat assembleDebug
```

APK được tạo tại:

```text
app\build\outputs\apk\debug\app-debug.apk
```

### Chạy unit test

```powershell
.\gradlew.bat testDebugUnitTest
```

### Chạy lint

```powershell
.\gradlew.bat lintDebug
```

### Clean và build lại

```powershell
.\gradlew.bat clean assembleDebug
```

## Kiểm tra notification trên thiết bị

1. Cài và mở ứng dụng.
2. Cấp quyền thông báo khi được yêu cầu.
3. Nhấn **Thử thông báo và âm thanh**.
4. Nếu không có âm thanh, nhấn **Âm thanh riêng của app** và bật kênh **Cảnh báo giao dịch**. Màn hình này chỉ thay đổi channel của Trading Alert Receiver, không đổi âm thanh notification của ứng dụng khác.
5. Sao chép FCM token để gửi thử từ server hoặc Firebase.

## Payload FCM khuyến nghị

Để `AlertMessagingService.onMessageReceived()` xử lý tin nhắn khi app ở nền, server nên gửi **data message** với Android priority `HIGH`. Không thêm trường `notification` nếu muốn app luôn tự tạo notification.

Ví dụ request body cho FCM HTTP v1:

```json
{
  "message": {
    "token": "FCM_TOKEN_HIEN_THI_TRONG_APP",
    "data": {
      "title": "Trading alert",
      "body": "BTCUSDT đã chạm mức 70,000",
      "reference": "btc-70000"
    },
    "android": {
      "priority": "HIGH",
      "ttl": "60s"
    }
  }
}
```

Endpoint:

```text
POST https://fcm.googleapis.com/v1/projects/PROJECT_ID/messages:send
Authorization: Bearer OAUTH2_ACCESS_TOKEN
Content-Type: application/json
```

OAuth2 access token phải được tạo trên server tin cậy bằng service account. Không nhúng service-account JSON hoặc server credential vào ứng dụng Android.

## Cấu trúc chính

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/tradingalert/receiver/
│   ├── MainActivity.kt
│   ├── TradingAlertReceiverApplication.kt
│   ├── messaging/
│   │   ├── AlertMessagingService.kt
│   │   ├── NotificationHelper.kt
│   │   └── PushMessage.kt
│   └── ui/theme/Theme.kt
└── res/
    ├── drawable/
    ├── drawable-nodpi/
    ├── mipmap-*/
    └── values/
```

## Xử lý sự cố

### Không có cấu hình `app` hoặc nút Run bị mờ

- Đóng project hiện tại.
- Mở lại thư mục gốc `D:\app`, không mở thư mục module `D:\app\app`.
- Chạy **Sync Project with Gradle Files**.
- Kiểm tra Gradle JDK đang dùng JDK 17 trở lên.

### Không lấy được FCM token

- Kiểm tra `app/google-services.json` tồn tại.
- Kiểm tra package trong Firebase là `com.tradingalert.receiver`.
- Kiểm tra thiết bị có Google Play services và có kết nối Internet.
- Clean, gỡ bản cũ rồi cài lại app.

### Nhận notification nhưng không có âm thanh

- Kiểm tra quyền thông báo của ứng dụng.
- Kiểm tra âm thanh của channel **Cảnh báo Firebase**.
- Tắt chế độ Không làm phiền nếu đang bật.
- Kiểm tra tối ưu pin và quyền tự khởi động trên điện thoại Xiaomi, Oppo, Vivo, Huawei hoặc Samsung.

### App không nhận push sau khi Force stop

Đây là hành vi của Android. Người dùng cần mở lại app ít nhất một lần trước khi hệ thống tiếp tục giao FCM.

## Lưu ý vận hành

- Người dùng có thể thay đổi hoặc tắt âm thanh notification channel trong cài đặt hệ thống; ứng dụng không thể tự ghi đè lựa chọn đó.
- FCM high priority chỉ nên dùng cho tín hiệu cần người dùng chú ý ngay. Lạm dụng có thể khiến hệ thống hạ mức ưu tiên.
- FCM không cam kết thời gian giao tuyệt đối; không dùng ứng dụng như hệ thống cảnh báo an toàn sinh mạng hoặc giao dịch bắt buộc thời gian thực.

## Tài liệu tham khảo

- [Thiết lập Firebase cho Android](https://firebase.google.com/docs/android/setup)
- [Nhận FCM trên Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)
- [Gửi FCM bằng HTTP v1](https://firebase.google.com/docs/cloud-messaging/send-message)
- [Cấu hình nhiều Firebase project](https://firebase.google.com/docs/projects/multiprojects)
