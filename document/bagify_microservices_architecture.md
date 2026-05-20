# Bagify — Complete Microservices Architecture Plan

> Derived from full analysis of your existing `user` service, Eureka server, all frontend components  
> (SellerDashboard, BuyerDashboard, AdminDashboard, CartPage, HomePage, ProductDetailPage),  
> and all entity/service/controller/DTO files.

---

## What You Already Have

| Service | Status | Port |
|---|---|---|
| `eureka-server` | ✅ Done | 8761 |
| `user` | ✅ Done (Auth, Registration, Profile, Verification) | 8081 |

---

## Services You Must Create

Based on the full frontend feature set and the domain model, you need **4 more microservices**:

| # | Service | Domain Responsibility | Port |
|---|---|---|---|
| 1 | `product` | Product catalogue, reviews | 8082 |
| 2 | `order` | Cart checkout → Orders, order history | 8083 |
| 3 | `notification` | Email triggers (order placed, shipped, etc.) | 8084 |
| 4 | `api-gateway` | Single entry-point, JWT routing | 8080 |

> [!IMPORTANT]
> An **API Gateway** is non-negotiable in a microservices setup. Without it, your frontend must know the port of every service — that is anti-pattern. All requests go through port `8080`.

---

## Why These Services — Detailed Justification

### 1. `product` Service
**Evidence from your code:**
- `ProductProvider.tsx` holds a `Product` type with `id, name, description, price, stock, category, image, sellerId, sellerName, sellerRating, reviews, averageRating`
- `SellerDashboard.tsx` calls `addProduct()` and `deleteProduct()`
- `AdminDashboard.tsx` shows all products, deletes any product
- `HomePage.tsx` browses products with search + category filter
- `ProductDetailPage.tsx` renders product details and reviews
- Your `UserService.deleteAccount()` already has a comment: *"Note: Detailed OrderRepository checks are currently omitted as the Order service logic is not present"* — confirming the author expected separate services
- The `Product` type is **entirely independent** of any user entity

**Owns:** Product CRUD, stock management, reviews, search/filter

---

### 2. `order` Service
**Evidence from your code:**
- `CartPage.tsx` has a full checkout flow: subtotal, shipping, tax, `handleCheckout()`
- `BuyerDashboard.tsx` shows "Recent Orders" with status: `pending | shipped | delivered | cancelled`
- `mockOrders` in `types/index.ts` shows the full `Order` type: `id, buyerId, products[], totalAmount, status, orderDate, shippingAddress`
- `Buyer.java` entity has `totalOrders` and `totalSpent` fields — these must be kept in sync from order events
- `UserService.deleteAccount()` comment: *"Buyer can delete only if they have no ongoing orders (status not PENDING or SHIPPED)"* — this check must query the order service

**Owns:** Order lifecycle, order status tracking, checkout, order history

---

### 3. `notification` Service
**Evidence from your code:**
- `UserService.java` already has `EmailService` injected for password reset emails
- `EmailService.java` exists and sends emails via Spring Mail
- Orders placed/shipped/delivered will need buyer email notifications
- Seller verification status changes need email notifications (APPROVED/REJECTED)
- This logic should NOT live in `user` or `order` — it's a pure side-effect listener

**Owns:** All outbound emails, event-driven via async messaging (or REST fallback for now)

---

### 4. `api-gateway` (Spring Cloud Gateway)
**Evidence from your code:**
- `authservice.ts` and `userservice.ts` both currently point to `localhost:8081`
- `productService.ts` (to be created) would point to `localhost:8082`
- Each service has its own SecurityConfig — that's duplication
- The gateway is the **only place** that needs CORS, rate limiting, and JWT pre-validation

**Owns:** Routing, CORS, rate limiting, JWT pre-validation for protected services

---

## Service Interaction Map

```
Frontend (port 5173)
        │
        ▼
  [api-gateway :8080]  ← Single entry-point
   │       │       │       │
   ▼       ▼       ▼       ▼
[user]  [product] [order] [notification]
:8081    :8082     :8083     :8084
   │                │
   └───── Eureka ───┘  ← Service Discovery
           :8761
```

**Inter-service calls:**
- `order` → `product` (verify stock, reduce stock on purchase)
- `order` → `user` (get buyer shipping address)
- `order` → `notification` (trigger email on status change)
- `user` → `notification` (already done — password reset email)

---

## Complete File Structure

### Service 1: `product`

```
bagify-backend/product/
├── pom.xml
└── src/main/java/com/mycompany/app/product/
    ├── ProductApplication.java
    │
    ├── config/
    │   ├── SecurityConfig.java       ← permitAll for GET /api/v1/products
    │   └── JwtFilter.java            ← same JWT filter as user service
    │
    ├── controller/
    │   ├── ProductController.java    ← GET/POST/PUT/DELETE /api/v1/products
    │   └── ReviewController.java     ← POST /api/v1/products/{id}/reviews
    │
    ├── service/
    │   ├── ProductService.java
    │   └── ReviewService.java
    │
    ├── repository/
    │   ├── ProductRepository.java
    │   └── ReviewRepository.java
    │
    ├── entity/
    │   ├── Product.java              ← id, name, desc, price, stock, category, imageUrl,
    │   │                                sellerId, sellerName, sellerRating, status(ACTIVE/DRAFT)
    │   └── Review.java               ← id, productId, buyerId, buyerName, rating, comment, date
    │
    ├── dto/
    │   ├── ProductRequest.java       ← create/update payload
    │   ├── ProductResponse.java      ← full response with computed averageRating
    │   └── ReviewRequest.java
    │
    └── exception/
        └── GlobalExceptionHandler.java
```

**Key endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/products` | None | Browse all active products |
| GET | `/api/v1/products/{id}` | None | Product detail + reviews |
| POST | `/api/v1/products` | SELLER | Create product |
| PUT | `/api/v1/products/{id}` | SELLER (owner) | Update product |
| DELETE | `/api/v1/products/{id}` | SELLER (owner) / ADMIN | Delete product |
| POST | `/api/v1/products/{id}/reviews` | BUYER | Add review |

---

### Service 2: `order`

```
bagify-backend/order/
├── pom.xml
└── src/main/java/com/mycompany/app/order/
    ├── OrderApplication.java
    │
    ├── config/
    │   ├── SecurityConfig.java       ← All endpoints require JWT
    │   ├── JwtFilter.java
    │   └── WebClientConfig.java      ← For calling product-service (stock check)
    │
    ├── controller/
    │   └── OrderController.java      ← POST /checkout, GET /my-orders, PATCH /status
    │
    ├── service/
    │   ├── OrderService.java
    │   └── ProductClient.java        ← WebClient call to product service
    │
    ├── repository/
    │   └── OrderRepository.java
    │
    ├── entity/
    │   ├── Order.java                ← id, buyerId, status, totalAmount, shippingAddress,
    │   │                                orderDate, items[]
    │   └── OrderItem.java            ← productId, productName, quantity, price, imageUrl
    │
    ├── dto/
    │   ├── CheckoutRequest.java      ← shippingAddress, items[]
    │   ├── OrderResponse.java
    │   └── OrderItemDto.java
    │
    └── exception/
        └── GlobalExceptionHandler.java
```

**Key endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/orders/checkout` | BUYER | Place an order from cart |
| GET | `/api/v1/orders/my-orders` | BUYER | Get buyer's order history |
| GET | `/api/v1/orders/{id}` | BUYER (owner) / ADMIN | Get order detail |
| PATCH | `/api/v1/orders/{id}/status` | SELLER / ADMIN | Update order status |
| GET | `/api/v1/orders/seller-orders` | SELLER | Get orders for seller's products |

---

### Service 3: `notification`

```
bagify-backend/notification/
├── pom.xml
└── src/main/java/com/mycompany/app/notification/
    ├── NotificationApplication.java
    │
    ├── config/
    │   └── MailConfig.java           ← Spring Mail configuration
    │
    ├── controller/
    │   └── NotificationController.java  ← Internal REST endpoint (called by other services)
    │
    ├── service/
    │   └── EmailService.java         ← Send order confirmation, shipped, delivered, OTP emails
    │
    ├── dto/
    │   ├── OrderConfirmationDto.java
    │   ├── ShipmentNotificationDto.java
    │   └── SellerVerificationDto.java
    │
    └── templates/                    ← Thymeleaf or plain-text email templates
        ├── order-confirmation.html
        ├── order-shipped.html
        └── seller-verification.html
```

> [!NOTE]
> For this project scope, `notification` receives REST calls from `order` and `user` services.  
> In production you'd swap to Kafka/RabbitMQ events — but REST is fine for now.

**Key endpoints (internal only, not exposed via gateway):**
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/internal/notify/order-placed` | Service-to-service | Send buyer confirmation email |
| POST | `/internal/notify/order-shipped` | Service-to-service | Send shipping notification |
| POST | `/internal/notify/verification-result` | Service-to-service | Seller approved/rejected email |

---

### Service 4: `api-gateway`

```
bagify-backend/api-gateway/
├── pom.xml
└── src/main/resources/
    └── application.yml               ← All routing rules here (no Java needed)
```

**`application.yml` routing rules:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user                    # Eureka load balancing
          predicates:
            - Path=/api/v1/auth/**, /api/v1/users/**
        - id: product-service
          uri: lb://product
          predicates:
            - Path=/api/v1/products/**
        - id: order-service
          uri: lb://order
          predicates:
            - Path=/api/v1/orders/**
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:5173"
            allowedMethods: "*"
            allowedHeaders: "*"
```

> [!IMPORTANT]
> Once the API Gateway is up, your frontend changes ALL base URLs from  
> `http://localhost:8081` → `http://localhost:8080`.  
> The gateway handles routing to the right service automatically.

---

## Updated Root `pom.xml` Modules

```xml
<modules>
    <module>eureka-server</module>   <!-- ✅ exists -->
    <module>user</module>            <!-- ✅ exists -->
    <module>product</module>         <!-- 🆕 create -->
    <module>order</module>           <!-- 🆕 create -->
    <module>notification</module>    <!-- 🆕 create -->
    <module>api-gateway</module>     <!-- 🆕 create -->
</modules>
```

---

## Service Startup Order

```
1. eureka-server  (must be first — all others register here)
2. user           (auth service — gateway depends on JWT validation)
3. product        (no dependencies)
4. notification   (no dependencies)
5. order          (depends on product for stock check)
6. api-gateway    (last — routes to all others)
```

---

## Database Strategy

Each service owns its own database schema (Database-per-Service pattern):

| Service | DB Schema | Key Tables |
|---|---|---|
| `user` | `bagify_users` | `users`, `buyers`, `sellers` |
| `product` | `bagify_products` | `products`, `reviews` |
| `order` | `bagify_orders` | `orders`, `order_items` |
| `notification` | stateless | (no DB needed) |

> [!WARNING]
> **Never share a database between two microservices.** Cross-service data needs  
> (e.g., `order` needs product name) should be captured at write-time as a **denormalized snapshot**  
> inside `OrderItem` — NOT by joining across service databases at read-time.

---

## What Stays in `user` Service

The `user` service is already well-built. Keep it as-is with these additions:

- Add `GET /api/v1/users/sellers/{id}` — public endpoint for product service to fetch seller info when creating a product
- Add `GET /api/v1/users/buyers/{id}` — internal endpoint for order service to fetch buyer's shipping address
- Move `EmailService` to `notification` service eventually (or keep for auth emails — both is fine)

---

## Implementation Order (Recommended)

```
Phase 1 (Current Story):  product service  ← Story 1.1 requires this
Phase 2:                  api-gateway      ← Clean up all frontend URLs
Phase 3:                  order service    ← Checkout, order history
Phase 4:                  notification     ← Email on order events
```
