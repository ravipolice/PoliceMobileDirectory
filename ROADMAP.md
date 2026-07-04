# 🗺️ Police Mobile Directory - Future Enhancements Roadmap

This document outlines the planned improvements for the Police Mobile Directory (PMD) application. We will proceed section-by-step.

---

## 🔐 Section 1: Login & Authentication
- [x] **Biometric Login**: Integrate Android `BiometricPrompt` API to allow logging in with Fingerprint or Face ID instead of typing the 6-digit PIN.
  * *UX*: Automatically prompts user for fingerprint/face scan after the first successful login, and provides a toggle in the Navigation Drawer to enable/disable biometric login at any time.
  * *Tech*: Uses Android `BiometricPrompt` library, Encrypted keystore decryption, and custom PIN verification for drawer configuration.
  * *Benefit*: Instant, passwordless offline login with clear management inside the app.
- [x] **Google Sign-In Account Auto-Switch**: Allow users to easily switch or disconnect their linked Google Account directly from the Login page.
  * *UX*: Adds a "Switch Account" button on the login screen.
  * *Tech*: Calls `.signOut()` and `.revokeAccess()` on GoogleSignInClient/CredentialManager to clear Google credentials.
  * *Benefit*: Easily correct accidental links to incorrect accounts without clearing app data.
- [x] **Brute Force Protection**: Lock the local offline PIN entry after 5 consecutive failed attempts for 10 minutes to protect data.
  * *UX*: Freezes the entry screen with a countdown timer after 5 wrong attempts.
  * *Tech*: Securely stores failed attempts count and timestamp in DataStore.
  * *Benefit*: Prevents local brute-force guessing of the 6-digit PIN.
- [x] **Session Expiry & Security**: Implement automatic session timeout/forced logouts if the app remains inactive for a long period (e.g., 30 days).
  * *UX*: Forces a Google re-login if the app has not been opened for more than 30 days.
  * *Tech*: Updates a `last_active_time` timestamp on launch and clears Room cache if expired.
  * *Benefit*: Automatically revokes data access for retired or suspended personnel.

---

## 📝 Section 2: Registration & Pending Approvals
- [ ] **Real-Time Registration Status Tracking**: Show a clean timeline on the user app (e.g., *Submitted* ➔ *Under Review* ➔ *Approved/Rejected*) so the user knows exactly where their request stands.
- [ ] **Detailed Rejection Log**: Show a detailed pop-up when a registration is rejected, including the rejection date and step-by-step instructions on what needs correcting (e.g., "Invalid KGID number").
- [ ] **Admin Approval Push Notification**: Automatically trigger a push notification to all admins when a new user registers so approvals are done faster.
- [ ] **Photo Quality Verification**: Add automatic image size and resolution validation during registration so users do not upload oversized/blurry photos.

---

## 🔍 Section 3: Employee Directory & Search
- [ ] **Unified Search Auto-Suggestions**: Show instant search results and query auto-completions as the user types.
- [ ] **Recent Searches & Favorites**: Cache the last 5 searches and allow users to "star" or bookmark specific contacts for quick access.
- [ ] **Kannada Font/Keyboard Toggle**: Add an in-app button to quickly switch search queries between English and Kannada (integrating with the Nudi converter).
- [ ] **Click-to-Action Shortcuts**: Add instant action buttons (Call, SMS, Email, WhatsApp) directly on the contact cards in the list, reducing the need to open the full details page.

---

## 🛠️ Section 4: Admin Panel & Personnel Management
- [ ] **Bulk Import Validation Preview**: When importing a CSV list of employees, show a preview page highlighting any errors (e.g., duplicate KGIDs, invalid phone numbers) before committing them to the database.
- [ ] **Manual Role Upgrades**: Allow super-admins to elevate regular employees to admin status or revoke admin privileges directly from the employee details screen.
- [ ] **Employee Activity Logs**: Maintain an audit trail of major actions (who edited a contact, who approved a user, when a backup was run).

---

## 📁 Section 5: Documents & Media (Gallery)
- [ ] **Document Categories & Search**: Group documents by departments/circulars and add a search bar inside the Document tab.
- [ ] **Offline PDF Caching**: Allow users to download circular PDFs so they can read them offline later.
- [ ] **Image Compression & Crop Enhancements**: Improve the profile photo uCrop settings to enforce a perfect circle aspect ratio.

---

## 🏗️ Section 6: Code & System Architecture
- [ ] **ViewModel Separation**: Split the monolithic 2,400+ line `EmployeeViewModel.kt` into clean, testable sub-ViewModels (`AuthViewModel`, `DirectoryViewModel`, `NotificationViewModel`, `ProfileViewModel`).
- [ ] **Centralized Error Boundary**: Implement a global error handling system to show user-friendly prompts for network errors instead of generic Toast messages.
