# Bookshop REST API

API dùng prefix `/api/v1` và chạy cùng ứng dụng Spring Boot với giao diện
Thymeleaf. Controller API và controller MVC dùng chung application service;
frontend không kết nối trực tiếp tới MySQL.

## Tài liệu tương tác

Khi chạy profile `dev`:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs/bookshop-v1`

Swagger và OpenAPI mặc định bị tắt ở profile `prod`. Chỉ bật lại khi các endpoint
tài liệu đã được bảo vệ phù hợp:

```text
SPRINGDOC_API_DOCS_ENABLED=true
SPRINGDOC_SWAGGER_UI_ENABLED=true
```

## Endpoint v1

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/v1/auth/csrf` | Công khai | Tạo/đọc CSRF token cho trình duyệt |
| POST | `/api/v1/auth/login` | Công khai + CSRF | Đăng nhập bằng JSON và tạo session |
| GET | `/api/v1/auth/me` | Đã đăng nhập | Lấy người dùng hiện tại và vai trò |
| POST | `/api/v1/auth/logout` | Session hiện tại + CSRF | Đăng xuất và hủy session |
| GET | `/api/v1/cart` | Đã đăng nhập | Lấy snapshot giỏ hàng hiện tại |
| POST | `/api/v1/cart/items` | Đã đăng nhập + CSRF | Thêm sản phẩm hoặc tăng số lượng |
| PUT | `/api/v1/cart/items/{productId}` | Đã đăng nhập + CSRF | Đặt số lượng chính xác |
| DELETE | `/api/v1/cart/items/{productId}` | Đã đăng nhập + CSRF | Xóa một sản phẩm khỏi giỏ |
| DELETE | `/api/v1/cart/items` | Đã đăng nhập + CSRF | Xóa toàn bộ giỏ |
| GET | `/api/v1/favorites` | Đã đăng nhập | Lấy danh sách yêu thích |
| PUT | `/api/v1/favorites/{productId}` | Đã đăng nhập + CSRF | Thêm yêu thích theo cách idempotent |
| DELETE | `/api/v1/favorites/{productId}` | Đã đăng nhập + CSRF | Xóa yêu thích theo cách idempotent |
| POST | `/api/v1/checkout` | Đã đăng nhập + CSRF | Đặt COD hoặc tạo PayPal order và giữ tồn kho |
| POST | `/api/v1/checkout/paypal/capture` | Đã đăng nhập + CSRF | Capture PayPal idempotent sau khi approve |
| POST | `/api/v1/checkout/paypal/cancel` | Đã đăng nhập + CSRF | Hủy PayPal đang chờ và hoàn tồn kho |
| GET | `/api/v1/orders` | Đã đăng nhập | Danh sách đơn của người dùng hiện tại |
| GET | `/api/v1/orders/{orderId}` | Chủ đơn | Chi tiết đơn, dòng hàng và thanh toán |
| PUT | `/api/v1/orders/{orderId}/cancellation` | Chủ đơn + CSRF | Hủy đơn idempotent và hoàn tồn kho đúng một lần |
| GET | `/api/v1/products` | Công khai | Phân trang, tìm kiếm, lọc và sắp xếp sản phẩm đang bán |
| GET | `/api/v1/products/{id}` | Công khai | Chi tiết sản phẩm đang bán |
| GET | `/api/v1/categories` | Công khai | Danh sách thể loại đang hoạt động |
| GET | `/api/v1/publishers` | Công khai | Danh sách nhà xuất bản đang hoạt động |
| POST | `/api/v1/products` | ADMIN hoặc STAFF | Tạo sản phẩm |
| PUT | `/api/v1/products/{id}` | ADMIN hoặc STAFF | Cập nhật sản phẩm |
| DELETE | `/api/v1/products/{id}` | ADMIN hoặc STAFF | Xóa hoặc ẩn sản phẩm |
| GET | `/api/v1/admin/users` | ADMIN | Tìm kiếm/phân trang người dùng |
| GET | `/api/v1/admin/users/{userId}` | ADMIN | Chi tiết quyền truy cập |
| PUT | `/api/v1/admin/users/{userId}/access` | ADMIN + CSRF | Cập nhật trạng thái và vai trò |
| GET | `/api/v1/admin/roles` | ADMIN | Danh sách vai trò có thể gán |
| GET | `/api/v1/admin/categories` | ADMIN hoặc STAFF | Tất cả thể loại, gồm bản ghi đã ẩn |
| POST | `/api/v1/admin/categories` | ADMIN hoặc STAFF + CSRF | Tạo thể loại bằng multipart `data` + `file` |
| PUT | `/api/v1/admin/categories/{id}` | ADMIN hoặc STAFF + CSRF | Cập nhật thể loại/ảnh |
| PUT | `/api/v1/admin/categories/{id}/visibility` | ADMIN hoặc STAFF + CSRF | Ẩn/hiện thể loại và sản phẩm con |
| GET | `/api/v1/admin/publishers` | ADMIN hoặc STAFF | Tất cả nhà xuất bản |
| POST | `/api/v1/admin/publishers` | ADMIN hoặc STAFF + CSRF | Tạo nhà xuất bản |
| PUT | `/api/v1/admin/publishers/{id}` | ADMIN hoặc STAFF + CSRF | Cập nhật nhà xuất bản |
| PUT | `/api/v1/admin/publishers/{id}/visibility` | ADMIN hoặc STAFF + CSRF | Ẩn/hiện NXB và sản phẩm con |
| GET | `/api/v1/admin/dashboard?days=7` | ADMIN hoặc STAFF | Snapshot KPI và chuỗi dữ liệu dashboard |
| GET | `/api/v1/admin/reports?groupBy=PRODUCT` | ADMIN | Báo cáo doanh số đơn hoàn tất |

## Luồng đăng nhập từ frontend

Hệ thống dùng cookie session `JSESSIONID`; không lưu access token trong
`localStorage`. Trình duyệt thực hiện theo thứ tự:

1. `GET /api/v1/auth/csrf`.
2. Gửi giá trị `token` trong header có tên nhận từ trường `headerName`.
3. `POST /api/v1/auth/login` với JSON `{ "login": "...", "password": "..." }`.
4. Gọi lại `GET /api/v1/auth/csrf` vì token được đổi sau đăng nhập.
5. Gửi cookie session và CSRF header cho mọi request `POST`, `PUT`, `PATCH`,
   `DELETE`.
6. Sau `POST /api/v1/auth/logout`, gọi lại endpoint CSRF nếu trang vẫn tiếp tục
   hoạt động.

Frontend đã có client dùng chung tại
`src/main/resources/static/storefront/js/api-client.js`:

```javascript
const currentUser = await window.bookshopApi.me();
const products = await window.bookshopApi.get("/api/v1/products?page=0&size=12");
const cart = await window.bookshopApi.addCartItem(1, 2);
await window.bookshopApi.addFavorite(1);
const checkout = await window.bookshopApi.checkout({
  address: "123 Nguyễn Huệ, TP.HCM",
  phone: "0900000000",
  note: "Giao giờ hành chính",
  paymentMethod: "COD"
});
const orders = await window.bookshopApi.orders(0, 10);
await window.bookshopApi.logout();
```

Client chỉ cho phép URL cùng origin, tự gửi cookie, tự gắn CSRF header và thử lại
một lần nếu token vừa hết hiệu lực.

## Hợp đồng lỗi

Mọi lỗi API trả cùng cấu trúc:

```json
{
  "timestamp": "2026-07-30T08:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu gửi lên không hợp lệ",
  "path": "/api/v1/products",
  "fieldErrors": [
    {
      "field": "name",
      "message": "Tên sản phẩm không được để trống"
    }
  ]
}
```

Các mã chính gồm `VALIDATION_FAILED`, `INVALID_REQUEST`,
`PRODUCT_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `PUBLISHER_NOT_FOUND`,
`PRODUCT_NAME_EXISTS`, `INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`,
`PRODUCT_UNAVAILABLE`, `CART_ITEM_NOT_FOUND`, `INSUFFICIENT_STOCK`,
`CART_EMPTY`, `CART_CHANGED`, `ORDER_NOT_FOUND`, `INVALID_ORDER_STATE`,
`PAYMENT_PROVIDER_ERROR`, `PAYMENT_CAPTURE_FAILED`,
`USER_NOT_FOUND`, `USER_ROLE_REQUIRED`, `INVALID_ROLE`,
`SELF_DISABLE_FORBIDDEN`, `SELF_DEMOTION_FORBIDDEN`,
`LAST_ADMIN_PROTECTED`, `CATEGORY_NAME_EXISTS`, `PUBLISHER_NAME_EXISTS`,
`AUTHENTICATION_REQUIRED`, `CSRF_TOKEN_INVALID`, `ACCESS_DENIED` và
`INTERNAL_ERROR`.

## Integration test

`BookshopApiIntegrationTest` tạo một MySQL 8.4 container độc lập, chạy Flyway từ
schema rỗng rồi kiểm tra API, validation, session rotation, CSRF, phân quyền,
checkout COD/PayPal, tồn kho, quyền sở hữu đơn, callback idempotent, phân quyền
quản trị, catalog multipart, báo cáo loại trừ đơn hủy và OpenAPI.
Docker Desktop phải chạy khi thực thi:

```powershell
.\mvnw.cmd test
```

Lộ trình chuyển từng chức năng MVC sang API được ghi tại
[`API_MIGRATION.md`](API_MIGRATION.md).
