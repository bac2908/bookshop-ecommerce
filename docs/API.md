# Bookshop REST API

API hiện tại dùng prefix `/api/v1` và chạy trong cùng ứng dụng Spring Boot với
giao diện Thymeleaf. Controller API và controller MVC dùng chung application
service; frontend không kết nối trực tiếp tới MySQL.

## Tài liệu tương tác

Khi chạy profile `dev`:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs/bookshop-v1`

Swagger và OpenAPI được tắt mặc định ở profile `prod`. Chỉ bật lại khi endpoint
được bảo vệ phù hợp:

```text
SPRINGDOC_API_DOCS_ENABLED=true
SPRINGDOC_SWAGGER_UI_ENABLED=true
```

## Endpoint v1

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/v1/products` | Công khai | Phân trang, tìm kiếm, lọc và sắp xếp sản phẩm đang bán |
| GET | `/api/v1/products/{id}` | Công khai | Chi tiết sản phẩm đang bán |
| GET | `/api/v1/categories` | Công khai | Danh sách thể loại đang hoạt động |
| GET | `/api/v1/publishers` | Công khai | Danh sách nhà xuất bản đang hoạt động |
| POST | `/api/v1/products` | ADMIN hoặc STAFF | Tạo sản phẩm |
| PUT | `/api/v1/products/{id}` | ADMIN hoặc STAFF | Cập nhật sản phẩm |
| DELETE | `/api/v1/products/{id}` | ADMIN hoặc STAFF | Xóa hoặc ẩn sản phẩm |

API ghi dữ liệu đang dùng cơ chế đăng nhập session của website. Vì vậy request
`POST`, `PUT`, `DELETE` cần cả cookie `JSESSIONID` và header `X-CSRF-TOKEN`.

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
`PRODUCT_NAME_EXISTS`, `AUTHENTICATION_REQUIRED`, `CSRF_TOKEN_INVALID`,
`ACCESS_DENIED` và `INTERNAL_ERROR`.

## Integration test

`ProductApiIntegrationTest` tạo một MySQL 8.4 container độc lập, chạy Flyway từ
schema rỗng rồi kiểm tra API, validation, security và OpenAPI. Docker Desktop
phải chạy khi thực thi:

```powershell
.\mvnw.cmd test
```
