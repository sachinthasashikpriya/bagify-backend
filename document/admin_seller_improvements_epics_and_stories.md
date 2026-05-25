# Bagify — Admin, Seller & Security Improvements: Epics & Stories

> **Scope:** May–June 2026 deadline | Frontend: React + TypeScript (Vite) | Backend: Spring Boot (Java) microservice  
> **Analysis date:** 2026-05-26

---

## 🗺️ Codebase Snapshot (Pre-Work)

### What Already Works ✅
| Area | Status |
|---|---|
| Admin dashboard: Orders tab (real API) | ✅ Done |
| Admin dashboard: Products tab (real API) | ✅ Done |
| Admin: per-item and global order status override | ✅ Done |
| Seller dashboard: Products tab (real API) | ✅ Done |
| Seller dashboard: Orders tab with per-item status update | ✅ Done |
| Seller: verified badge shown when `verificationStatus === APPROVED` | ✅ Done |
| Backend: `GET /api/v1/users` — returns all users (Admin only) | ✅ Done |
| Backend: `PUT /api/v1/users/{id}/disable` — disable user | ✅ Done |
| Backend: `PUT /api/v1/users/{id}/enable` — enable user | ✅ Done |
| Backend: `GET /api/v1/admin/verifications` — pending verifications | ✅ Done |
| Backend: `PUT /api/v1/admin/verifications/{sellerId}` — approve/reject | ✅ Done |
| Backend: `POST /api/v1/auth/refresh` — token refresh endpoint | ✅ Done |
| Backend: `POST /api/v1/auth/forgot-password` — password reset request | ✅ Done |
| Backend: `POST /api/v1/auth/reset-password` — password reset confirmation | ✅ Done |

### Known Gaps ❌
| Gap | Where |
|---|---|
| Admin Users tab uses `mockBuyers` and `mockSellers` static arrays | Frontend |
| No Enable / Disable user buttons in Admin Users tab | Frontend |
| No Seller Verification Review panel in Admin dashboard | Frontend |
| Admin platform stats (Total Users, Total Revenue) use mock data | Frontend |
| Seller revenue and items sold computed from static `30 - stock` assumption | Frontend |
| Seller rating is hardcoded as `4.5` — not derived from real reviews | Frontend |
| "View Analytics" button on Seller dashboard is a no-op placeholder | Frontend |
| Frontend has no silent token refresh interceptor on `401` responses | Frontend |
| No backend seller analytics endpoint for real revenue and sales count | Backend |

---

## Epic 1 — Admin User Management (Real Data)

**Goal:** The admin can view all real registered users, disable/re-enable accounts, and monitor platform activity from the dashboard.

---

### Story 1.1 — Admin: View All Users (Real Data)
**As an** admin, **I want** to see a real list of all registered users in my dashboard, **so that** I can monitor who is using the platform.

**Background:** The Users tab in `AdminDashboard.tsx` currently renders `mockBuyers` and `mockSellers` imported from `src/data/mockData.ts`. The backend `GET /api/v1/users` endpoint already exists and is protected by `@PreAuthorize("hasRole('ADMIN')")`.

#### Tasks
**Frontend**
- [ ] Add `getAllUsers()` method to `userservice.ts` calling `GET /api/v1/users` with `auth: true`
- [ ] Remove the `import { mockBuyers, mockSellers } from '../types'` import from `AdminDashboard.tsx`
- [ ] Add state `const [users, setUsers] = useState<UserProfileResponse[]>([])` and `isLoadingUsers` flag
- [ ] Trigger `userService.getAllUsers()` inside a `useEffect` that fires when `activeTab === 'users'`
- [ ] Show a loading spinner while fetching
- [ ] Display a single unified user table with columns: Name, Email, Role, Status (Enabled / Disabled), Joined Date
- [ ] Add a role-based filter dropdown (`ALL`, `BUYER`, `SELLER`) to the table header
- [ ] Add a search input to filter by name or email (client-side debounced filter)
- [ ] Show empty state with a message if no users are registered
- [ ] Update the "Total Users" stat card in the overview to use `users.length` from the fetched list

**Backend**
- [ ] Verify `GET /api/v1/users` response does **not** include the `password` field (add `@JsonIgnore` on the `password` field in `User.java` if not already present)
- [ ] Ensure the response includes `enabled`, `role`, and `createdAt` fields so the frontend table can display them

**Acceptance Criteria**
- Admin sees real users from the database in the Users tab
- The table is filterable by role and searchable by name or email
- No mock data arrays are used anywhere in the Admin dashboard user list
- `password` field is never present in the JSON response

---

### Story 1.2 — Admin: Disable / Re-enable User Account
**As an** admin, **I want** to disable or re-enable a user account from the dashboard, **so that** I can block problematic users without deleting them.

**Background:** Backend endpoints `PUT /api/v1/users/{id}/disable` and `PUT /api/v1/users/{id}/enable` exist. There is no corresponding UI to invoke them.

#### Tasks
**Frontend**
- [ ] Add `disableUser(id: number)` method to `userservice.ts` calling `PUT /api/v1/users/{id}/disable` with `auth: true`
- [ ] Add `enableUser(id: number)` method to `userservice.ts` calling `PUT /api/v1/users/{id}/enable` with `auth: true`
- [ ] Add an "Actions" column to the Admin Users table
- [ ] Render a conditional button per row:
  - If user is `enabled: true` → show a red "Disable" button
  - If user is `enabled: false` → show a green "Enable" button
- [ ] Clicking "Disable" opens a `ConfirmModal` with: *"Are you sure? This user will lose access immediately."*
- [ ] On confirm, call `disableUser(id)` and update the local `users` state optimistically (toggle `enabled`)
- [ ] Show success toast: *"User [name] has been disabled"* / *"User [name] has been enabled"*
- [ ] Show error toast if the API call fails
- [ ] Prevent admin from disabling their own account (compare `user.id !== currentUser.id`)

**Backend**
- [ ] Verify `JwtFilter` rejects tokens for disabled users with `401 Unauthorized` (checks `user.isEnabled()`)
- [ ] Add `@PreAuthorize("hasRole('ADMIN')")` guard to both enable and disable endpoints (already done — verify)

**Acceptance Criteria**
- Admin can disable any user except themselves
- Disabled user receives `401` on their next authenticated request
- Admin can re-enable a previously disabled user
- UI updates immediately after action without a full page reload

---

## Epic 2 — Admin Seller Verification Review

**Goal:** The admin has a dedicated panel to review pending seller verification requests, approve them, or reject them with a required written reason.

---

### Story 2.1 — Admin: View Pending Seller Verifications
**As an** admin, **I want** to see all pending seller verification requests in one place, **so that** I can review submitted business documents efficiently.

**Background:** `GET /api/v1/admin/verifications` returns all sellers with `verificationStatus = PENDING`. There is no UI panel in the admin dashboard to display this data.

#### Tasks
**Frontend**
- [ ] Add a "Verifications" tab button to the `AdminDashboard.tsx` tab navigation bar
- [ ] Add `activeTab` type to include `'verifications'`
- [ ] Create `adminService.ts` (new file at `src/services/adminService.ts`) with:
  - `getPendingVerifications()` calling `GET /api/v1/admin/verifications` with `auth: true`
  - `approveVerification(sellerId: number)` calling `PUT /api/v1/admin/verifications/{sellerId}` with body `{ decision: "APPROVED" }` and `auth: true`
  - `rejectVerification(sellerId: number, reason: string)` calling `PUT /api/v1/admin/verifications/{sellerId}` with body `{ decision: "REJECTED", rejectionReason: reason }` and `auth: true`
- [ ] Fetch verifications with a `useEffect` when `activeTab === 'verifications'`
- [ ] Show loading spinner while fetching
- [ ] Display each pending seller as a card containing:
  - Seller name and email
  - Business name and registration number
  - Submitted date (`submittedAt`)
  - Clickable thumbnail links to the BR Certificate and NIC images
- [ ] Show empty state with message: *"No pending verification requests."* when list is empty

**Acceptance Criteria**
- Admin sees all sellers currently awaiting verification in a dedicated tab
- Document thumbnails/links are rendered and clickable
- Empty state appears correctly when no verifications are pending

---

### Story 2.2 — Admin: Approve or Reject Seller Verification
**As an** admin, **I want** to approve or reject a seller's verification request, **so that** verified sellers can begin selling and unqualified submissions are returned with feedback.

**Background:** `PUT /api/v1/admin/verifications/{sellerId}` endpoint accepts `{ decision, rejectionReason }`. There is no UI to invoke it.

#### Tasks
**Frontend**
- [ ] Add "Approve" (green button) and "Reject" (red button) action buttons to each seller verification card
- [ ] Clicking "Approve":
  - Opens a `ConfirmModal`: *"Approve [seller name]'s verification? They will receive a verified badge."*
  - On confirm, calls `adminService.approveVerification(sellerId)`
  - On success: removes the card from the list and shows toast *"[seller name] has been approved."*
- [ ] Clicking "Reject":
  - Opens a **rejection reason modal** (a new inline modal or extended `ConfirmModal`) with a required `<textarea>` field for the rejection reason
  - Submit button is disabled until at least 10 characters are entered in the reason field
  - On confirm, calls `adminService.rejectVerification(sellerId, reason)`
  - On success: removes the card from the list and shows toast *"[seller name]'s verification has been rejected."*
- [ ] Show loading state on the clicked button while the request is in flight
- [ ] Show error toast if either API call fails

**Acceptance Criteria**
- Admin can approve a seller with a single confirmed click
- Admin cannot reject without providing a written reason
- The seller's card disappears from the list immediately after any decision
- The seller's `verificationStatus` is correctly updated in the backend

---

## Epic 3 — Seller Dashboard Analytics (Real Data)

**Goal:** The seller dashboard displays accurate, real-time performance metrics — items sold, total revenue, and ratings — calculated from actual completed orders and buyer reviews.

---

### Story 3.1 — Seller: Real Revenue and Items Sold Stats
**As a** seller, **I want** my dashboard stats to show my real revenue and items sold count, **so that** I can understand my true performance.

**Background:** `SellerDashboard.tsx` computes `totalRevenue` and `totalSold` using `Math.max(0, 30 - p.stock)` — a static formula assuming each product started with 30 units. This is inaccurate and does not reflect real sales data from completed orders.

#### Tasks
**Backend**
- [ ] Add `GET /api/v1/orders/seller/stats` endpoint in the `order-service` `OrderController.java`
  - Compute `totalRevenue` by summing `priceAtPurchase * quantity` for all `OrderItem` records belonging to this seller where `itemStatus = DELIVERED`
  - Compute `totalItemsSold` as the total count of delivered order items for this seller
  - Return a `SellerStatsResponse` DTO: `{ totalRevenue: double, totalItemsSold: int }`
- [ ] Protect the endpoint with SELLER role only
- [ ] Add the route `GET /api/v1/orders/seller/stats` to the API gateway routing config

**Frontend**
- [ ] Add `getSellerStats()` method to `orderService.ts` calling `GET /api/v1/orders/seller/stats` with `auth: true`
- [ ] Add state `const [sellerStats, setSellerStats] = useState<{ totalRevenue: number; totalItemsSold: number } | null>(null)`
- [ ] Call `orderService.getSellerStats()` on dashboard mount inside `useEffect`
- [ ] Replace the `totalRevenue` and `totalSold` client-side computed values with `sellerStats.totalRevenue` and `sellerStats.totalItemsSold`
- [ ] Show `—` or a skeleton loader in the stat cards while the stats are loading

**Acceptance Criteria**
- "Items Sold" and "Total Revenue" stat cards reflect real data from the database
- Only orders with `itemStatus = DELIVERED` are counted towards revenue
- Stats update correctly when the seller refreshes the page

---

### Story 3.2 — Seller: Real Rating from Reviews
**As a** seller, **I want** my displayed rating to be calculated from my actual product reviews, **so that** I can see how buyers truly rate my store.

**Background:** `SellerDashboard.tsx` shows a hardcoded `sellerRating = 4.5`. The `totalReviews` count is summed from `product.reviews.length` which uses the local product state (loaded from the backend), but the rating is not actually derived from those reviews.

#### Tasks
**Frontend**
- [ ] Remove the `const sellerRating = 4.5;` hardcoded constant from `SellerDashboard.tsx`
- [ ] Compute `sellerRating` dynamically from the seller's own products using real review data:
  ```ts
  const allReviews = sellerProducts.flatMap(p => p.reviews);
  const sellerRating = allReviews.length > 0
    ? allReviews.reduce((sum, r) => sum + r.rating, 0) / allReviews.length
    : 0;
  const totalReviews = allReviews.length;
  ```
- [ ] Show `"No reviews yet"` (instead of `"0.0 rating"`) when `totalReviews === 0`

**Backend**
- [ ] Verify the `GET /api/v1/products` response includes `reviews` array (or at minimum `averageRating` and `reviewCount`) per product — this powers the client-side aggregation
- [ ] Ensure `GET /api/v1/products` filters products by the authenticated seller's `sellerId` when called from the seller context (or add a dedicated `GET /api/v1/products/my` endpoint for sellers)

**Acceptance Criteria**
- The seller dashboard rating is the mathematical average of all individual review ratings across all of the seller's products
- The total review count is accurate
- When a seller has no reviews, the display reads "No reviews yet" rather than a numeric placeholder

---

### Story 3.3 — Seller: Product Edit Capability
**As a** seller, **I want** to edit the details of my existing products (price, stock, description, image), **so that** I can keep my listings up to date without deleting and re-creating them.

**Background:** The Seller Products table has a "View" button (using the `Edit` icon) that navigates to the public product detail page. There is no in-dashboard edit form, and no `PUT /api/v1/products/{id}` call is made.

#### Tasks
**Backend**
- [ ] Verify `PUT /api/v1/products/{id}` endpoint exists in the `product-service` `ProductController.java`
  - Only the owning seller can update their product (check `product.sellerId === authenticatedUserId`)
  - Return `403 Forbidden` if the user is not the owner
  - Return the updated `ProductResponse` on success

**Frontend**
- [ ] Replace the `Edit` icon button in the Seller Products table with an "Edit" action that opens an inline edit form (or a modal) pre-filled with the product's current data
- [ ] The edit form should include: Name, Description, Price, Stock, Category, Image URL
- [ ] Add `updateProduct(productId, updates)` to `productService.ts` calling `PUT /api/v1/products/{id}` with `auth: true`
- [ ] On successful save: update the product in the local `products` state and show toast *"Product updated successfully"*
- [ ] On error: show toast with the error message
- [ ] Add a "Cancel" button to dismiss the edit form without saving

**Acceptance Criteria**
- Seller can edit all fields of their own products without leaving the dashboard
- Only the owning seller can edit a product (backend enforces this)
- Changes are reflected immediately in the product table after saving

---

## Epic 4 — Session Security (Token Refresh)

**Goal:** Authenticated users are never abruptly logged out mid-session due to an expired access token. The frontend silently renews the session using the stored refresh token.

---

### Story 4.1 — Silent Token Refresh Interceptor
**As a** logged-in user, **I want** my session to be automatically renewed when my access token expires, **so that** I am not unexpectedly logged out while actively using the application.

**Background:** The backend issues a `refreshToken` alongside the `accessToken` on login (`POST /api/v1/auth/refresh` endpoint exists). The frontend stores the `refreshToken` in `localStorage` (stored in `AuthProvider.tsx`), but `httpClient.ts` does not intercept `401` responses to attempt a silent token refresh.

#### Tasks
**Frontend**
- [ ] Create `src/api/tokenRefresher.ts` utility module with a function `attemptTokenRefresh()`:
  - Read `refreshToken` from `localStorage`
  - If no refresh token exists, return `false`
  - Call `POST /api/v1/auth/refresh` with `{ refreshToken }` directly via `fetch` (not `httpClient` to avoid circular dependency)
  - On success: store the new `token` and `refreshToken` in `localStorage` and return `true`
  - On failure: call `logout()` from `AuthContext`, show toast *"Your session has expired. Please log in again."*, and return `false`
- [ ] Update `httpClient.ts` response handling to:
  - On `401 Unauthorized` response, call `attemptTokenRefresh()`
  - If refresh succeeds, retry the original request once with the new access token
  - If refresh fails, abort the retry and allow the logout flow to complete
- [ ] Update `AuthProvider.tsx` `login()` to store both `token` and `refreshToken` in `localStorage` (verify this is already done — if not, add it)
- [ ] Update `AuthProvider.tsx` `logout()` to also clear `refreshToken` from `localStorage`

**Acceptance Criteria**
- A user with a valid refresh token is never shown a `401` error mid-session
- The failed request is transparently retried with the refreshed token
- If the refresh token is also expired, the user is logged out with a friendly message
- On logout, both `token` and `refreshToken` are cleared from `localStorage`

---

## 📅 Suggested Delivery Order

| Priority | Epic | Stories | Effort |
|---|---|---|---|
| 🔴 **High** | Admin User Management | 1.1 (Real user list), 1.2 (Disable/Enable) | Medium |
| 🔴 **High** | Admin Seller Verification | 2.1 (View pending), 2.2 (Approve/Reject) | Medium |
| 🟡 **Medium** | Seller Analytics | 3.1 (Real revenue/sales), 3.2 (Real rating) | Medium |
| 🟡 **Medium** | Seller Product Editing | 3.3 (Edit product form) | Small |
| 🟢 **Low** | Session Security | 4.1 (Token refresh interceptor) | Small |

---

## 🧪 Testing Checkpoints (per Story)

Each story should pass:
- [ ] **Happy path:** feature works as expected end-to-end
- [ ] **Error path:** API failures show correct error toasts or fallback states
- [ ] **Auth path:** unauthenticated access is blocked with `401 Unauthorized`
- [ ] **Role path:** non-ADMIN roles are blocked (`403 Forbidden`) on admin endpoints
- [ ] **Persistence:** data survives page refresh (fetched from backend, not hardcoded)
- [ ] **Empty state:** appropriate UI shown when no data exists (no pending verifications, no users, etc.)
