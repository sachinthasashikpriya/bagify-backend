# Bagify — PayHere Payment Gateway: Epics & Stories

> **Scope:** May–June 2026 | Frontend: React + TypeScript (Vite) | Backend: Spring Boot (Java) microservice
> **Analysis date:** 2026-06-13 | **Environment:** PayHere Sandbox

---

## 🗺️ Codebase Snapshot (Pre-Work)

### What Already Works ✅
| Area | Status |
|---|---|
| Order entity with `paymentStatus` and `paymentId` fields | ✅ Done (backend) |
| `PayHereSignatureGenerator` utility (checkout hash + webhook hash) | ✅ Done (backend) |
| `GET /api/v1/orders/{id}/payment-params` endpoint | ✅ Done (backend) |
| `POST /api/v1/orders/payment/notify` webhook endpoint (public) | ✅ Done (backend) |
| `payhere.js` SDK loaded globally via `<script>` in `index.html` | ✅ Done (frontend) |
| `orderService.getPaymentParams()` frontend service call | ✅ Done (frontend) |
| `handlePay()` in `OrderConfirmationPage.tsx` initiating the popup | ✅ Done (frontend) |
| `UNPAID` card on Order Confirmation page with "Pay Now" button | ✅ Done (frontend) |
| `PAID` card on Order Confirmation page with Reference ID | ✅ Done (frontend) |
| `payhere.onCompleted` — optimistic UI update + backend polling | ✅ Done (frontend) |
| Seller stats update via `UserClient` after successful payment | ✅ Done (backend) |
| Order status auto-transition `PENDING → PROCESSING` on payment | ✅ Done (backend) |
| Duplicate webhook guard (idempotency via `isAlreadyPaid` check) | ✅ Done (backend) |

### Known Gaps ❌
| Gap | Where |
|---|---|
| No end-to-end test covering the full payment flow | Testing |
| `notify_url` relies on an ngrok tunnel — not a stable production URL | Backend / DevOps |
| PayHere SDK load failure is only caught at click-time; no page-load detection | Frontend |
| No UI indicator for `paymentStatus = FAILED` (only UNPAID and PAID handled) | Frontend |
| Seller stats update has a silent catch — failure is only logged to `stderr` | Backend |

---

## Epic 1 — Secure Payment Parameter Generation

**Goal:** The backend generates a cryptographically signed set of payment parameters for each order so the buyer can initiate a PayHere checkout without the merchant secret ever reaching the browser.

---

### Story 1.1 — Fetch PayHere Payment Parameters
**As a** buyer, **I want** the system to provide signed payment parameters for my order, **so that** I can safely open the PayHere checkout popup without the merchant secret being exposed to the browser.

**Background:** When the buyer clicks "Pay Now with PayHere" on the Order Confirmation page, the frontend calls `GET /api/v1/orders/{id}/payment-params`. The backend generates an MD5 hash using the merchant secret and returns all parameters needed to call `payhere.startPayment()`. The merchant secret stays on the server at all times.

#### Tasks
**Backend**
- [x] Create `GET /api/v1/orders/{id}/payment-params` endpoint in `OrderController`
- [x] Restrict endpoint to `BUYER` role only using `@PreAuthorize("hasRole('BUYER')")`
- [x] Extract `buyerId` from the JWT claims inside `authentication.getDetails()`
- [x] Return `401 Unauthorized` if `buyerId` is null (missing JWT details)
- [x] In `OrderService.getPaymentParams()` — look up the order by ID; throw `404` if not found
- [x] Validate that `order.getBuyerId()` matches the authenticated `buyerId`; throw `403 Forbidden` if not
- [x] Reject orders with status `CANCELLED` with `400 Bad Request`
- [x] Format `totalAmount` to exactly 2 decimal places using `String.format(Locale.US, "%.2f", amount)`
- [x] Generate checkout hash: `MD5(merchantId + orderId + amount + currency + MD5(merchantSecret))` — uppercase hex
- [x] Build and return `PayHereParamsResponse` with fields: `merchantId`, `orderId`, `amount`, `currency`, `hash`, `sandbox`

**Frontend**
- [x] Add `orderService.getPaymentParams(orderId)` in `src/services/orderService.ts` calling `GET /api/v1/orders/{orderId}/payment-params`
- [x] In `OrderConfirmationPage.tsx`, wire `handlePay()` to call `orderService.getPaymentParams()` on button click
- [x] Show a spinner on the "Pay Now" button while the request is in flight (`isPaying` state)
- [x] If the response is not OK, show an error toast with the error message and stop the flow
- [x] Check that `window.payhere` exists (SDK loaded); show a toast error if not yet available

**Acceptance Criteria**
- [x] A BUYER with a valid JWT receives `200 OK` with all six payment parameters
- [x] The returned `hash` is a valid uppercase MD5 hex string matching PayHere's expected formula
- [x] A BUYER requesting params for another buyer's order receives `403 Forbidden`
- [x] Requesting params for a `CANCELLED` order returns `400 Bad Request`
- [x] A SELLER or ADMIN JWT receives `403 Forbidden`
- [x] An unauthenticated request returns `401 Unauthorized`
- [x] The `amount` field uses a dot decimal separator and exactly 2 decimal digits (e.g. `"1500.00"`)

---

### Story 1.2 — Initiate PayHere Checkout Popup
**As a** buyer, **I want** to see the PayHere payment popup pre-filled with my order details, **so that** I can complete the payment without manually entering the amount.

**Background:** Once the frontend receives the payment parameters from Story 1.1, it builds a complete `payment` object (including buyer info from the auth context and URLs) and calls `payhere.startPayment(payment)` on the globally loaded SDK instance.

#### Tasks
**Frontend**
- [x] Build the `payment` payload object using params from the backend response and buyer profile from `useAuth()`
- [x] Set `sandbox: payhereParams.sandbox` (ensures sandbox mode matches backend config)
- [x] Set `merchant_id: payhereParams.merchantId`
- [x] Set `return_url` to `${window.location.origin}/orders/${orderId}/confirmation`
- [x] Set `cancel_url` to `window.location.href` (current page)
- [x] Set `notify_url` to `env.PAYHERE_NOTIFY_URL` (from `VITE_PAYHERE_NOTIFY_URL` env variable)
- [x] Set `order_id`, `items`, `amount`, `currency`, `hash` from backend response
- [x] Set `first_name`, `email`, `phone` from `currentUser`; set `address` from order shipping address
- [x] Set `city` from `currentUser.city` and `country` to `"Sri Lanka"`
- [x] Call `payhere.startPayment(payment)` to open the PayHere popup

**Acceptance Criteria**
- [x] Clicking "Pay Now with PayHere" opens the PayHere Sandbox checkout popup
- [x] The popup displays the correct order amount in LKR
- [x] The popup pre-fills buyer's name, email, and address
- [x] The `notify_url` is a publicly accessible URL (not `localhost`)

---

## Epic 2 — Payment Webhook & Status Management

**Goal:** PayHere's asynchronous server-to-server notification is securely received, signature-verified, and used to update the order's payment status and trigger downstream actions (order status change, seller stats update).

---

### Story 2.1 — Receive and Verify PayHere Webhook
**As a** system, **I want** to receive PayHere's server-to-server payment notification and verify its authenticity, **so that** only legitimate payment results are accepted.

**Background:** After a payment is processed, PayHere sends a `POST` request to `notify_url` with the payment outcome as `application/x-www-form-urlencoded` form data. This endpoint must be **publicly accessible** (no JWT) because PayHere's server is the caller. Security is instead enforced by verifying the MD5 `md5sig` field.

#### Tasks
**Backend**
- [x] Create `POST /api/v1/orders/payment/notify` endpoint in `OrderController` — no `@PreAuthorize` (public)
- [x] Set `consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE` to accept form-encoded body
- [x] Delegate all processing to `OrderService.processPaymentNotification(params)`
- [x] Validate that all required fields are present: `merchant_id`, `order_id`, `payhere_amount`, `payhere_currency`, `status_code`, `md5sig`; return `400` if any are missing
- [x] Compute verification hash: `MD5(merchantId + orderId + payhereAmount + payhereCurrency + statusCode + MD5(merchantSecret))` — uppercase hex
- [x] Compare the computed hash with `md5sig` using case-insensitive comparison; reject with `400` if they do not match
- [x] Parse `order_id` to `Long`; return `400 Bad Request` if not a valid number
- [x] Fetch the order from `orderRepository`; throw `404` if not found
- [x] Return `200 OK` with body `"Notification Processed Successfully"` on every valid call

**Acceptance Criteria**
- [x] A valid webhook with the correct `md5sig` returns `200 OK`
- [x] A webhook with a tampered or incorrect `md5sig` returns `400 Bad Request`
- [x] A webhook with a missing required field returns `400 Bad Request`
- [x] A webhook with a non-numeric `order_id` returns `400 Bad Request`
- [x] A webhook for a non-existent order returns `404 Not Found`
- [x] The endpoint is accessible without a Bearer token (no `401` for unauthenticated calls)

---

### Story 2.2 — Update Order Payment Status on Webhook
**As a** system, **I want** to update the order's payment and fulfilment status according to the webhook result, **so that** buyers and sellers see an accurate order state after payment.

**Background:** After signature verification in Story 2.1, the service processes the `status_code` field to determine the outcome. Each outcome maps to specific changes on the `Order` entity.

#### Tasks
**Backend**
- [x] On `status_code = "2"` (success): set `order.paymentStatus = "PAID"` and `order.paymentId = payment_id`
- [x] On `status_code = "2"` and order is currently `PENDING`: set `order.status = PROCESSING`
- [x] On `status_code = "2"`: iterate all order items and set any item with status `PENDING` → `PROCESSING`
- [x] On `status_code = "0"` (pending / authorised): set `order.paymentStatus = "PENDING"` only
- [x] On any other (negative) status code (failed / cancelled / chargebacked): set `order.paymentStatus = "FAILED"` only
- [x] Persist the updated order via `orderRepository.save(order)`

**Acceptance Criteria**
- [x] A `status_code = "2"` webhook sets `paymentStatus = PAID`, `status = PROCESSING`, and all pending items to `PROCESSING`
- [x] A `status_code = "0"` webhook sets `paymentStatus = PENDING` with no other changes
- [x] A negative `status_code` webhook sets `paymentStatus = FAILED` with no other changes
- [x] The `paymentId` field on the order is populated from the `payment_id` webhook parameter after a successful payment

---

### Story 2.3 — Update Seller Stats After Successful Payment
**As a** seller, **I want** my revenue and items-sold statistics to be updated when a buyer pays for an order containing my products, **so that** my dashboard reflects accurate earnings.

**Background:** After a successful payment, the webhook handler calls `UserClient.updateSellerStats()` for each seller whose items are in the order. This is a cross-microservice call from the order service to the user service.

#### Tasks
**Backend**
- [x] Add idempotency guard: only call `UserClient` if the order was not already `PAID` before this webhook (use `isAlreadyPaid` local flag checked before setting `paymentStatus`)
- [x] Iterate all `order.getItems()` and for each item, parse `item.getSellerId()` to `Integer`
- [x] Calculate per-item revenue as `item.getPriceAtPurchase() * item.getQuantity()`
- [x] Call `userClient.updateSellerStats(sellerId, itemRevenue, itemQuantity)` for each item
- [x] Wrap the `UserClient` call in a try-catch; log to `stderr` on failure without failing the overall webhook

**Acceptance Criteria**
- [x] After a successful payment, each seller's `totalRevenue` and `totalItemsSold` are incremented in the user microservice
- [x] A second identical webhook (duplicate) does NOT re-increment seller stats (idempotency)
- [x] A failure to reach the user microservice does not cause the webhook endpoint to return an error

---

## Epic 3 — Order Confirmation Page — Payment UI

**Goal:** The Order Confirmation page reflects the buyer's current payment state accurately and provides a clear, actionable payment experience.

---

### Story 3.1 — Display Payment Status on Order Confirmation Page
**As a** buyer, **I want** to see whether my order has been paid or is awaiting payment when I open the Order Confirmation page, **so that** I know what action to take.

**Background:** `OrderConfirmationPage.tsx` fetches the order from the backend on mount. The `paymentStatus` field on the returned `OrderResponse` determines which card is shown: an "UNPAID" action card or a "PAID" confirmation card.

#### Tasks
**Frontend**
- [x] On component mount, call `orderService.getOrder(orderId)` and set the result in state
- [x] Show a full-page spinner (`Loader2`) while the order is loading
- [x] Show an error card with "Order Not Found" and a "Return Home" button if the fetch fails or returns `403`
- [x] When `order.paymentStatus === 'UNPAID'`, render the "Payment Required" card with a purple border and red "UNPAID" badge
- [x] When `order.paymentStatus === 'PAID'`, render the "Payment Received" card with a green border and green "PAID" badge
- [x] Display the PayHere Reference ID (`order.paymentId`) in the PAID card

**Acceptance Criteria**
- [x] Page shows loading spinner while the order is being fetched
- [x] A buyer viewing their own order sees the correct payment card (UNPAID or PAID)
- [x] A buyer attempting to view another buyer's order sees a "You do not have permission" error message
- [x] An invalid order ID shows the "Order Not Found" error card

---

### Story 3.2 — Handle PayHere Payment Lifecycle Events
**As a** buyer, **I want** to receive immediate feedback after each payment action (success, dismiss, error), **so that** I always know the outcome of my payment attempt.

**Background:** The PayHere SDK fires three callback events on the `window.payhere` object: `onCompleted`, `onDismissed`, and `onError`. Each must be handled to give the buyer a clear and responsive experience.

#### Tasks
**Frontend**
- [x] Register `payhere.onCompleted` callback: show a success toast ("Payment successful! Thank you.")
- [x] On `onCompleted`: apply an optimistic UI update — set local `order.paymentStatus = "PAID"` and `order.status = "PROCESSING"` without waiting for the next poll
- [x] On `onCompleted`: call `refreshProducts()` to refresh the global product stock cache (stock was already deducted at order creation)
- [x] On `onCompleted`: start a polling loop — call `fetchOrderDetails()` every 1 500 ms for a maximum of 6 attempts to sync the real webhook-updated data from the backend
- [x] Register `payhere.onDismissed` callback: show a warning toast ("Payment dismissed.")
- [x] Register `payhere.onError` callback: show an error toast with the error string from the SDK

**Acceptance Criteria**
- [x] After a successful sandbox payment, a success toast appears and the page switches to the PAID card immediately (optimistic)
- [x] The PayHere Reference ID appears within a few seconds of payment (fetched from backend via polling)
- [x] After dismissing the popup, a warning toast appears and the "Pay Now" button becomes available again
- [x] After a payment error, an error toast appears with the SDK error message
- [x] After refreshing the page post-payment, the PAID state is still shown (data persisted via webhook)

---

### Story 3.3 — Handle PayHere SDK Not Loaded (Edge Case)
**As a** buyer, **I want** to see a clear error message if the PayHere SDK has not loaded yet when I try to pay, **so that** I can retry instead of getting a silent failure.

**Background:** `payhere.js` is loaded via a `<script>` tag in `index.html`. On slow networks or if the script is blocked, `window.payhere` may be undefined when the buyer clicks "Pay Now". The `handlePay()` function checks for this before proceeding.

#### Tasks
**Frontend**
- [x] Before calling `payhere.startPayment()`, check if `window.payhere` is truthy
- [x] If `window.payhere` is `undefined`, show a toast error: "PayHere SDK is not loaded yet. Please wait a few seconds and try again."
- [x] Set `isPaying = false` to re-enable the button after the check fails

**Acceptance Criteria**
- [x] If the SDK is not available when the button is clicked, a descriptive toast error is shown
- [x] The "Pay Now" button is re-enabled after the SDK check fails (buyer can retry)
- [x] No JavaScript runtime error is thrown when `window.payhere` is undefined

---

## 📅 Suggested Delivery Order

| Priority | Stories | Notes |
|---|---|---|
| 🔴 Core — security | 1.1 (Payment params generation), 2.1 (Webhook receipt + signature) | Must be correct before any QA |
| 🔴 Core — state | 2.2 (Order status update), 2.3 (Seller stats) | Triggers downstream seller dashboard |
| 🟡 UX | 1.2 (Initiate popup), 3.1 (Display status), 3.2 (Lifecycle events) | End-to-end buyer experience |
| 🟢 Polish | 3.3 (SDK edge case) | Defensive UX improvement |

---

## 🧪 QA Testing Checkpoints

### Environment Prerequisites
| Item | Requirement |
|---|---|
| Running services | `eureka-server`, `api-gateway` (8080), `user` (8081), `product` (8082), `order` (8083) |
| Database | PostgreSQL — `bagify_orders` schema with `orders` and `order_items` tables |
| Frontend | Vite dev server; `index.html` loads `https://www.payhere.lk/lib/payhere.js` |
| `VITE_PAYHERE_NOTIFY_URL` | Must be a **publicly reachable** URL (e.g. ngrok tunnel pointing to `localhost:8080/api/v1/orders/payment/notify`) |
| PayHere Sandbox credentials | Merchant ID `1235988`, sandbox test card: `4916217501611292`, Expiry: any future date, CVV: `100` |

### Per-Story Testing Checklist

Each story should pass:
- [ ] **Happy path:** feature works as expected end-to-end
- [ ] **Error path:** invalid inputs or failures show the correct error messages
- [ ] **Auth path:** unauthenticated access is blocked (`401 Unauthorized`)
- [ ] **Role path:** non-BUYER roles are blocked (`403 Forbidden`) where applicable
- [ ] **Security path:** tampered signatures or missing fields are rejected (`400 Bad Request`)
- [ ] **Idempotency:** duplicate webhook calls do not double-update data
- [ ] **Persistence:** payment status survives page refresh (fetched from backend)
- [ ] **Edge case:** SDK not loaded produces a graceful user-visible error
