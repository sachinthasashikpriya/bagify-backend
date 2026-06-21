# Bagify — Buyer Features: Epics & Stories

> **Scope:** May–June 2026 | Frontend: React + TypeScript (Vite) | Backend: Spring Boot (Java) microservice
> **Analysis date:** 2026-05-19

---

## 🗺️ Codebase Snapshot (Pre-Work)

### What Already Works ✅
| Area | Status |
|---|---|
| Product listing on HomePage (search + category filter) | ✅ UI done (mock data) |
| Product detail page (image, price, stock, seller info) | ✅ UI done (mock data) |
| Add to Cart from HomePage and ProductDetailPage | ✅ UI done (localStorage) |
| Cart page (view, update quantity, remove, clear) | ✅ UI done (localStorage) |
| Order summary panel (subtotal, shipping, tax) | ✅ UI done |
| Wishlist tab in BuyerDashboard | ✅ UI done (pulls from mock products) |
| My Reviews tab in BuyerDashboard | ✅ UI done (empty state only) |
| Submit review form on ProductDetailPage | ✅ UI done (in-memory only) |
| Average rating display on ProductDetailPage | ✅ UI done |
| BuyerDashboard stats (total orders, spent, cart, wishlist) | ✅ UI done (mock data) |
| Buyer role guard (BUYER-only cart, checkout, reviews) | ✅ Done |
| Edit Profile / Change Password (covered in user_accounts_epics_and_stories.md) | ✅ Done |

### Known Gaps ❌
| Gap | Where |
|---|---|
| Product data comes from `mockProducts` — no real API call | Frontend |
| Cart stored in localStorage only — not persisted to backend | Both |
| Checkout simulates a 2-second delay — no real order created | Both |
| Orders in BuyerDashboard use `mockOrders` hardcoded array | Frontend |
| Wishlist uses `products.slice(0, 3)` — no real wishlist entity | Both |
| Reviews submitted via `addReview()` in memory — not saved to DB | Both |
| No duplicate review prevention (buyer can review same product multiple times) | Both |
| No "review only after purchase" enforcement | Both |
| No order cancellation feature | Both |
| No order status tracking UI (PENDING / SHIPPED / DELIVERED) | Both |
| Cart does not validate stock availability at checkout | Both |
| No backend Cart or Wishlist microservice/entity | Backend |

---

## Epic 1 — Product Discovery & Browsing

**Goal:** Buyers can discover, search, filter, and view detailed information about products from real backend data.

---

### Story 1.1 — Browse Products (Real API)
**As a** buyer, **I want** to see real products listed on the home page, **so that** I can discover bags available for purchase.

**Background:** `useProducts()` initialises from `mockProducts`. The products must come from a real backend product service.

#### Tasks
**Frontend**
- [ ] Create `src/services/productService.ts` with `getProducts()` calling `GET /api/v1/products`
- [ ] Update `ProductProvider.tsx` to call `productService.getProducts()` on mount instead of using `mockProducts`
- [ ] Show a loading skeleton grid while products are fetching
- [ ] Show an error state with a "Retry" button if the request fails
- [ ] Retain client-side search and category filtering after data loads

**Backend**
- [ ] Ensure `GET /api/v1/products` endpoint exists and returns all active/published products
- [ ] Response must include: `id`, `name`, `description`, `price`, `stock`, `category`, `image` (URL), `sellerId`, `sellerName`, `sellerRating`, `averageRating`, `reviews`
- [ ] Allow unauthenticated access (`permitAll()` in `SecurityConfig`)

**Acceptance Criteria**
- Home page loads real products from the database
- Search and category filter work on the fetched data
- Loading and error states are handled gracefully

---

### Story 1.2 — Filter Products by Category
**As a** buyer, **I want** to filter products by category, **so that** I can quickly find the type of bag I'm looking for.

#### Tasks
**Frontend**
- [ ] Populate category buttons dynamically from the fetched products (`getCategories()`)
- [ ] Add an "All" default category that shows every product
- [ ] Highlight the active category button
- [ ] Optionally support URL query param `?category=` for shareable filtered links

**Backend**
- [ ] Add `GET /api/v1/products?category={category}` optional query parameter support
- [ ] Return only products matching the category when the param is provided

**Acceptance Criteria**
- Clicking a category shows only matching products
- Selecting "All" restores the full product list
- Categories update dynamically based on available products

---

### Story 1.3 — Search Products
**As a** buyer, **I want** to search for products by name or description, **so that** I can quickly find a specific bag.

#### Tasks
**Frontend**
- [ ] Retain search input in `HomePage.tsx` (already implemented client-side)
- [ ] Add debounce (300ms) to avoid filtering on every keystroke
- [ ] Show "No results found" with a "Clear search" button when empty

**Backend**
- [ ] Add `GET /api/v1/products?search={query}` optional query parameter
- [ ] Perform a case-insensitive `LIKE` search on `name` and `description` fields

**Acceptance Criteria**
- Typing in the search bar filters products with a short delay
- Empty state message appears when no products match
- Clearing the search restores all products

---

### Story 1.4 — View Product Detail
**As a** buyer, **I want** to click a product and see its full details, **so that** I can make an informed purchase decision.

**Background:** `ProductDetailPage.tsx` uses `products.find()` from local state. After Story 1.1, this should work. This story adds a dedicated API fetch per product.

#### Tasks
**Frontend**
- [ ] Add `productService.getProductById(productId)` calling `GET /api/v1/products/{id}`
- [ ] Fetch the product by ID in `ProductDetailPage` if not found in local state (handles direct URL access)
- [ ] Show skeleton loader while fetching
- [ ] Show 404 state if product not found

**Backend**
- [ ] Ensure `GET /api/v1/products/{id}` endpoint exists
- [ ] Return `404 Not Found` if product does not exist or is unlisted

**Acceptance Criteria**
- Navigating directly to `/product/{id}` loads the correct product
- Page shows loading state while fetching
- Invalid product ID shows a "Product not found" message

---

## Epic 2 — Shopping Cart Management

**Goal:** Buyers can manage a persistent cart that survives page refresh and is synced with the backend.

---

### Story 2.1 — Add Item to Cart
**As a** buyer, **I want** to add a product to my cart, **so that** I can purchase it later.

**Background:** `addToCart()` in `CartProvider.tsx` uses localStorage. Must be synced to backend.

#### Tasks
**Frontend**
- [ ] Create `src/services/cartService.ts` with `addToCart(productId, quantity)` calling `POST /api/v1/cart/items`
- [ ] Update `CartProvider.tsx` `addCart()` to call `cartService.addToCart()` and update local state on success
- [ ] Show loading indicator on the "Add to Cart" button while request is pending
- [ ] Prevent adding if product is out of stock (disable button + show "Out of Stock")
- [ ] Prevent non-BUYER roles from adding to cart (already enforced in UI — verify backend also enforces)

**Backend**
- [ ] Create `Cart` entity linked to `Buyer` (one-to-one)
- [ ] Create `CartItem` entity with `productId`, `quantity`, `addedAt`
- [ ] Add `POST /api/v1/cart/items` — body: `{ productId, quantity }` — adds item or increases quantity if already in cart
- [ ] Validate stock availability; return `409 Conflict` if requested quantity exceeds stock
- [ ] Protect endpoint with BUYER role only

**Acceptance Criteria**
- Adding a product to cart persists after page refresh
- Adding an already-carted product increases its quantity
- Out-of-stock products cannot be added
- Non-buyers receive `403 Forbidden`

---

### Story 2.2 — Update Cart Item Quantity
**As a** buyer, **I want** to increase or decrease the quantity of an item in my cart, **so that** I can buy the right amount.

#### Tasks
**Frontend**
- [ ] Add `cartService.updateQuantity(productId, quantity)` calling `PUT /api/v1/cart/items/{productId}`
- [ ] If quantity reaches 0, trigger `removeFromCart()` instead
- [ ] Disable +/- buttons while update request is in flight

**Backend**
- [ ] Add `PUT /api/v1/cart/items/{productId}` — body: `{ quantity }` — updates quantity
- [ ] Validate new quantity does not exceed available stock
- [ ] If quantity ≤ 0, remove the item from cart

**Acceptance Criteria**
- Quantity changes are saved to the backend immediately
- Cannot set quantity higher than available stock
- Setting quantity to 0 removes the item

---

### Story 2.3 — Remove Item from Cart
**As a** buyer, **I want** to remove an item from my cart, **so that** I can discard products I no longer want.

#### Tasks
**Frontend**
- [ ] Add `cartService.removeFromCart(productId)` calling `DELETE /api/v1/cart/items/{productId}`
- [ ] Show confirmation dialog before removing: "Remove [product name] from cart?"
- [ ] Show success toast on removal

**Backend**
- [ ] Add `DELETE /api/v1/cart/items/{productId}` — removes that item from the buyer's cart

**Acceptance Criteria**
- Removed item disappears from cart immediately
- Removal persists after page refresh

---

### Story 2.4 — View Cart & Order Summary
**As a** buyer, **I want** to view all items in my cart with an accurate price breakdown, **so that** I know what I'm about to pay.

**Background:** Cart page already shows subtotal, shipping, and tax. Needs real data.

#### Tasks
**Frontend**
- [ ] Add `cartService.getCart()` calling `GET /api/v1/cart`
- [ ] Load cart from backend on `CartProvider` mount (when user is BUYER)
- [ ] Display subtotal, shipping (free if ≥ $100), and 8% tax
- [ ] Show "Cart is empty" state with a CTA to browse products

**Backend**
- [ ] Add `GET /api/v1/cart` — returns all cart items for the authenticated buyer
- [ ] Include `productName`, `productImage`, `price`, `stock`, `quantity` per item

**Acceptance Criteria**
- Cart shows real items from the backend
- Price breakdown is accurate
- Cart state persists across sessions

---

### Story 2.5 — Clear Entire Cart
**As a** buyer, **I want** to clear all items from my cart at once, **so that** I can start over.

#### Tasks
**Frontend**
- [ ] Add `cartService.clearCart()` calling `DELETE /api/v1/cart`
- [ ] Show confirmation dialog: "Are you sure you want to clear your entire cart?"

**Backend**
- [ ] Add `DELETE /api/v1/cart` — removes all items from the buyer's cart

**Acceptance Criteria**
- All cart items are removed after confirmation
- Cart shows empty state immediately after clearing

---

## Epic 3 — Checkout & Order Placement

**Goal:** Buyers can complete a purchase, creating a real order in the system.

---

### Story 3.1 — Proceed to Checkout
**As a** buyer, **I want** to place an order from my cart, **so that** I can purchase the products I've selected.

**Background:** Current `handleCheckout()` in `CartPage.tsx` simulates a 2-second delay and calls `clearCart()` — no real order is created.

#### Tasks
**Frontend**
- [ ] Create `src/services/orderService.ts` with `placeOrder(shippingAddress)` calling `POST /api/v1/orders`
- [ ] Replace simulated checkout with a real API call in `CartPage.tsx`
- [ ] Collect / confirm shipping address before placing order (use profile address or prompt for one)
- [ ] Show loading state on "Proceed to Checkout" button during the request
- [ ] On success: clear cart, navigate to order confirmation page `/orders/{orderId}`
- [ ] On failure: show specific error toast (e.g. "Item out of stock", "Payment failed")

**Backend**
- [ ] Create `Order` entity with: `buyerId`, `items` (list of `OrderItem`), `status`, `totalAmount`, `shippingAddress`, `createdAt`
- [ ] Create `OrderItem` entity with: `productId`, `productName`, `quantity`, `priceAtPurchase`
- [ ] Add `POST /api/v1/orders` endpoint:
  - Validate cart is not empty
  - Validate each item's stock availability
  - Deduct stock from each product
  - Create order with status `PENDING`
  - Clear buyer's cart after order creation
  - Return created order with ID
- [ ] Protect with BUYER role only

**Acceptance Criteria**
- A real order is created in the database when buyer checks out
- Stock is reduced for each purchased product
- Cart is cleared automatically after successful checkout
- Buyer is redirected to order confirmation page

---

### Story 3.2 — Order Confirmation Page
**As a** buyer, **I want** to see a confirmation page after placing an order, **so that** I know my purchase was successful.

#### Tasks
**Frontend**
- [ ] Create `src/components/OrderConfirmationPage.tsx` with route `/orders/{orderId}/confirmation`
- [ ] Fetch order details using `orderService.getOrder(orderId)` calling `GET /api/v1/orders/{orderId}`
- [ ] Display: order ID, items ordered, total amount, estimated delivery, shipping address
- [ ] Include a "Continue Shopping" button back to home page
- [ ] Include a "View All Orders" button linking to the buyer dashboard orders tab

**Backend**
- [ ] Add `GET /api/v1/orders/{orderId}` — returns order details for the authenticated buyer

**Acceptance Criteria**
- Buyer sees accurate order details after checkout
- Page is accessible via direct URL with valid order ID
- Non-owner buyers receive `403 Forbidden`

---

## Epic 4 — Order History & Tracking

**Goal:** Buyers can view their complete order history and track the current status of each order.

---

### Story 4.1 — View Order History
**As a** buyer, **I want** to see a list of all my past and current orders in my dashboard, **so that** I can track my purchases.

**Background:** `BuyerDashboard.tsx` uses `mockOrders` hardcoded array. Must connect to real backend.

#### Tasks
**Frontend**
- [ ] Add `orderService.getMyOrders()` calling `GET /api/v1/orders`
- [ ] Replace `mockOrders` in `BuyerDashboard.tsx` with real data fetched on mount
- [ ] Show loading spinner while fetching
- [ ] Show empty state with CTA if no orders exist
- [ ] Display per order: order ID, date, items summary, total amount, status badge

**Backend**
- [ ] Add `GET /api/v1/orders` — returns all orders for the authenticated buyer, sorted by `createdAt` descending
- [ ] Include order status, items, and total in the response

**Acceptance Criteria**
- Buyer sees all real orders in their dashboard
- Orders are sorted newest first
- Empty state shows when buyer has no orders

---

### Story 4.2 — View Order Status
**As a** buyer, **I want** to see the current status of each order (PENDING, SHIPPED, DELIVERED, CANCELLED), **so that** I know when to expect my delivery.

#### Tasks
**Frontend**
- [ ] Add colour-coded status badges:
  - `PENDING` → yellow
  - `SHIPPED` → blue
  - `DELIVERED` → green
  - `CANCELLED` → red
- [ ] Show status label in the order list and order detail views
- [ ] Add a "Track Order" button that shows status history if available

**Backend**
- [ ] Ensure `Order.status` enum includes: `PENDING`, `SHIPPED`, `DELIVERED`, `CANCELLED`
- [ ] Allow seller/admin to update order status via `PUT /api/v1/orders/{id}/status`

**Acceptance Criteria**
- Each order shows its current status with a distinct colour
- Status updates from the seller/admin are reflected on next refresh

---

### Story 4.3 — Cancel Order
**As a** buyer, **I want** to cancel an order that is still pending, **so that** I can change my mind before it is shipped.

#### Tasks
**Frontend**
- [ ] Show "Cancel Order" button only when order status is `PENDING`
- [ ] Show confirmation dialog: "Are you sure you want to cancel Order #[id]?"
- [ ] Call `orderService.cancelOrder(orderId)` on confirmation
- [ ] Update order status in UI immediately after cancellation

**Backend**
- [ ] Add `PUT /api/v1/orders/{id}/cancel` endpoint
- [ ] Return `400 Bad Request` if order status is not `PENDING`
- [ ] On success: set status to `CANCELLED`, restore stock for each item
- [ ] Only the owning buyer can cancel their own order

**Acceptance Criteria**
- Buyer can cancel only PENDING orders
- Cancelling restores product stock
- Orders that are SHIPPED or DELIVERED cannot be cancelled

---

## Epic 5 — Wishlist Management

**Goal:** Buyers can save products to a persistent wishlist and manage it over time.

---

### Story 5.1 — Add Product to Wishlist
**As a** buyer, **I want** to save a product to my wishlist, **so that** I can easily find it and buy it later.

**Background:** Current wishlist is `products.slice(0, 3)` — a display stub with no real backend.

#### Tasks
**Frontend**
- [ ] Add a "♡ Wishlist" / "♥ Wishlisted" toggle button on `ProductCard` and `ProductDetailPage`
- [ ] Create `src/services/wishlistService.ts` with `addToWishlist(productId)` calling `POST /api/v1/wishlist/{productId}`
- [ ] Show filled heart icon when product is in wishlist
- [ ] Update wishlist count in `BuyerDashboard` stats

**Backend**
- [ ] Create `Wishlist` entity: `buyerId`, list of `productId`s
- [ ] Add `POST /api/v1/wishlist/{productId}` — adds product to buyer's wishlist (idempotent)
- [ ] Protect with BUYER role only

**Acceptance Criteria**
- Clicking the wishlist button adds the product to the backend wishlist
- Heart icon turns filled when product is wishlisted
- Adding the same product again does nothing (no duplicate)

---

### Story 5.2 — View & Remove from Wishlist
**As a** buyer, **I want** to see my wishlisted products and remove ones I'm no longer interested in, **so that** my wishlist stays relevant.

#### Tasks
**Frontend**
- [ ] Add `wishlistService.getWishlist()` calling `GET /api/v1/wishlist`
- [ ] Populate the Wishlist tab in `BuyerDashboard` with real fetched data
- [ ] Add `wishlistService.removeFromWishlist(productId)` calling `DELETE /api/v1/wishlist/{productId}`
- [ ] Show "Remove" button on each wishlist item
- [ ] Add "Add to Cart" button directly from the wishlist card

**Backend**
- [ ] Add `GET /api/v1/wishlist` — returns all wishlisted products for the authenticated buyer
- [ ] Add `DELETE /api/v1/wishlist/{productId}` — removes the product from the wishlist

**Acceptance Criteria**
- Wishlist tab shows real persisted products from the backend
- Removing a product from wishlist updates UI immediately
- Buyer can add a wishlisted product directly to cart

---

## Epic 6 — Product Reviews & Ratings

**Goal:** Buyers who have purchased a product can leave one review per product, and all users can see average ratings.

---

### Story 6.1 — Submit a Product Review
**As a** buyer, **I want** to write a review and rating for a product I purchased, **so that** I can share my experience with other shoppers.

**Background:** `handleSubmitReview()` calls `addReview()` which only updates in-memory state — nothing is saved to the backend.

#### Tasks
**Frontend**
- [ ] Create `src/services/reviewService.ts` with `submitReview(productId, rating, comment)` calling `POST /api/v1/reviews`
- [ ] Replace `addReview()` call in `ProductDetailPage.tsx` with `reviewService.submitReview()`
- [ ] After successful submission, refresh the product's review list from the backend
- [ ] Disable the "Write a Review" button if the buyer has already reviewed this product
- [ ] Show error message if buyer has not purchased the product

**Backend**
- [ ] Create `Review` entity: `id`, `productId`, `buyerId`, `buyerName`, `rating` (1–5), `comment`, `createdAt`
- [ ] Add `POST /api/v1/reviews` endpoint — body: `{ productId, rating, comment }`
  - Verify buyer has a `DELIVERED` order containing this product
  - Reject if buyer already reviewed this product (`409 Conflict`)
  - Save review; update product's `averageRating`
- [ ] Protect with BUYER role only

**Acceptance Criteria**
- Review is saved to the database and visible to all users
- Buyer cannot review the same product twice
- Buyer can only review products they have actually purchased and received

---

### Story 6.2 — View Product Reviews & Average Rating
**As a** visitor or buyer, **I want** to see all reviews and the average rating for a product, **so that** I can evaluate its quality.

#### Tasks
**Frontend**
- [ ] Add `reviewService.getReviews(productId)` calling `GET /api/v1/reviews?productId={id}`
- [ ] Fetch reviews in `ProductDetailPage` from the backend (replace in-memory reviews)
- [ ] Display star rating, reviewer name, comment, and date
- [ ] Show average rating with star icons at the top of the product page

**Backend**
- [ ] Add `GET /api/v1/reviews?productId={id}` — returns all reviews for a product (public, no auth required)
- [ ] Include `averageRating` and `reviewCount` in the product detail response

**Acceptance Criteria**
- Reviews are loaded from the backend and visible to all users
- Average rating is calculated from real reviews
- Page shows "No reviews yet" when there are none

---

### Story 6.3 — View My Reviews
**As a** buyer, **I want** to see all the reviews I have written, **so that** I can keep track of my feedback.

**Background:** The "My Reviews" tab in `BuyerDashboard` currently shows only an empty state placeholder.

#### Tasks
**Frontend**
- [ ] Add `reviewService.getMyReviews()` calling `GET /api/v1/reviews/me`
- [ ] Populate the "My Reviews" tab in `BuyerDashboard` with real fetched reviews
- [ ] Show per review: product name, product image thumbnail, star rating, comment, date
- [ ] Add a link to the product detail page from each review card

**Backend**
- [ ] Add `GET /api/v1/reviews/me` — returns all reviews written by the authenticated buyer

**Acceptance Criteria**
- Buyer sees all their past reviews in the dashboard
- Each review links to the reviewed product's detail page
- Empty state message appears if buyer has not written any reviews

---

## Epic 7 — Buyer Dashboard

**Goal:** The buyer dashboard displays accurate, real-time stats and provides quick access to all key buyer functions.

---

### Story 7.1 — Dashboard Stats (Real Data)
**As a** buyer, **I want** my dashboard to show accurate counts for orders, total spent, cart items, and wishlist, **so that** I have a clear overview of my activity.

**Background:** All four stat cards in `BuyerDashboard.tsx` currently use mock data.

#### Tasks
**Frontend**
- [ ] Fetch real order history on dashboard mount → derive `totalOrders` and `totalSpent`
- [ ] Use `cartService.getCart()` for `cartItemsCount`
- [ ] Use `wishlistService.getWishlist()` for `wishlistCount`
- [ ] Show skeleton loaders in stat cards while data is loading

**Acceptance Criteria**
- All four stat cards show real data from the backend
- Stats update immediately after cart or wishlist changes

---

### Story 7.2 — Recent Orders Tab (Real Data)
**As a** buyer, **I want** my orders tab to show my real recent orders, **so that** I can track my latest purchases at a glance.

#### Tasks
**Frontend**
- [ ] Connect the Orders tab to `orderService.getMyOrders()` (links to Epic 4, Story 4.1)
- [ ] Show the 5 most recent orders in the dashboard Orders tab
- [ ] Add a "View All Orders" link to see complete history
- [ ] Show colour-coded status badges (PENDING / SHIPPED / DELIVERED / CANCELLED)

**Acceptance Criteria**
- Orders tab shows real data from the backend
- Status badges are colour-coded and accurate

---

### Story 7.3 — Wishlist Tab (Real Data)
**As a** buyer, **I want** my wishlist tab to show my real saved products, **so that** I can easily revisit and purchase them.

#### Tasks
**Frontend**
- [ ] Connect the Wishlist tab to `wishlistService.getWishlist()` (links to Epic 5, Story 5.2)
- [ ] Replace `products.slice(0, 3)` stub with real wishlist products
- [ ] Add "Remove" and "Add to Cart" buttons per wishlist item

**Acceptance Criteria**
- Wishlist tab shows real wishlisted products
- Buyer can add to cart or remove directly from the dashboard

---

### Story 7.4 — My Reviews Tab (Real Data)
**As a** buyer, **I want** my reviews tab to show all the reviews I've written, **so that** I can recall my past feedback.

#### Tasks
**Frontend**
- [ ] Connect the Reviews tab to `reviewService.getMyReviews()` (links to Epic 6, Story 6.3)
- [ ] Show product name, star rating, comment, and date per review card

**Acceptance Criteria**
- Reviews tab shows real buyer reviews from the backend
- Empty state appears if no reviews have been written

---

## 📅 Suggested Delivery Order

| Week | Stories | Priority |
|---|---|---|
| **Week 1** | 1.1 (Product API), 1.4 (Product Detail API), 2.4 (Load Cart from backend) | 🔴 Foundation — real data |
| **Week 2** | 2.1 (Add to Cart), 2.2 (Update Qty), 2.3 (Remove Item), 2.5 (Clear Cart) | 🔴 Cart persistence |
| **Week 3** | 3.1 (Checkout), 3.2 (Order confirmation), 4.1 (Order history), 4.2 (Order status) | 🔴 Core purchase flow |
| **Week 4** | 5.1 (Wishlist add), 5.2 (Wishlist view/remove), 6.1 (Submit review), 6.2 (View reviews) | 🟡 Engagement features |
| **Week 5** | 1.2 (Filter), 1.3 (Search), 4.3 (Cancel order), 6.3 (My reviews), 7.1–7.4 (Dashboard) | 🟢 Polish + dashboard wiring |

---

## 🧪 Testing Checkpoints (per Story)

Each story should pass:
- [ ] **Happy path:** feature works as expected end-to-end
- [ ] **Error path:** invalid inputs or failures show correct error messages
- [ ] **Auth path:** unauthenticated access is blocked (`401 Unauthorized`)
- [ ] **Role path:** non-BUYER roles are blocked (`403 Forbidden`) where applicable
- [ ] **Persistence:** data survives page refresh (fetched from backend, not localStorage)
- [ ] **Empty state:** appropriate UI shown when no data exists
