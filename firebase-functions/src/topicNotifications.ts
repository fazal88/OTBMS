import * as functions from 'firebase-functions/v1';
import { sendToTopic } from './notificationService';

export const sendTestNotification = functions.https.onCall(async (data, context) => {
    // Open HTTP endpoint - no context.auth check
    const { topic, title, body } = data;

    if (!topic || !title || !body) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing topic, title, or body.');
    }

    try {
        await sendToTopic(topic, { title, body });
        return { success: true };
    } catch (error) {
        throw new functions.https.HttpsError('internal', 'Failed to send notification: ' + error);
    }
});

export const sendManualTopicNotification = functions.https.onCall(async (data, context) => {
    // Open HTTP endpoint - no context.auth check
    const { topic, title, body, extraData } = data;

    try {
        await sendToTopic(topic, { title, body, data: extraData });
        return { success: true };
    } catch (error) {
        throw new functions.https.HttpsError('internal', 'Failed to send notification.');
    }
});
