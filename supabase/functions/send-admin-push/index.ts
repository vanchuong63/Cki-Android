// Supabase Edge Function: send-admin-push
// Deploy after setting secrets:
// supabase secrets set SUPABASE_URL=... SUPABASE_SERVICE_ROLE_KEY=...
// supabase secrets set FIREBASE_PROJECT_ID=... FIREBASE_CLIENT_EMAIL=... FIREBASE_PRIVATE_KEY="..."

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

type WebhookPayload = {
  type?: string;
  table?: string;
  record?: {
    id?: string;
    message?: string;
    machine_name?: string;
  };
};

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const firebaseProjectId = Deno.env.get("FIREBASE_PROJECT_ID")!;
const firebaseClientEmail = Deno.env.get("FIREBASE_CLIENT_EMAIL")!;
const firebasePrivateKey = Deno.env.get("FIREBASE_PRIVATE_KEY")!.replaceAll("\\n", "\n");

const supabase = createClient(supabaseUrl, serviceRoleKey);

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const payload = (await req.json()) as WebhookPayload;
  console.log("send-admin-push invoked", {
    type: payload.type,
    table: payload.table,
    notificationId: payload.record?.id,
  });

  const record = payload.record;
  const message = record?.message ?? "San pham da het hang";
  const title = "Canh bao kho hang";

  const { data: devices, error } = await supabase
    .from("admin_devices")
    .select("token");

  if (error) {
    console.error("Cannot load admin device tokens", error);
    return new Response(error.message, { status: 500 });
  }

  const tokens = (devices ?? []).map((device) => device.token).filter(Boolean);
  console.log(`Found ${tokens.length} admin device token(s)`);

  if (tokens.length === 0) {
    return Response.json({ sent: 0, reason: "No admin device tokens" });
  }

  const accessToken = await getFirebaseAccessToken();
  const results = await Promise.allSettled(
    tokens.map((token) => sendFcm(accessToken, token, title, message)),
  );

  for (const result of results) {
    if (result.status === "rejected") {
      console.error("FCM send failed", result.reason);
    }
  }

  const sent = results.filter((result) => result.status === "fulfilled").length;
  const failed = results.filter((result) => result.status === "rejected").length;
  const failedReasons = results
    .filter((result): result is PromiseRejectedResult => result.status === "rejected")
    .map((result) => String(result.reason?.message ?? result.reason));

  console.log("send-admin-push completed", { sent, failed });

  return Response.json({
    sent,
    failed,
    failedReasons,
  });
});

async function sendFcm(accessToken: string, token: string, title: string, body: string) {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${firebaseProjectId}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token,
          notification: { title, body },
          android: {
            priority: "HIGH",
            notification: {
              channel_id: "fcm_low_stock_alerts",
              sound: "default",
            },
          },
          data: {
            title,
            body,
            type: "LOW_STOCK",
          },
        },
      }),
    },
  );

  if (!res.ok) {
    throw new Error(await res.text());
  }
}

async function getFirebaseAccessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const jwtHeader = { alg: "RS256", typ: "JWT" };
  const jwtClaim = {
    iss: firebaseClientEmail,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };

  const unsignedJwt = `${base64UrlEncode(JSON.stringify(jwtHeader))}.${base64UrlEncode(JSON.stringify(jwtClaim))}`;
  const signature = await signJwt(unsignedJwt, firebasePrivateKey);
  const jwt = `${unsignedJwt}.${signature}`;

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }

  const json = await res.json();
  return json.access_token;
}

async function signJwt(data: string, privateKeyPem: string): Promise<string> {
  const pemContents = privateKeyPem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");

  const binaryDer = Uint8Array.from(atob(pemContents), (char) => char.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(data),
  );

  return base64UrlEncodeBytes(new Uint8Array(signature));
}

function base64UrlEncode(value: string): string {
  return base64UrlEncodeBytes(new TextEncoder().encode(value));
}

function base64UrlEncodeBytes(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}
