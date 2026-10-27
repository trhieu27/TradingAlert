# Trading Alert Cloudflare Worker

Luồng hoạt động: TradingView gửi webhook đến Worker, Worker xác thực payload và gửi FCM HTTP v1. Ứng dụng Android lấy Firebase client config cùng topic từ `GET /config`, tự khởi tạo Firebase và subscribe topic.

## 1. Cấu hình Firebase cho Worker

Bật Firebase Cloud Messaging API trong Google Cloud. Tạo service account có quyền gửi FCM (ví dụ role **Firebase Cloud Messaging API Admin**) rồi đặt ba giá trị dưới dạng Cloudflare secrets; không commit file service-account vào Git:

```powershell
cd worker
npm install
npx wrangler secret put FIREBASE_PROJECT_ID
npx wrangler secret put FIREBASE_CLIENT_EMAIL
npx wrangler secret put FIREBASE_PRIVATE_KEY
npx wrangler secret put WEBHOOK_SECRET
```

Topic được cấu hình tại `FCM_TOPIC` trong `wrangler.toml`. Khi đổi topic và deploy lại Worker, app sẽ lấy topic mới từ server ở lần mở tiếp theo và tự bỏ đăng ký topic cũ.

## 2. Deploy

```powershell
npm run typecheck
npm run deploy
```

Sau khi deploy, webhook TradingView là:

```text
https://<worker-name>.<account-subdomain>.workers.dev/webhooks/tradingview
```

Endpoint kiểm tra:

```text
GET https://<worker-host>/health
GET https://<worker-host>/config
```

## 3. Cấu hình Firebase client cho app

App không dùng `google-services.json`. Điền các biến public trong `wrangler.toml` từ Android app `com.tradingalert.receiver` đã tạo trong Firebase:

```toml
ANDROID_PACKAGE_NAME = "com.tradingalert.receiver"
FIREBASE_ANDROID_APP_ID = "1:1234567890:android:abcdef123456"
FIREBASE_ANDROID_API_KEY = "example-android-api-key"
FIREBASE_SENDER_ID = "1234567890"
FIREBASE_STORAGE_BUCKET = "example-project.firebasestorage.app"
```

`GET /config` trả các giá trị này cho app. Đây là client identifiers công khai; service-account private key không được trả về endpoint.

Đặt URL Worker bằng Gradle property hoặc biến môi trường:

```powershell
.\gradlew.bat assembleRelease -PWORKER_BASE_URL=https://<worker-host>
```

Private key của service account chỉ được đặt trong Cloudflare secrets.

## 4. Nội dung alert TradingView

Ví dụ cho strategy alert (thay `YOUR_WEBHOOK_SECRET` bằng secret đã đặt trên Worker):

```json
{
  "secret": "YOUR_WEBHOOK_SECRET",
  "eventId": "{{ticker}}-{{timenow}}-{{strategy.order.id}}",
  "symbol": "{{ticker}}",
  "action": "{{strategy.order.action}}",
  "price": "{{strategy.order.price}}",
  "quantity": "{{strategy.order.contracts}}",
  "time": "{{timenow}}"
}
```

Có thể gửi thêm `title`, `body` và `image` (URL HTTPS công khai). TradingView phải bật 2FA để dùng webhook. Worker mặc định chỉ nhận request từ các IP webhook chính thức của TradingView.

Để test cục bộ bằng `wrangler dev`, sao chép `.dev.vars.example` thành `.dev.vars` và tạm đặt `ALLOW_UNVERIFIED_WEBHOOKS = "true"` trong `wrangler.toml`. Không dùng giá trị này khi deploy production.
