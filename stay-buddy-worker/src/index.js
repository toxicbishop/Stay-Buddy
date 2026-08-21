import { Client } from "@notionhq/client";

// Admin "Channel" → Android notification channel (matches NotificationHelper.kt).
// Everything without a dedicated Android channel falls back to "messages".
const ANDROID_CHANNEL_BY_ROUTE = {
    listings: 'listings',
    roommates: 'roommates',
    messages: 'messages'
};

function resolveChannelId(routeTypeOrChannel) {
    return ANDROID_CHANNEL_BY_ROUTE[routeTypeOrChannel] || 'messages';
}

export default {
    async fetch(request, env, ctx) {
        try {
            const url = new URL(request.url);
            if (request.method === "POST" && url.pathname === "/api/ticket") {
                const body = await request.json();
                const ticketId = body.ticketId;
                const ticket = body.ticket;

                if (!ticketId || !ticket) {
                    return new Response("Missing ticketId or ticket", { status: 400 });
                }

                const notion = new Client({ auth: env.NOTION_API_KEY });
                const response = await notion.pages.create({
                    parent: { database_id: env.NOTION_DATABASE_ID },
                    properties: {
                        "Ticket ID": { title: [{ text: { content: body.displayId || ticketId } }] },
                        "User ID": { rich_text: [{ text: { content: ticket.userId || "" } }] },
                        "Email": { email: ticket.email || "" },
                        "Issue Type": { select: { name: ticket.issueType || "Other" } },
                        "Status": { select: { name: ticket.status || "Pending" } },
                        "Message": { rich_text: [{ text: { content: ticket.description || "" } }] }
                    },
                    children: [
                        {
                            object: 'block',
                            type: 'paragraph',
                            paragraph: {
                                rich_text: [
                                    { type: 'text', text: { content: ticket.description || "No description provided." } }
                                ]
                            }
                        }
                    ]
                });
                return new Response(JSON.stringify({ success: true, notionPageId: response.id }), {
                    status: 200,
                    headers: { "Content-Type": "application/json" }
                });
            }
            
            // New Webhook Endpoint for Notion Automations
            if (request.method === "POST" && url.pathname === "/api/notion-webhook") {
                try {
                    await syncFromNotionToFirestore(env);
                    return new Response("Webhook received and sync triggered successfully", { status: 200 });
                } catch (err) {
                    console.error(err);
                    return new Response("Error: " + err.message, { status: 500 });
                }
            }

            // Notification Endpoint (replaces Vercel)
            if (request.method === "POST" && url.pathname === "/api/send-notification") {
                const body = await request.json();
                const { targetUserId, fcmTokenOverride, title, body: notifBody, routeType, routeId, route, imageUrl, avatarUrl, messageId, senderId, channelId } = body;

                if ((!targetUserId && !fcmTokenOverride) || !title || !notifBody) {
                    return new Response(JSON.stringify({ error: 'Missing required fields' }), { status: 400 });
                }

                try {
                    let fcmToken = fcmTokenOverride;
                    if (!fcmToken && targetUserId) {
                        fcmToken = await getUserFCMToken(targetUserId, env.FIREBASE_SERVICE_ACCOUNT_JSON, env.FIREBASE_PROJECT_ID);
                    }
                    if (!fcmToken) {
                        return new Response(JSON.stringify({ error: 'User FCM token not found' }), { status: 404 });
                    }

                    const message = {
                        token: fcmToken,
                        android: { priority: "high" },
                        notification: {
                            title: title,
                            body: notifBody,
                            ...(imageUrl ? { image: imageUrl } : {})
                        },
                        data: {
                            title: title,
                            body: notifBody,
                            routeType: routeType || 'default',
                            routeId: routeId || '',
                            channelId: channelId || ANDROID_CHANNEL_BY_ROUTE[routeType] || 'messages',
                            ...(route ? { route: route } : {}),
                            avatarUrl: avatarUrl || '',
                            messageId: messageId || '',
                            senderId: senderId || ''
                        }
                    };

                    const result = await sendFCMMessage(message, env.FIREBASE_SERVICE_ACCOUNT_JSON, env.FIREBASE_PROJECT_ID);
                    return new Response(JSON.stringify({ success: true, result }), { status: 200, headers: { "Content-Type": "application/json" } });
                } catch (err) {
                    console.error("Push Notification Error:", err);
                    return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: { "Content-Type": "application/json" } });
                }
            }

            return new Response("Not found", { status: 404 });
        } catch (error) {
            console.error("Fetch error:", error);
            return new Response(error.message, { status: 500 });
        }
    },

    async scheduled(event, env, ctx) {
        ctx.waitUntil((async () => {
            try {
                await syncFromNotionToFirestore(env);
            } catch (e) {
                console.error("Notion sync error:", e);
            }
            try {
                await processScheduledNotifications(env);
            } catch (e) {
                console.error("Scheduled notifications error:", e);
            }
        })());
    }
};

async function syncFromNotionToFirestore(env) {
    try {
        const notion = new Client({ auth: env.NOTION_API_KEY });
        const response = await notion.databases.query({
            database_id: env.NOTION_DATABASE_ID,
            sorts: [{ timestamp: "last_edited_time", direction: "descending" }],
            page_size: 20
        });

        const token = await getServiceAccountToken(env.FIREBASE_SERVICE_ACCOUNT_JSON, [
            "https://www.googleapis.com/auth/datastore",
            "https://www.googleapis.com/auth/cloud-platform"
        ]);

        for (const page of response.results) {
            const ticketId = page.properties["Ticket ID"]?.title[0]?.plain_text;
            const status = page.properties["Status"]?.select?.name;
            const adminReply = page.properties["Admin Reply"]?.rich_text[0]?.plain_text || "";
            const userId = page.properties["User ID"]?.rich_text[0]?.plain_text;
            const isClosed = page.properties["Closed"]?.checkbox || false;
            
            if (ticketId && status) {
                let shouldUpdate = true;
                let isSignificantChange = false;
                
                try {
                    const getUrl = `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/support_tickets/${ticketId}`;
                    const getRes = await fetch(getUrl, {
                        headers: { "Authorization": `Bearer ${token}` }
                    });
                    
                    if (getRes.ok) {
                        const existingDoc = await getRes.json();
                        const existingStatus = existingDoc.fields?.status?.stringValue;
                        const existingReply = existingDoc.fields?.adminReply?.stringValue || "";
                        const existingIsClosed = existingDoc.fields?.isClosed?.booleanValue || false;
                        
                        if (existingStatus === status && existingReply === adminReply && existingIsClosed === isClosed) {
                            shouldUpdate = false;
                        } else {
                            isSignificantChange = true; 
                        }
                    } else if (getRes.status === 404) {
                        isSignificantChange = true;
                    }
                } catch (e) {
                    console.error("Error fetching existing ticket:", e);
                }

                if (!shouldUpdate) continue;

                const updated = await updateFirestoreTicket(ticketId, status, adminReply, isClosed, token, env);
                
                if (updated && userId && isSignificantChange) {
                    try {
                        let bodyText = adminReply ? `New reply: ${adminReply.substring(0, 50)}${adminReply.length > 50 ? '...' : ''}` : `Status changed to ${status}`;
                        if (isClosed) bodyText = "This ticket has been closed.";
                        
                        const fcmToken = await getUserFCMToken(userId, env.FIREBASE_SERVICE_ACCOUNT_JSON, env.FIREBASE_PROJECT_ID);
                        if (fcmToken) {
                            const message = {
                                token: fcmToken,
                                android: { priority: "high" },
                                data: {
                                    title: `Support Ticket Update (${ticketId})`,
                                    body: bodyText,
                                    routeType: "support",
                                    routeId: ticketId
                                }
                            };
                            await sendFCMMessage(message, env.FIREBASE_SERVICE_ACCOUNT_JSON, env.FIREBASE_PROJECT_ID);
                        }
                    } catch (fcmErr) {
                        console.error("Failed to trigger push notification for ticket", ticketId, fcmErr);
                    }
                }
            }
        }
    } catch (e) {
        console.error("Error in scheduled sync:", e);
    }
}



async function getUserFCMToken(targetUserId, serviceAccountJson, projectId) {
    const authToken = await getServiceAccountToken(serviceAccountJson, [
        "https://www.googleapis.com/auth/datastore",
        "https://www.googleapis.com/auth/cloud-platform"
    ]);
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/users/${targetUserId}`;
    const res = await fetch(url, {
        headers: { "Authorization": `Bearer ${authToken}` }
    });
    if (!res.ok) {
        console.error("Firestore read error:", res.status, res.statusText, await res.text());
        return null;
    }
    const doc = await res.json();
    return doc.fields?.fcmToken?.stringValue || null;
}

async function updateFirestoreTicket(ticketId, status, adminReply, isClosed, token, env) {
    const url = `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/support_tickets/${ticketId}?updateMask.fieldPaths=status&updateMask.fieldPaths=adminReply&updateMask.fieldPaths=unreadByUser&updateMask.fieldPaths=isClosed`;
    
    const payload = {
        fields: {
            status: { stringValue: status },
            adminReply: { stringValue: adminReply },
            unreadByUser: { booleanValue: true },
            isClosed: { booleanValue: isClosed }
        }
    };

    const response = await fetch(url, {
        method: "PATCH",
        headers: {
            "Authorization": `Bearer ${token}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    });
    
    if (!response.ok) {
        console.error(`Failed to update ticket ${ticketId}: ${response.statusText}`);
        return false;
    }
    return true;
}

// ==========================================
// WebCrypto JWT Logic for FCM v1 REST API
// ==========================================

function base64urlEncode(source) {
  let encoded = btoa(String.fromCharCode.apply(null, new Uint8Array(source)));
  return encoded.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function stringToArrayBuffer(str) {
  const encoder = new TextEncoder();
  return encoder.encode(str);
}

async function getServiceAccountToken(serviceAccountJson, scopes = "https://www.googleapis.com/auth/firebase.messaging") {
  const account = JSON.parse(serviceAccountJson);
  const header = { alg: "RS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const claim = {
    iss: account.client_email,
    scope: Array.isArray(scopes) ? scopes.join(" ") : scopes,
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now
  };

  const headerB64 = base64urlEncode(stringToArrayBuffer(JSON.stringify(header)));
  const claimB64 = base64urlEncode(stringToArrayBuffer(JSON.stringify(claim)));
  const signatureInput = headerB64 + "." + claimB64;

  let pem = account.private_key.replace(/-----BEGIN PRIVATE KEY-----/g, '')
                                 .replace(/-----END PRIVATE KEY-----/g, '')
                                 .replace(/\\n/g, '')
                                 .replace(/\\r/g, '')
                                 .replace(/[^A-Za-z0-9+/=]/g, '');
  pem = pem.padEnd(pem.length + (4 - (pem.length % 4)) % 4, '=');
  
  let binaryDer;
  try {
      binaryDer = Uint8Array.from(atob(pem), c => c.charCodeAt(0));
  } catch (e) {
      throw new Error("Base64 Error. Length: " + pem.length + " Last 10 chars: " + pem.slice(-10) + " Error: " + e.message);
  }


  let key;
  try {
      key = await crypto.subtle.importKey(
        "pkcs8",
        binaryDer.buffer,
        { name: "RSASSA-PKCS1-v1_5", hash: { name: "SHA-256" } },
        false,
        ["sign"]
      );
  } catch(e) {
      throw new Error("PKCS8 Error. PEM starts with: " + account.private_key.substring(0, 35) + " Error: " + e.message);
  }

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    stringToArrayBuffer(signatureInput)
  );

  const signatureB64 = base64urlEncode(signature);
  const jwt = signatureInput + "." + signatureB64;

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`
  });

  const data = await response.json();
  if (data.error) {
      throw new Error(`Auth Error: ${data.error_description || data.error}`);
  }
  return data.access_token;
}

async function sendFCMMessage(message, serviceAccountJson, projectId) {
    const accessToken = await getServiceAccountToken(serviceAccountJson);
    return await sendFCMWithToken(message, projectId, accessToken);
}

// ==========================================
// Scheduled push notifications (cron auto-send)
// ==========================================

/** One access token valid for both Firestore REST and FCM v1. */
async function firestoreAndMessagingToken(env) {
    return getServiceAccountToken(env.FIREBASE_SERVICE_ACCOUNT_JSON, [
        "https://www.googleapis.com/auth/datastore",
        "https://www.googleapis.com/auth/cloud-platform",
        "https://www.googleapis.com/auth/firebase.messaging"
    ]);
}

async function sendFCMWithToken(message, projectId, accessToken) {
    const url = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${accessToken}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ message })
    });
    if (!response.ok) {
        throw new Error(`FCM Error: ${await response.text()}`);
    }
    return await response.json();
}

/**
 * Runs a Firestore structuredQuery (optionally filtered) and returns all docs.
 * Handles both the array-form and object-form REST responses and pages through.
 */
async function runFirestoreQuery(env, accessToken, collectionId, filter) {
    const url = `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents:runQuery`;
    const docs = [];
    let pageToken = null;
    let offset = 0;

    do {
        const structuredQuery = { from: [{ collectionId }], limit: 300 };
        if (filter) {
            structuredQuery.where = {
                fieldFilter: { field: { fieldPath: filter.field }, op: filter.op, value: filter.value }
            };
        }
        const body = { structuredQuery };
        if (pageToken) {
            body.pageToken = pageToken;
        } else if (offset > 0) {
            structuredQuery.offset = offset;
        }

        const res = await fetch(url, {
            method: "POST",
            headers: { "Authorization": `Bearer ${accessToken}`, "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });
        if (!res.ok) {
            throw new Error(`Firestore query error ${res.status}: ${await res.text()}`);
        }
        const data = await res.json();

        if (Array.isArray(data)) {
            let count = 0;
            for (const item of data) {
                if (item && item.document) {
                    docs.push({ id: item.document.name.split('/').pop(), doc: item.document });
                    count++;
                }
            }
            if (count < 300) break; // last page
            offset += 300;
        } else {
            const batch = data.batch || [];
            for (const item of batch) {
                if (item && item.document) {
                    docs.push({ id: item.document.name.split('/').pop(), doc: item.document });
                }
            }
            pageToken = data.nextPageToken || null;
            if (!pageToken) break;
        }
    } while (true);

    return docs;
}

/** Collect non-empty fcmToken values from the users collection (optionally filtered). */
async function collectUserTokens(env, accessToken, field, value) {
    const users = await runFirestoreQuery(
        env,
        accessToken,
        "users",
        field ? { field, op: "EQUAL", value: { stringValue: value } } : null
    );
    const tokens = [];
    for (const u of users) {
        const t = u.doc.fields?.fcmToken?.stringValue;
        if (t && t.trim() !== '') tokens.push(t.trim());
    }
    return tokens;
}

/** Fetch a single user doc's fcmToken with an already-obtained access token. */
async function getUserFCMTokenWithToken(env, accessToken, targetUserId) {
    const url = `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/users/${targetUserId}`;
    const res = await fetch(url, {
        headers: { "Authorization": `Bearer ${accessToken}` }
    });
    if (!res.ok) return null;
    const doc = await res.json();
    return doc.fields?.fcmToken?.stringValue || null;
}

/** PATCH a scheduled_notifications doc (must include all fields being written). */
async function updateScheduledNotification(env, accessToken, id, patch) {
    const fields = {
        status: { stringValue: patch.status },
        sentAt: { timestampValue: new Date().toISOString() }
    };
    if (typeof patch.recipientCount === 'number') fields.recipientCount = { integerValue: patch.recipientCount };
    if (typeof patch.failedCount === 'number') fields.failedCount = { integerValue: patch.failedCount };
    if (patch.error) fields.error = { stringValue: String(patch.error).slice(0, 300) };

    const paths = Object.keys(fields).map(p => `updateMask.fieldPaths=${encodeURIComponent(p)}`).join('&');
    const url = `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/scheduled_notifications/${id}?${paths}`;
    const res = await fetch(url, {
        method: "PATCH",
        headers: { "Authorization": `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({ fields })
    });
    if (!res.ok) {
        throw new Error(`Firestore update error ${res.status}: ${await res.text()}`);
    }
}

/** Run `fn` over items with at most `limit` in flight at once. */
async function mapLimit(items, limit, fn) {
    const results = new Array(items.length);
    let next = 0;
    const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
        while (next < items.length) {
            const i = next++;
            results[i] = await fn(items[i], i);
        }
    });
    await Promise.all(workers);
    return results;
}

/**
 * Finds `scheduled_notifications` with status == 'scheduled' and scheduledFor <= now,
 * resolves recipients, and sends them via FCM v1. Marks each doc sent/partial/failed.
 */
async function processScheduledNotifications(env) {
    const accessToken = await firestoreAndMessagingToken(env);
    const now = new Date();
    const due = await runFirestoreQuery(env, accessToken, "scheduled_notifications", {
        field: "status", op: "EQUAL", value: { stringValue: "scheduled" }
    });

    for (const entry of due) {
        const id = entry.id;
        const doc = entry.doc.fields || {};
        const scheduledFor = doc.scheduledFor?.timestampValue;
        if (!scheduledFor || new Date(scheduledFor) > now) continue; // not due yet

        const recipient = doc.recipient?.stringValue || 'all';
        const title = doc.title?.stringValue || '';
        const body = doc.body?.stringValue || '';
        const channel = doc.channel?.stringValue || 'general';
        const imageUrl = doc.imageUrl?.stringValue || null;
        const deepLink = doc.deepLink?.stringValue || null;

        // Resolve FCM tokens for the recipient group
        let tokens = [];
        try {
            if (recipient === 'all') {
                tokens = await collectUserTokens(env, accessToken, null, null);
            } else if (recipient === 'students') {
                tokens = await collectUserTokens(env, accessToken, 'role', 'student');
            } else if (recipient === 'owners') {
                tokens = await collectUserTokens(env, accessToken, 'role', 'owner');
            } else if (recipient.startsWith('city:')) {
                tokens = await collectUserTokens(env, accessToken, 'city', recipient.replace('city:', ''));
            } else if (recipient.startsWith('user:') || recipient.startsWith('uid:') || recipient.includes(',')) {
                const uids = recipient.replace(/^(user:|uid:)/, '').split(',').map(s => s.trim()).filter(Boolean);
                for (const uid of uids) {
                    const t = await getUserFCMTokenWithToken(env, accessToken, uid);
                    if (t) tokens.push(t);
                }
            } else {
                const t = await getUserFCMTokenWithToken(env, accessToken, recipient);
                if (t) tokens.push(t);
            }
        } catch (e) {
            console.error(`Token resolution error for scheduled notification ${id}:`, e);
        }

        tokens = [...new Set(tokens)];

        if (tokens.length === 0) {
            await updateScheduledNotification(env, accessToken, id, {
                status: 'failed',
                recipientCount: 0,
                failedCount: 0,
                error: 'No active FCM tokens found for the selected recipient group'
            });
            console.warn(`Scheduled notification ${id}: no active FCM tokens`);
            continue;
        }

        // Send to each token with bounded concurrency
        let firstError = null;
        const results = await mapLimit(tokens, 20, async (token) => {
            try {
                await sendFCMWithToken({
                    token,
                    android: { priority: "high" },
                    notification: {
                        title,
                        body,
                        ...(imageUrl ? { image: imageUrl } : {})
                    },
                    data: {
                        title,
                        body,
                        routeType: channel,
                        routeId: '',
                        channelId: resolveChannelId(channel),
                        ...(deepLink ? { route: deepLink } : {})
                    }
                }, env.FIREBASE_PROJECT_ID, accessToken);
                return true;
            } catch (e) {
                if (!firstError) firstError = e.message || String(e);
                return false;
            }
        });

        const successCount = results.filter(Boolean).length;
        const failureCount = results.length - successCount;

        await updateScheduledNotification(env, accessToken, id, {
            status: failureCount > 0 ? 'partial' : 'sent',
            recipientCount: successCount,
            failedCount: failureCount,
            ...(firstError ? { error: String(firstError).slice(0, 300) } : {})
        });

        console.log(`Scheduled notification ${id}: sent=${successCount} failed=${failureCount}`);
    }
}
