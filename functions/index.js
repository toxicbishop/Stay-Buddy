/**
 * ⚠️ DEPRECATED — Firebase Functions are NOT deployed or used.
 * All backend logic (push notifications, Notion sync, support tickets)
 * lives in the Cloudflare Worker at: staybuddy-worker/src/index.js
 *
 * This file is kept ONLY as a rollback reference.
 * To re-enable, add the "functions" block back to firebase.json and deploy.
 *
 * Original endpoints:
 *   - onChatMessage: Firestore trigger → push notification on new chat message
 *   - onNewInquiry: Firestore trigger → push notification on new inquiry
 *   - onTicketCreated: Firestore trigger → Notion page creation
 *
 * NOTE: The original code used admin.messaging().sendToDevice() which is
 * DEPRECATED (legacy FCM HTTP protocol shut down June 2024).
 * If re-enabling, migrate to admin.messaging().send() (FCM v1 API).
 */

// const functions = require("firebase-functions");
// const admin = require("firebase-admin");
// admin.initializeApp();
// See git history for full original implementation.
