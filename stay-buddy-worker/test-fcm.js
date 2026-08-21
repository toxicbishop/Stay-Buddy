const { google } = require('googleapis');
const fs = require('fs');

async function testPush() {
    try {
        const keyData = JSON.parse(fs.readFileSync('../stay-buddy-9d294-firebase-adminsdk-fbsvc-97b073b608.json', 'utf8'));
        
        // Use JWT to get access token
        const jwtClient = new google.auth.JWT({
            email: keyData.client_email,
            key: keyData.private_key,
            scopes: ['https://www.googleapis.com/auth/firebase.messaging', 'https://www.googleapis.com/auth/datastore']
        });
        
        const token = await jwtClient.authorize();
        console.log("Got access token");
        
        // 1. Fetch some tokens from Firestore
        const projectId = keyData.project_id;
        const firestoreUrl = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/users`;
        
        const fsResponse = await fetch(firestoreUrl, {
            headers: { Authorization: `Bearer ${token.access_token}` }
        });
        
        const fsData = await fsResponse.json();
        const users = fsData.documents || [];
        
        const fcmTokens = [];
        for (const user of users) {
            if (user.fields && user.fields.fcmToken && user.fields.fcmToken.stringValue) {
                const fcm = user.fields.fcmToken.stringValue;
                if (!fcmTokens.includes(fcm)) {
                    fcmTokens.push(fcm);
                }
            }
        }
        
        console.log(`Found ${fcmTokens.length} unique FCM tokens`);
        
        // 2. Send to all tokens
        for (const fcmToken of fcmTokens) {
            const message = {
                message: {
                    token: fcmToken,
                    android: { priority: "high" },
                    data: {
                        title: "Test Notification",
                        body: "If you see this, push notifications are working!",
                        routeType: "default"
                    }
                }
            };
            
            console.log(`Sending to ${fcmToken.substring(0, 20)}...`);
            const sendUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
            const sendResponse = await fetch(sendUrl, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token.access_token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(message)
            });
            
            const result = await sendResponse.json();
            if (sendResponse.ok) {
                console.log("Success:", result);
            } else {
                console.error("Failed:", result);
            }
        }
        
    } catch (e) {
        console.error("Error:", e);
    }
}

testPush();
