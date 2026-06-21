# Bagify — User Accounts Management: Epics & Stories

> **Scope:** May 2026 deadline | Frontend: React + TypeScript (Vite) | Backend: Spring Boot (Java) microservice  
> **Analysis date:** 2026-05-06

---

## 🗺️ Codebase Snapshot (Pre-Work)

### What Already Works ✅
| Area | Status |
|---|---|
| Registration (BUYER/SELLER) wired to real API | ✅ Done |
| Login with JWT → role-based navigation | ✅ Done |
| AuthContext (user + token in localStorage) | ✅ Done |
| Protected routes with role enforcement | ✅ Done |
| Edit profile (name, email, phone, address) | ✅ Connected to API |
| Profile photo upload via Cloudinary | ✅ Done |
| Seller verification form + document upload | ✅ UI + API call done |
| Logout | ✅ Done |
| Admin: `disableUser` endpoint exists | ✅ Backend done |
| Backend: `updateProfile` supports password change | ✅ Done (via UpdateUserRequest) |

### Known Gaps ❌
| Gap | Where |
|---|---|
| No Change Password UI | Frontend only |
| No Forgot/Reset Password flow | Both |
| No refresh token / auto-logout on expiry | Both |
| `authService.getCurrentUser()` uses mock data | Frontend |
| Signup sends lowercase `"buyer"/"seller"` — backend expects it lowercase but RegisterRequest type says `"BUYER"/"SELLER"` | Frontend type mismatch |
| No tab/section switcher UI in SellerEditProfile (only URL param) | Frontend |
| No Delete Account feature | Both |
| Admin user management uses mock data | Frontend |
| Admin seller verification review panel missing | Both |
| `AuthProvider` loading state shows bare `<div>Loading...</div>` | Frontend |
| JWT expiry is 24h; requirement is 30min + refresh token | Backend |

---

## Epic 1 — Authentication & Session Management

**Goal:** Users can securely register, log in, recover their password, change their password, and be automatically managed when their token expires.

---

### Story 1.1 — Fix Registration Form Bug
**As a** new user, **I want** the signup form to correctly send my role to the backend, **so that** my account is created with the right role.

**Background:** The `<select>` options send `"buyer"/"seller"` (lowercase) which the backend accepts, but the TypeScript `RegisterRequest` type declares `"BUYER" | "SELLER"` causing a type mismatch.

#### Tasks
**Frontend**
- [ ] Fix `SignupPage.tsx` `<select>` option values to match what the backend accepts (`"BUYER"` / `"SELLER"` or lowercase — align with backend's `.toLowerCase()` call)
- [ ] Update `RegisterRequest` type in `types/index.ts` to `userrole: 'BUYER' | 'SELLER'` (keep consistent)
- [ ] Add full client-side form validation: name min 2 chars, valid email format, password min 8 chars, password strength hint
- [ ] Show inline field-level error messages (not just toast)
- [ ] Disable the submit button until required fields are filled

**Acceptance Criteria**
- Registering as BUYER creates a Buyer entity; registering as SELLER creates a Seller entity
- All validation errors appear inline beneath each field
- No TypeScript type warnings on the registration form

---

### Story 1.2 — Fix AuthService `getCurrentUser()` (Remove Mock)
**As a** logged-in user, **I want** my real profile data to be fetched from the backend on page load, **so that** I always see accurate information.

**Background:** `authService.getCurrentUser()` currently returns `mockBuyers[0]` unconditionally.

#### Tasks
**Frontend**
- [ ] Replace mock in `authService.getCurrentUser()` with a real `GET /api/v1/users/me` call using the stored token
- [ ] Update `AuthProvider.tsx` to call `getCurrentUser()` if a token exists in localStorage (to refresh stale user data on reload)
- [ ] Map the backend `User` response (which includes `profileImageUrl`) to the frontend `User` type (`profileImage`)
- [ ] Replace the bare `<div>Loading...</div>` in `AuthProvider` with a proper full-screen loading spinner component

**Backend**
- [ ] The `GET /api/v1/users/me` endpoint already exists. Verify response includes all needed fields (`id`, `name`, `email`, `phone`, `address`, `profileImageUrl`, `role`, `enabled`)
- [ ] Ensure `AuthResponse` and `User` entity do **not** serialize `password` in JSON responses (add `@JsonIgnore` on password field)

**Acceptance Criteria**
- On hard refresh, the logged-in user's real name and photo appear in the header
- `password` field is never returned in any JSON response

---

### Story 1.3 — Change Password
**As a** logged-in user, **I want** to change my password from my profile settings, **so that** I can keep my account secure.

**Background:** The backend `updateProfile` method in `UserService` already supports password update via `UpdateUserRequest.password` + `confirmPassword`. Only the frontend UI and a dedicated endpoint are missing.

#### Tasks
**Frontend**
- [ ] Add a "Security" tab/section to the Edit Profile page (visible for all roles)
- [ ] Create `ChangePasswordForm.tsx` component with fields: Current Password, New Password, Confirm New Password
- [ ] Add validation: current password required, new password min 8 chars, passwords must match
- [ ] Call `PUT /api/v1/users/profile` with `{ password, confirmPassword }` on submit
- [ ] On success, show toast "Password changed. Please log in again." then call `logout()` and redirect to `/login`
- [ ] Add loading state to the submit button

**Backend**
- [ ] Add `currentPassword` field to `UpdateUserRequest.java`
- [ ] Add `POST /api/v1/users/change-password` endpoint in `UserController.java`
- [ ] In `UserService`, verify `currentPassword` matches stored hash before allowing change
- [ ] Return `400 Bad Request` with message `"Current password is incorrect"` if it doesn't match
- [ ] Ensure the existing `SecurityConfig` allows this endpoint only for authenticated users (already covered by `.anyRequest().authenticated()`)

**Acceptance Criteria**
- User cannot change password without providing the correct current password
- After successful change, the user is logged out and redirected to login
- Error message shown if current password is wrong

---

### Story 1.4 — Forgot Password / Reset Password
**As a** user who has forgotten their password, **I want** to receive a reset link by email, **so that** I can regain access to my account.

#### Tasks
**Backend**
- [ ] Add `passwordResetToken` (String) and `passwordResetTokenExpiry` (LocalDateTime) fields to `User.java` entity
- [ ] Create `ForgotPasswordRequest.java` DTO with `email` field
- [ ] Create `ResetPasswordRequest.java` DTO with `token`, `newPassword`, `confirmPassword` fields
- [ ] Add `POST /api/v1/auth/forgot-password` endpoint in `AuthController.java`
  - Find user by email (silently succeed if not found to prevent email enumeration)
  - Generate secure random token (UUID), store hashed in DB, set expiry to 15 minutes
  - Send email with reset link (e.g. using Spring Boot Mail / Mailgun)
- [ ] Add `POST /api/v1/auth/reset-password` endpoint in `AuthController.java`
  - Validate token exists, matches, and has not expired
  - Update password; clear token fields
  - Return `200 OK`
- [ ] Add both new endpoints to `SecurityConfig` `permitAll()` list

**Frontend**
- [ ] Create `ForgotPasswordPage.tsx` with email input form, route `/forgot-password`
- [ ] Create `ResetPasswordPage.tsx` with new password + confirm fields, reads `?token=` from URL, route `/reset-password`
- [ ] Add "Forgot password?" link on `LoginPage.tsx`
- [ ] Add both routes to `AppRoutes.tsx` (public, no `ProtectedRoute`)
- [ ] Add `forgotPassword(email)` and `resetPassword(token, newPassword, confirmPassword)` methods to `authservice.ts`

**Acceptance Criteria**
- If user enters a registered email, they receive a reset email within 60 seconds
- Reset link expires after 15 minutes
- After successful reset, user is redirected to login with a success message
- Entering an invalid/expired token shows an appropriate error

---

### Story 1.5 — JWT Expiry (30 min) + Refresh Token
**As a** user, **I want** my session to be automatically renewed while I'm active, **so that** I am not abruptly logged out mid-use.

**Background:** Current JWT expiry is 24 hours (`86_400_000L` ms). Requirement: access token = 30 min, refresh token = 7 days.

#### Tasks
**Backend**
- [ ] Change `JwtUtil.generateToken()` expiry from `86_400_000L` (24h) to `1_800_000L` (30 min)
- [ ] Add `generateRefreshToken(User user)` method in `JwtUtil.java` — expiry 7 days, minimal claims (userId only)
- [ ] Update `AuthResponse.java` to include `refreshToken` field alongside `token`
- [ ] Update `UserService.authenticate()` to generate and return both tokens
- [ ] Add `POST /api/v1/auth/refresh` endpoint in `AuthController.java`
  - Accept `{ refreshToken }` in request body
  - Validate refresh token (not expired, user exists, user is enabled)
  - Return new `{ token, refreshToken }`
- [ ] Add `/api/v1/auth/refresh` to `SecurityConfig` `permitAll()` list

**Frontend**
- [ ] Update `AuthContext` / `AuthProvider` to store `refreshToken` in localStorage
- [ ] Update `login()` in `AuthProvider.tsx` to save both `token` and `refreshToken`
- [ ] Create `src/api/tokenRefresher.ts` — an interceptor/utility that:
  - On any `401` response, attempts `POST /api/v1/auth/refresh` with stored refresh token
  - On success, stores new access token and retries the original request
  - On refresh failure (refresh token expired), calls `logout()` and redirects to `/login`
- [ ] Integrate `tokenRefresher` into `httpClient.ts` response handling
- [ ] Update `userservice.ts` direct `fetch()` calls to go through `httpClient` so they benefit from the interceptor

**Acceptance Criteria**
- Access token expires after 30 minutes; refresh token after 7 days
- An active user is never logged out mid-session due to token expiry
- An idle user who returns after >30 min is refreshed silently if refresh token is valid
- If refresh fails, user is redirected to `/login` with a "Session expired" message

---

## Epic 2 — User Profile Management

**Goal:** All user roles can view and update their profile information, photo, and account details.

---

### Story 2.1 — View My Profile (Real Data)
**As a** logged-in user, **I want** to see my actual profile data, **so that** I know what information is stored about me.

#### Tasks
**Frontend**
- [ ] Ensure `ProfileEditForm` pre-fills from `currentUser` pulled from AuthContext (already done; verify it works after Story 1.2 fix)
- [ ] Fix field mapping: backend returns `profileImageUrl`; frontend expects `profileImage` — normalise in `AuthProvider` or `authservice`
- [ ] Show `createdAt` (member since) on the profile page as a read-only field

**Backend**
- [ ] Verify `GET /api/v1/users/me` endpoint returns `profileImageUrl` correctly
- [ ] Add `createdAt` timestamp field to `User.java` entity (`@CreationTimestamp`)

**Acceptance Criteria**
- Profile page loads with real data from the backend
- Profile photo appears if one has been uploaded

---

### Story 2.2 — Edit Profile (All Roles)
**As a** logged-in user, **I want** to update my name, email, phone, and address, **so that** my information stays current.

**Background:** Core update functionality exists. This story cleans up remaining issues.

#### Tasks
**Frontend**
- [ ] Fix `UpdateProfileRequest` interface in `userservice.ts` — rename `profileImage` to `profileImageUrl` to match backend's `UpdateUserRequest.ProfileImageUrl`
- [ ] Add section tab navigation UI to `SellerEditProfile.tsx` (Profile / Verification tabs with visual indicators — currently switching only works via URL `?section=`)
- [ ] Validate phone number format (optional field, but if provided, must match a basic phone regex)
- [ ] Confirm email-change flow: if email is changed, force re-login (already implemented — verify it works end-to-end)

**Backend**
- [ ] Fix `UpdateUserRequest.java` — `ProfileImageUrl` has uppercase P, causing Jackson not to map `profileImageUrl` from JSON. Rename field to `profileImageUrl` and add `@JsonProperty("profileImageUrl")` if needed.

**Acceptance Criteria**
- Profile updates persist and are visible on next login
- Changing email forces user to log in again
- Field mapping between frontend and backend is correct (no silent failures)

---

### Story 2.3 — Profile Photo Upload
**As a** logged-in user, **I want** to upload a profile photo, **so that** I have a personalised avatar.

**Background:** Upload via Cloudinary works. This story validates the end-to-end save.

#### Tasks
**Frontend**
- [ ] Ensure uploaded Cloudinary URL is sent in profile update request as `profileImageUrl`
- [ ] Show a loading spinner on the avatar while uploading
- [ ] Support removing the photo (set `profileImageUrl` to `null`)
- [ ] Display optimized 40×40 thumbnail in header (already done via `cloudinaryService.getOptimizedUrl`)

**Backend**
- [ ] Accept `null` or empty string for `profileImageUrl` to allow photo removal

**Acceptance Criteria**
- Uploaded photo appears immediately in the header after save
- Photo persists across sessions (stored in DB, retrieved via `GET /api/v1/users/me`)

---

### Story 2.4 — Delete Account
**As a** buyer or seller, **I want** to delete my account when I no longer want to use the platform, **so that** my data is removed.

**Rules:**
- **Buyer** can delete only if they have **no ongoing orders** (status not `PENDING` or `SHIPPED`)
- **Seller** can delete only if they have **no ongoing deliveries** (no active products with pending orders)

#### Tasks
**Backend**
- [ ] Add `DELETE /api/v1/users/me` endpoint in `UserController.java`
- [ ] In `UserService.deleteAccount()`:
  - For Buyer: check OrderRepository for any order with status `PENDING` or `SHIPPED` belonging to this buyer — reject if found
  - For Seller: check if any of the seller's products have associated pending/shipped orders — reject if found
  - If safe, delete the user record (cascade to Buyer/Seller table)
- [ ] Return `409 Conflict` with a clear message if deletion is blocked
- [ ] Return `200 OK` on success

**Frontend**
- [ ] Add a "Danger Zone" section at the bottom of the Edit Profile page (all roles)
- [ ] Create a confirmation modal: "Are you sure? This action cannot be undone."
- [ ] Call `DELETE /api/v1/users/me` on confirmation
- [ ] On success: call `logout()`, redirect to `/`, show toast "Your account has been deleted"
- [ ] On `409` response: show specific message (e.g. "You have ongoing orders. Please wait for them to complete.")
- [ ] Add `deleteAccount()` method to `userservice.ts`

**Acceptance Criteria**
- Buyers with pending/shipped orders cannot delete their account
- Sellers with active delivery processes cannot delete their account
- After deletion, user is logged out and cannot log back in

---

## Epic 3 — Seller Verification Workflow

**Goal:** Sellers can submit business documents for verification, track status, and receive a verified badge when approved.

---

### Story 3.1 — Seller Submits Verification Request
**As a** seller, **I want** to submit my business registration documents, **so that** I can get a verified badge on my store.

**Background:** The UI exists in `SellerEditProfile.tsx`. The API endpoint `POST /api/v1/users/profile/verification` is called by `userservice.submitVerification()`. Need to verify the backend endpoint actually exists.

#### Tasks
**Backend**
- [ ] Add `verificationStatus` field (`NONE`, `PENDING`, `APPROVED`, `REJECTED`) to `Seller.java` entity
- [ ] Add `rejectionReason` (String) field to `Seller.java`
- [ ] Add `submittedAt` (LocalDateTime) to `Seller.java`
- [ ] Create `VerificationRequest.java` DTO (`businessName`, `registrationNumber`, `brCertificateUrl`, `nicImageUrl`)
- [ ] Add `POST /api/v1/users/profile/verification` endpoint in `UserController.java`
  - Only accessible by SELLER role
  - Set `verificationStatus = PENDING`, save documents, set `submittedAt = now()`
  - Reject if status is already `APPROVED` or `PENDING`

**Frontend**
- [ ] Add tab navigation UI (Profile / Verification) to `SellerEditProfile.tsx` — visible as two clickable tabs, not just URL-driven
- [ ] Verify `VerificationRequest` payload fields match the backend DTO exactly
- [ ] Pre-fill form fields from `currentUser.verification` data if a previous submission exists
- [ ] Lock form inputs when status is `PENDING` or `APPROVED` (already done — verify it works)
- [ ] Show `rejectionReason` from backend response when status is `REJECTED`

**Acceptance Criteria**
- Seller can submit verification exactly once (subsequent submissions only allowed after REJECTED)
- All required documents must be uploaded before submitting
- Verification status badge updates immediately on the UI after submission

---

### Story 3.2 — Verification Status Display
**As a** seller, **I want** to see the current status of my verification request at all times, **so that** I know what action to take.

#### Tasks
**Frontend**
- [ ] Ensure `currentUser.verification.status` is correctly populated from the backend on login/refresh
- [ ] `VerificationBadge` component shows correct badge for all 4 states (`NONE`, `PENDING`, `APPROVED`, `REJECTED`)
- [ ] Show `submittedAt` date in the pending banner
- [ ] Show rejection reason prominently when status is `REJECTED`
- [ ] Show verified checkmark on the seller's profile card in their dashboard

**Backend**
- [ ] Ensure `GET /api/v1/users/me` response includes the seller's verification fields (`verificationStatus`, `businessName`, `registrationNumber`, `nicImageUrl`, `brCertificateUrl`, `rejectionReason`, `submittedAt`)
- [ ] Create a `SellerProfileResponse` DTO that extends `UserProfileResponse` with these seller-specific fields

**Acceptance Criteria**
- Seller sees their real verification status immediately after login
- All 4 status states are visually distinct

---

### Story 3.3 — Seller Resubmits After Rejection
**As a** rejected seller, **I want** to update and resubmit my documents, **so that** I can try to get verified again.

#### Tasks
**Backend**
- [ ] When status is `REJECTED`, allow a new `POST /api/v1/users/profile/verification` request
- [ ] Reset status to `PENDING` on resubmission; clear `rejectionReason`; update `submittedAt`

**Frontend**
- [ ] Unlock form fields when status is `REJECTED`
- [ ] Show "Resubmit Verification" button (already in UI — verify it calls the correct endpoint)

**Acceptance Criteria**
- Seller with `REJECTED` status can edit and resubmit documents
- Status changes back to `PENDING` after resubmission

---

## Epic 4 — Admin User Management

**Goal:** Admins can view all users, manage their status, and review/approve/reject seller verification requests.

---

### Story 4.1 — Admin: View All Users (Real Data)
**As an** admin, **I want** to see a real list of all registered users, **so that** I can monitor the platform.

**Background:** `AdminDashboard` currently shows `mockBuyers` and `mockSellers`. Backend has `GET /api/v1/users` endpoint.

#### Tasks
**Frontend**
- [ ] Add `getAllUsers()` method to `userservice.ts` calling `GET /api/v1/users`
- [ ] Update `AdminDashboard.tsx` Users tab to call `getAllUsers()` on mount
- [ ] Display loading spinner while fetching
- [ ] Show user table with columns: Name, Email, Role, Status (enabled/disabled), Joined date
- [ ] Add search/filter by name or email
- [ ] Separate buyers and sellers in distinct sections or use a role filter

**Backend**
- [ ] Add `@PreAuthorize("hasRole('ADMIN')")` or role check in `UserController.getAllUsers()`
- [ ] Ensure response does not include `password` field

**Acceptance Criteria**
- Admin sees real user data from the database
- Passwords are never exposed in the response

---

### Story 4.2 — Admin: Disable / Re-enable User
**As an** admin, **I want** to disable or re-enable a user account, **so that** I can manage problematic accounts.

**Background:** `PUT /api/v1/users/{id}/disable` exists in the backend. Re-enable endpoint is missing.

#### Tasks
**Backend**
- [ ] Add `PUT /api/v1/users/{id}/enable` endpoint in `UserController.java`
- [ ] Add `enableUser(int id)` method in `UserService.java` setting `enabled = true`
- [ ] Ensure the `JwtFilter` / `UserDetailsService` checks `user.isEnabled()` — reject disabled users with `401`

**Frontend**
- [ ] Add "Disable" / "Enable" toggle button per user in the Admin Users table
- [ ] Add `disableUser(id)` and `enableUser(id)` methods to `userservice.ts`
- [ ] Show confirmation dialog before disabling: "Are you sure? This user will lose access immediately."
- [ ] Update UI state optimistically after action

**Acceptance Criteria**
- Disabling a user prevents them from making authenticated API calls
- Admin can re-enable a previously disabled user
- Disabled users who try to log in see "Account is disabled" error

---

### Story 4.3 — Admin: Seller Verification Review Page
**As an** admin, **I want** a dedicated page to review seller verification requests, **so that** I can approve or reject them efficiently.

#### Tasks
**Backend**
- [ ] Add `GET /api/v1/admin/verifications` endpoint — returns all sellers with `verificationStatus = PENDING`
- [ ] Create `VerificationReviewRequest.java` DTO: `{ decision: "APPROVED" | "REJECTED", rejectionReason?: string }`
- [ ] Add `PUT /api/v1/admin/verifications/{sellerId}` endpoint
  - If `APPROVED`: set `verificationStatus = APPROVED`, clear `rejectionReason`, set `reviewedAt`
  - If `REJECTED`: set `verificationStatus = REJECTED`, save `rejectionReason`, set `reviewedAt`
- [ ] Protect both endpoints with ADMIN role check

**Frontend**
- [ ] Create `src/components/AdminVerificationPage.tsx` — new dedicated page
- [ ] Add route `/admin/verifications` protected by `allowedroles={["ADMIN"]}`
- [ ] Add navigation link in `AdminDashboard` header to "Seller Verifications"
- [ ] Page shows a list of pending verifications with:
  - Seller name, email, business name, registration number
  - Uploaded document thumbnails/links (BR cert, NIC)
  - Submitted date
  - "Approve" (green) and "Reject" (red) buttons
- [ ] Rejection requires entering a reason (modal with textarea)
- [ ] Add `getPendingVerifications()`, `approveVerification(sellerId)`, `rejectVerification(sellerId, reason)` methods to a new `adminService.ts`
- [ ] Show empty state if no pending verifications
- [ ] Show loading state while fetching

**Acceptance Criteria**
- Admin can see all pending seller verification requests
- Admin can approve with one click
- Admin must provide a reason when rejecting
- The seller's status updates immediately in the backend when the admin acts
- Seller sees updated status on next app load/refresh

---

## 📅 Suggested Delivery Order (May 2026)

| Week | Stories | Priority |
|---|---|---|
| **Week 1** (May 6–12) | 1.1 (Bug fix), 1.2 (Remove mock), 2.2 (Profile mapping fix) | 🔴 Critical bugs |
| **Week 2** (May 13–19) | 1.3 (Change Password), 1.5 (JWT 30min + Refresh), 2.1 (View profile) | 🔴 Core features |
| **Week 3** (May 20–26) | 3.1 (Submit verification), 3.2 (Status display), 4.1 (Admin real data), 4.2 (Disable user) | 🟡 Key features |
| **Week 4** (May 27–31) | 1.4 (Forgot/Reset password), 2.4 (Delete account), 3.3 (Resubmit), 4.3 (Verification review page) | 🟢 Remaining features + Testing |

---

## 🧪 Testing Checkpoints (per Story)

Each story should pass:
- [ ] Happy path: feature works as expected
- [ ] Error path: invalid inputs show correct error messages
- [ ] Auth path: unauthenticated access is blocked
- [ ] Role path: wrong-role access is blocked
- [ ] Persistence: data survives page refresh
