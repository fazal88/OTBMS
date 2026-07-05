# Olive Trust Charity - Enhanced User Journey Roadmap

This document outlines the core operational flows and the robust accountability measures integrated into the Olive Trust mobile application.

---

## 1. Beneficiary Support & Accountability Cycle

### Phase A: Onboarding (Employee)
*   **Action**: Field Employee visits a potential beneficiary's location.
*   **Process**:
    *   Fills out the comprehensive digital Onboarding Form.
    *   **Mandatory GPS Logging**: The app captures the exact latitude and longitude of the onboarding site. This ensures the employee is physically present at the beneficiary's doorstep.
*   **Outcome**: Application moves to "Pending Approval" status.

### Phase B: Verification & Aid Allocation (Approver)
*   **Action**: Approver reviews the application.
*   **Process**:
    *   Assigns **Aid Type**: Ration, Monetary, or Both.
    *   Sets **Aid Expiry**: Defines a clear re-evaluation timeline (e.g., 6 months).
    *   **Monitor Assignment**: Assigns a specific Monitor (Employee) to the beneficiary for long-term tracking.
*   **Outcome**: Beneficiary is "Approved."

### Phase C: Distribution Events (Approver & Employee)
*   **Planning**: Approvers can create **Planned Distribution Events** (e.g., "Ramadan Ration Drive").
*   **Invitees**: Specific approved beneficiaries are added to these events as invitees.
*   **Execution**:
    *   Employee records the distribution during the event.
    *   **Evidence Collection**: Captures receiver's name, signature, and optional evidence photo.
    *   **Event Tracking**: Links individual aid delivery to the specific planned event for bulk reporting.

### Phase D: Ad-hoc Aid Distribution (Employee)
*   **Action**: Regular monthly delivery to a beneficiary's home.
*   **Process**:
    *   Employee records the delivery.
    *   **Geofencing Proof**: The system logs GPS coordinates at the moment of distribution. If the delivery location doesn't match the onboarding location, it's flagged for review.

### Phase E: Verification Visits (Employee/Monitor)
*   **Action**: Unannounced or scheduled follow-up visits.
*   **Process**:
    *   Records "Visit Success", "Edit Required", or "Misuse Reported".
    *   **Location Validation**: Every visit must be recorded with GPS data, creating a historical breadcrumb trail of monitoring.

---

## 2. Donation Box Management

### Step 1: Installation (Collector)
*   **Action**: Collector installs a physical donation box at a partner shop or mosque.
*   **Process**:
    *   Records shop details and contact person.
    *   **GPS Pinning**: The exact location of the box is saved. This allows the trust to map all physical touchpoints in the city.
*   **Outcome**: Box appears as "Pending Approval."

### Step 2: Verification (Approver)
*   **Action**: Approver reviews the installation.
*   **Outcome**: Box status changes to "Active."

### Step 3: Collection & Transparency (Collector)
*   **Action**: Visit to collect funds.
*   **Process**:
    *   Records the collected amount.
    *   **Location Verification**: GPS is recorded to prove the collector visited the shop.
    *   **Instant Transparency**: The app generates a message for the shop owner.
    *   **WhatsApp Integration**: Shares collection details (Amount + Box ID + Date) directly with the partner for immediate trust building.

---

## 3. The "Silent Auditor" (Security & Logs)

### Universal GPS Recording
The app acts as a "silent auditor" by recording **Latitude and Longitude** at every critical step:
*   Onboarding a beneficiary.
*   Approving aid (records approver's login location).
*   Distributing aid (delivery location).
*   Performing verification visits.
*   Installing/Collecting from donation boxes.

### Full Audit Trail
Every single action (Creating, Editing, Deleting, or Approving) is captured in the **Audit Log**:
*   **WHO**: The user ID and role.
*   **WHAT**: The action taken (e.g., "UPDATE_EMPLOYEE_STATUS").
*   **WHEN**: Precise timestamp.
*   **WHERE**: GPS coordinates and Device ID.
*   **CHANGE**: Before/After values (e.g., "Status changed from ACTIVE to SUSPENDED").

### Stakeholder Dashboard
Admins and Approvers have a bird's-eye view:
*   **Performance Metrics**: Monthly aid distributed vs. monthly collections.
*   **Attention Required**: Real-time alerts for "Misuse Reported", "Expired Aid", or "Pending Device Approvals".
*   **Geographical Oversight**: Visualizing where activities are happening across the city.
