"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.messaging = exports.db = void 0;
exports.sendToTopic = sendToTopic;
exports.sendToUser = sendToUser;
const admin = __importStar(require("firebase-admin"));
if (admin.apps.length === 0) {
    admin.initializeApp();
}
exports.db = admin.firestore();
exports.messaging = admin.messaging();
async function sendToTopic(topic, payload) {
    const message = {
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
        const response = await exports.messaging.send(message);
        console.log(`Successfully sent message to topic ${topic}:`, response);
        await exports.db.collection('notificationLogs').add({
            topic: topic,
            title: payload.title,
            body: payload.body,
            status: 'SUCCESS',
            messageId: response,
            createdAt: Date.now(),
        });
        return response;
    }
    catch (error) {
        console.error(`Error sending message to topic ${topic}:`, error);
        await exports.db.collection('notificationLogs').add({
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
async function sendToUser(userId, payload) {
    const userDoc = await exports.db.collection('users').doc(userId).get();
    const userData = userDoc.data();
    if (!userData || !userData.fcmToken) {
        console.warn(`User ${userId} has no FCM token.`);
        return null;
    }
    const message = {
        token: userData.fcmToken,
        notification: {
            title: payload.title,
            body: payload.body,
        },
        data: payload.data,
    };
    return exports.messaging.send(message);
}
//# sourceMappingURL=notificationService.js.map