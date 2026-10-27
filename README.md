# Trading Alert Receiver (Android)

Ứng dụng Android nhận cảnh báo từ Firebase Cloud Messaging (FCM), hiển thị notification ưu tiên cao và phát âm thanh ngắn ngay cả khi ứng dụng đang ở nền.

## Chức năng

- Nhận FCM `data message` trong `FirebaseMessagingService`.
- Notification channel ưu tiên cao, có âm thanh và rung.
- Xin quyền `POST_NOTIFICATIONS` trên Android 13 trở lên.
- Hiển thị/copy FCM registration token để thử gửi tới đúng thiết bị.
- Nút thử notification cục bộ và nút mở cài đặt âm thanh riêng của channel.
- Lưu và hiển thị cảnh báo nhận gần nhất.

## 1. Kết nối Firebase

1. Mở [Firebase Console](https://console.firebase.google.com/) và tạo/chọn project.
2. Thêm Android app với package name `com.tradingalert.receiver`.
3. Tải `google-services.json` và đặt vào `app/google-services.json`.
4. Mở project bằng Android Studio, đồng bộ Gradle rồi chạy trên điện thoại có Google Play services.
5. Mở app lần đầu và cấp quyền thông báo.

> Không commit `google-services.json` hay service-account key. Hai loại file này đã được thêm vào `.gitignore`.

## 2. Payload nên gửi

Để logic tùy chỉnh chạy ổn định khi app ở nền, hãy gửi **data message** với Android priority `HIGH`. Không thêm trường `notification` nếu muốn `onMessageReceived()` luôn tự tạo notification.

Ví dụ body cho FCM HTTP v1:

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

Token OAuth2 phải được tạo ở server tin cậy bằng service account; không nhúng service-account key vào app.

## 3. Kiểm thử

1. Chạy app và bấm **Thử thông báo và âm thanh**.
2. Copy FCM token trên màn hình.
3. Gửi payload HTTP v1 ở trên tới token đó.
4. Đưa app về nền hoặc tắt màn hình để kiểm tra.

Chạy unit test:

```powershell
.\gradlew.bat testDebugUnitTest
```

## Lưu ý Android

- Người dùng có thể đổi hoặc tắt âm thanh của notification channel trong cài đặt hệ thống. Sau khi channel đã được tạo, app không thể tự ghi đè lựa chọn này.
- Chế độ Không làm phiền, tiết kiệm pin, hoặc cơ chế chặn nền của từng hãng có thể làm chậm/tắt âm thanh.
- FCM high priority chỉ nên dùng cho cảnh báo cần người dùng nhìn thấy ngay. Nếu bị lạm dụng, Android/FCM có thể hạ mức ưu tiên.
- Nếu người dùng **Force stop** ứng dụng, Android sẽ không giao push cho đến khi họ mở app lại.
- FCM không cam kết thời gian giao tuyệt đối; không nên dùng làm hệ thống cảnh báo an toàn sinh mạng hoặc giao dịch bắt buộc thời gian thực.

## Đổi package name

Nếu đổi `applicationId`, hãy đăng ký Android app mới trong Firebase và tải lại đúng `google-services.json` tương ứng.
