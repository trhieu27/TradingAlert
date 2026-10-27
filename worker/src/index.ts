interface Env {
  FIREBASE_PROJECT_ID: string;
  FIREBASE_CLIENT_EMAIL: string;
  FIREBASE_PRIVATE_KEY: string;
  FIREBASE_ANDROID_APP_ID: string;
  FIREBASE_ANDROID_API_KEY: string;
  FIREBASE_SENDER_ID: string;
  FIREBASE_STORAGE_BUCKET?: string;
  ANDROID_PACKAGE_NAME: string;
  WEBHOOK_SECRET?: string;
  FCM_TOPIC: string;
  ALLOW_UNVERIFIED_WEBHOOKS?: string;
}

interface TradingAlertPayload {
  secret?: unknown;
  eventId?: unknown;
  event_id?: unknown;
  title?: unknown;
  body?: unknown;
  symbol?: unknown;
  ticker?: unknown;
  action?: unknown;
  side?: unknown;
  price?: unknown;
  quantity?: unknown;
  qty?: unknown;
  time?: unknown;
  image?: unknown;
  imageUrl?: unknown;
  image_url?: unknown;
}

interface AccessTokenCache {
  value: string;
  expiresAt: number;
}

const TRADINGVIEW_IPS = new Set([
  "52.89.214.238",
  "34.212.75.30",
  "54.218.53.128",
  "52.32.178.7",
]);
const TOPIC_PATTERN = /^[a-zA-Z0-9-_.~%]{1,900}$/;
const MAX_BODY_BYTES = 32 * 1024;
let accessTokenCache: AccessTokenCache | undefined;

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/config") {
      if (!TOPIC_PATTERN.test(env.FCM_TOPIC)) {
        return json({ error: "FCM_TOPIC is invalid" }, 500);
      }
      if (
        !env.FIREBASE_PROJECT_ID ||
        !env.FIREBASE_ANDROID_APP_ID ||
        !env.FIREBASE_ANDROID_API_KEY ||
        !env.FIREBASE_SENDER_ID ||
        !env.ANDROID_PACKAGE_NAME
      ) {
        return json({ error: "Firebase Android client configuration is incomplete" }, 503);
      }
      return json({
        topic: env.FCM_TOPIC,
        firebase: {
          androidPackageName: env.ANDROID_PACKAGE_NAME,
          projectId: env.FIREBASE_PROJECT_ID,
          applicationId: env.FIREBASE_ANDROID_APP_ID,
          apiKey: env.FIREBASE_ANDROID_API_KEY,
          gcmSenderId: env.FIREBASE_SENDER_ID,
          storageBucket: env.FIREBASE_STORAGE_BUCKET ?? "",
        },
      });
    }

    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true });
    }

    if (request.method !== "POST" || url.pathname !== "/webhooks/tradingview") {
      return json({ error: "Not found" }, 404);
    }

    const sourceIp = request.headers.get("CF-Connecting-IP") ?? "";
    if (env.ALLOW_UNVERIFIED_WEBHOOKS !== "true" && !TRADINGVIEW_IPS.has(sourceIp)) {
      return json({ error: "Webhook source is not allowed" }, 403);
    }

    const declaredLength = Number(request.headers.get("Content-Length") ?? "0");
    if (declaredLength > MAX_BODY_BYTES) {
      return json({ error: "Payload is too large" }, 413);
    }

    let payload: TradingAlertPayload;
    try {
      const text = await request.text();
      if (new TextEncoder().encode(text).byteLength > MAX_BODY_BYTES) {
        return json({ error: "Payload is too large" }, 413);
      }
      payload = JSON.parse(text) as TradingAlertPayload;
    } catch {
      return json({ error: "A valid JSON body is required" }, 400);
    }

    if (env.WEBHOOK_SECRET && stringValue(payload.secret) !== env.WEBHOOK_SECRET) {
      return json({ error: "Invalid webhook secret" }, 401);
    }
    if (!TOPIC_PATTERN.test(env.FCM_TOPIC)) {
      return json({ error: "FCM_TOPIC is invalid" }, 500);
    }

    const message = normalizePayload(payload);
    ctx.waitUntil(
      sendFcmMessage(env, message).catch((error: unknown) => {
        console.error("Unable to send FCM message", error);
      }),
    );
    return json({ accepted: true, eventId: message.eventId }, 202);
  },
} satisfies ExportedHandler<Env>;

function normalizePayload(payload: TradingAlertPayload) {
  const symbol = firstString(payload.symbol, payload.ticker);
  const action = firstString(payload.action, payload.side).toUpperCase();
  const price = stringValue(payload.price);
  const quantity = firstString(payload.quantity, payload.qty);
  const image = firstString(payload.image, payload.imageUrl, payload.image_url);
  const eventId = firstString(payload.eventId, payload.event_id) || crypto.randomUUID();
  const title = stringValue(payload.title) || [symbol, action].filter(Boolean).join(" • ") || "Trading alert";
  const details = [price && `@${price}`, quantity && `× ${quantity}`].filter(Boolean).join(" • ");
  const body = stringValue(payload.body) || details || "A new trading signal was received.";

  return {
    eventId,
    title,
    body,
    symbol,
    action,
    price,
    quantity,
    time: stringValue(payload.time) || new Date().toISOString(),
    image,
  };
}

async function sendFcmMessage(env: Env, alert: ReturnType<typeof normalizePayload>): Promise<void> {
  const accessToken = await getAccessToken(env);
  const data: Record<string, string> = {
    eventId: alert.eventId,
    symbol: alert.symbol,
    action: alert.action,
    price: alert.price,
    quantity: alert.quantity,
    time: alert.time,
  };
  if (alert.image) data.image = alert.image;

  const notification: Record<string, string> = {
    title: alert.title,
    body: alert.body,
  };
  if (alert.image) notification.image = alert.image;

  const androidNotification: Record<string, string> = {
    channel_id: "trading_alerts_v5",
    sound: "default",
  };
  if (alert.image) androidNotification.image = alert.image;

  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(env.FIREBASE_PROJECT_ID)}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          topic: env.FCM_TOPIC,
          notification,
          data,
          android: {
            priority: "HIGH",
            notification: androidNotification,
          },
        },
      }),
    },
  );

  if (!response.ok) {
    throw new Error(`FCM returned HTTP ${response.status}: ${await response.text()}`);
  }
}

async function getAccessToken(env: Env): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (accessTokenCache && accessTokenCache.expiresAt > now + 60) {
    return accessTokenCache.value;
  }

  const assertion = await createServiceAccountAssertion(env, now);
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) {
    throw new Error(`OAuth token endpoint returned HTTP ${response.status}: ${await response.text()}`);
  }

  const token = (await response.json()) as { access_token?: string; expires_in?: number };
  if (!token.access_token) throw new Error("OAuth response did not include an access token");
  accessTokenCache = {
    value: token.access_token,
    expiresAt: now + (token.expires_in ?? 3600),
  };
  return token.access_token;
}

async function createServiceAccountAssertion(env: Env, issuedAt: number): Promise<string> {
  const header = base64UrlJson({ alg: "RS256", typ: "JWT" });
  const claims = base64UrlJson({
    iss: env.FIREBASE_CLIENT_EMAIL,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: issuedAt,
    exp: issuedAt + 3600,
  });
  const unsigned = `${header}.${claims}`;
  const privateKey = env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, "\n");
  const keyBytes = pemToBytes(privateKey);
  const keyBuffer = keyBytes.buffer.slice(
    keyBytes.byteOffset,
    keyBytes.byteOffset + keyBytes.byteLength,
  ) as ArrayBuffer;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    keyBuffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned),
  );
  return `${unsigned}.${base64UrlBytes(new Uint8Array(signature))}`;
}

function pemToBytes(pem: string): Uint8Array {
  const base64 = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  if (!base64) throw new Error("FIREBASE_PRIVATE_KEY is invalid");
  return Uint8Array.from(atob(base64), (character) => character.charCodeAt(0));
}

function base64UrlJson(value: unknown): string {
  return base64UrlBytes(new TextEncoder().encode(JSON.stringify(value)));
}

function base64UrlBytes(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

function firstString(...values: unknown[]): string {
  for (const value of values) {
    const normalized = stringValue(value);
    if (normalized) return normalized;
  }
  return "";
}

function stringValue(value: unknown): string {
  if (typeof value === "string") return value.trim();
  if (typeof value === "number" || typeof value === "boolean") return String(value);
  return "";
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });
}
