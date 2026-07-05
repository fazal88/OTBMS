import * as admin from 'firebase-admin';

if (admin.apps.length === 0) {
    admin.initializeApp();
}

export const db = admin.firestore();
export const messaging = admin.messaging();

export interface NotificationPayload {
    title: string;
    body: string;
    data?: { [key: string]: string };
}

export async function sendToTopic(topic: string, payload: NotificationPayload) {
    const message: admin.messaging.Message = {
        topic: topic,
        notification: {
            title: payload.title,
            body: payload.body,
        },
        data: payload.data,
        android: {
            priority: 'high',
            notification: {
                sound: 'default',
            },
        },
        apns: {
            payload: {
                aps: {
                    sound: 'default',
                    badge: 1,
                },
            },
        },
    };

    try {
        const response = await messaging.send(message);
        console.log(`Successfully sent message to topic ${topic}:`, response);

        // Log to Firestore
        await db.collection('notificationLogs').add({
            topic: topic,
            title: payload.title,
            body: payload.body,
            status: 'SUCCESS',
            messageId: response,
            createdAt: Date.now(),
        });

        return response;
    } catch (error) {
        console.error(`Error sending message to topic ${topic}:`, error);

        await db.collection('notificationLogs').add({
            topic: topic,
            title: payload.title,
            body: payload.body,
            status: 'FAILED',
            error: String(error),
            createdAt: Date.now(),
        });

        throw error;
    }
}

export async function sendToUser(userId: string, payload: NotificationPayload) {
    const userDoc = await db.collection('users').doc(userId).get();
    const userData = userDoc.data();

    if (!userData || !userData.fcmToken) {
        console.warn(`User ${userId} has no FCM token.`);
        return null;
    }

    const message: admin.messaging.Message = {
        token: userData.fcmToken,
        notification: {
            title: payload.title,
            body: payload.body,
        },
        data: payload.data,
    };

    return messaging.send(message);
}
