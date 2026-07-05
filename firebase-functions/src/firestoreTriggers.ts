import * as functions from 'firebase-functions/v1';
import { sendToTopic } from './notificationService';

/**
 * Triggered when a new aid distribution is recorded.
 */
export const onAidDistributionCreated = functions.firestore
    .document('aidDistributions/{distributionId}')
    .onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        if (!data) return;

        await sendToTopic('aid_updates', {
            title: 'Aid Distribution Completed',
            body: `New aid recorded for beneficiary ${data.beneficiaryId}.`,
            data: {
                type: 'AID_DISTRIBUTION',
                id: context.params.distributionId,
                beneficiaryId: data.beneficiaryId
            }
        });
    });

/**
 * Triggered when a new verification visit is recorded.
 */
export const onVerificationVisitCreated = functions.firestore
    .document('verificationVisits/{visitId}')
    .onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        if (!data) return;

        await sendToTopic('visit_updates', {
            title: 'Verification Visit Conducted',
            body: `Visit completed for ${data.beneficiaryName || 'beneficiary'}.`,
            data: {
                type: 'VERIFICATION_VISIT',
                id: context.params.visitId
            }
        });
    });

/**
 * Triggered when a donation collection is recorded.
 */
export const onDonationCollectionCreated = functions.firestore
    .document('donationCollections/{collectionId}')
    .onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        if (!data) return;

        await sendToTopic('donation_updates', {
            title: 'Donation Box Collected',
            body: `Amount ₹${data.amount} collected from box ${data.boxId}.`,
            data: {
                type: 'DONATION_COLLECTION',
                id: context.params.collectionId,
                amount: String(data.amount)
            }
        });
    });

/**
 * Triggered when a new event is created.
 */
export const onEventCreated = functions.firestore
    .document('events/{eventId}')
    .onCreate(async (snapshot, context) => {
        const data = snapshot.data();
        if (!data) return;

        await sendToTopic('event_updates', {
            title: 'New Campaign/Event',
            body: `${data.title}: ${data.description}`,
            data: {
                type: 'EVENT',
                id: context.params.eventId
            }
        });
    });
