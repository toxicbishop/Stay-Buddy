const admin = require('firebase-admin');

// Initialize Firebase Admin if not already initialized
if (!admin.apps.length) {
    try {
        const credentialsJson = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
        
        let credential;
        // Check if it's a User Refresh Token (Application Default Credentials)
        if (credentialsJson.type === 'authorized_user') {
            credential = admin.credential.refreshToken(credentialsJson);
        } else {
            // Otherwise, assume it's a standard Service Account key
            credential = admin.credential.cert(credentialsJson);
        }

        admin.initializeApp({
            credential: credential
        });
        console.log("Firebase Admin initialized successfully.");
    } catch (error) {
        console.error("Firebase Admin initialization error", error);
    }
}

export default async function handler(req, res) {
    // Only allow POST requests
    if (req.method !== 'POST') {
        return res.status(405).json({ error: 'Method Not Allowed' });
    }

    try {
        const { targetUserId, title, body, routeType, routeId, avatarUrl } = req.body;

        if (!targetUserId || !title || !body) {
            return res.status(400).json({ error: 'Missing required fields' });
        }

        // Fetch user's FCM token from Firestore
        const userDoc = await admin.firestore().collection('users').doc(targetUserId).get();
        if (!userDoc.exists) {
            return res.status(404).json({ error: 'User not found' });
        }

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) {
            return res.status(404).json({ error: 'User does not have an FCM token registered' });
        }

        // Create the notification payload
        const message = {
            token: fcmToken,
            data: {
                title: title,
                body: body,
                routeType: routeType || 'default',
                routeId: routeId || '',
                avatarUrl: avatarUrl || ''
            }
        };

        // Send the notification
        const response = await admin.messaging().send(message);
        console.log('Successfully sent message:', response);
        
        return res.status(200).json({ success: true, messageId: response });

    } catch (error) {
        console.error('Error sending message:', error);
        return res.status(500).json({ error: 'Internal Server Error', details: error.message });
    }
}
