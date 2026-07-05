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
exports.onEventCreated = exports.onDonationCollectionCreated = exports.onVerificationVisitCreated = exports.onAidDistributionCreated = void 0;
const functions = __importStar(require("firebase-functions/v1"));
const notificationService_1 = require("./notificationService");
exports.onAidDistributionCreated = functions.firestore
    .document('aidDistributions/{distributionId}')
    .onCreate(async (snapshot, context) => {
    const data = snapshot.data();
    if (!data)
        return;
    await (0, notificationService_1.sendToTopic)('aid_updates', {
        title: 'Aid Distribution Completed',
        body: `New aid recorded for beneficiary ${data.beneficiaryId}.`,
        data: {
            type: 'AID_DISTRIBUTION',
            id: context.params.distributionId,
            beneficiaryId: data.beneficiaryId
        }
    });
});
exports.onVerificationVisitCreated = functions.firestore
    .document('verificationVisits/{visitId}')
    .onCreate(async (snapshot, context) => {
    const data = snapshot.data();
    if (!data)
        return;
    await (0, notificationService_1.sendToTopic)('visit_updates', {
        title: 'Verification Visit Conducted',
        body: `Visit completed for ${data.beneficiaryName || 'beneficiary'}.`,
        data: {
            type: 'VERIFICATION_VISIT',
            id: context.params.visitId
        }
    });
});
exports.onDonationCollectionCreated = functions.firestore
    .document('donationCollections/{collectionId}')
    .onCreate(async (snapshot, context) => {
    const data = snapshot.data();
    if (!data)
        return;
    await (0, notificationService_1.sendToTopic)('donation_updates', {
        title: 'Donation Box Collected',
        body: `Amount ₹${data.amount} collected from box ${data.boxId}.`,
        data: {
            type: 'DONATION_COLLECTION',
            id: context.params.collectionId,
            amount: String(data.amount)
        }
    });
});
exports.onEventCreated = functions.firestore
    .document('events/{eventId}')
    .onCreate(async (snapshot, context) => {
    const data = snapshot.data();
    if (!data)
        return;
    await (0, notificationService_1.sendToTopic)('event_updates', {
        title: 'New Campaign/Event',
        body: `${data.title}: ${data.description}`,
        data: {
            type: 'EVENT',
            id: context.params.eventId
        }
    });
});
//# sourceMappingURL=firestoreTriggers.js.map