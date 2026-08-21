const admin = require('firebase-admin');
const serviceAccount = require('../stay-buddy-9d294-firebase-adminsdk-fbsvc-97b073b608.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: serviceAccount.project_id
});

async function sendTestPush() {
    try {
        const db = admin.firestore();
        const tickets = await db.collection('support_tickets').get();
        console.log(`Read ${tickets.size} tickets.`);
        const message = {
            data: {
                title: "Test Notification",
                body: "Push notifications are working in the background!",
                routeType: "default",
                channelId: "staybuddy_messages"
            },
            android: {
                priority: "high"
            },
            topic: "audience_students"
        };
        
        const response = await admin.messaging().send(message);
        console.log('Successfully sent message to topic audience_students:', response);

        const notificationMessage = {
            notification: {
                title: "Test System Notification",
                body: "This one has the notification block included."
            },
            data: {
                title: "Test System Notification",
                body: "This one has the notification block included."
            },
            android: {
                priority: "high"
            },
            topic: "audience_students"
        };
        const response2 = await admin.messaging().send(notificationMessage);
        console.log('Successfully sent notification block message to topic audience_students:', response2);

    } catch (error) {
        console.error('Error sending message:', error);
    }
}

sendTestPush();
