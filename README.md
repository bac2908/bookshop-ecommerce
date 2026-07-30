# TMDT Bookshop

Ứng dụng bán sách dùng Java 17, Spring Boot 3.5.16, Thymeleaf, Spring Security,
Spring Data JPA, MySQL 8.4 và Flyway.

## Chạy toàn bộ dự án bằng Docker

Yêu cầu duy nhất: Docker Desktop đang chạy.

```powershell
docker compose up -d --build
```

REST API và tài liệu phát triển:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs/bookshop-v1
- Quy ước API: [`docs/API.md`](docs/API.md)

Sau khi hai container ở trạng thái chạy, mở:

- Website: http://localhost:8080
- MySQL từ máy host: `localhost:3308`

Kiểm tra trạng thái và log:

```powershell
docker compose ps
docker compose logs -f app
```

Dừng ứng dụng nhưng giữ nguyên dữ liệu:

```powershell
docker compose down
```

Docker Compose gồm:

- `bookshop-app`: Spring Boot chạy bằng JRE 17.
- `bookshop-mysql`: MySQL 8.4.
- `bookshop_mysql_data`: volume lưu dữ liệu MySQL qua các lần restart.
- `./upload`: thư mục ảnh runtime được mount vào `/app/upload`.

Flyway tự tạo và nâng cấp schema khi container ứng dụng khởi động. Không import
SQL thủ công và không sửa migration đã được áp dụng.

## Cấu hình

Giá trị mặc định trong `compose.yaml` chỉ dành cho development. Khi cần cấu hình
mail, PayPal, cổng hoặc mật khẩu MySQL:

```powershell
Copy-Item .env.example .env
```

Sau đó chỉnh `.env` và chạy lại:

```powershell
docker compose up -d --build
```

Không commit `.env` hoặc secret thật vào Git.

## Tạo quản trị viên đầu tiên

Hệ thống không tạo tài khoản ADMIN với mật khẩu mặc định. Khi database chưa có
ADMIN, điền các biến sau trong `.env`:

```text
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_BOOTSTRAP_USERNAME=your_admin
ADMIN_BOOTSTRAP_EMAIL=your_admin@example.com
ADMIN_BOOTSTRAP_PASSWORD=use-a-unique-password-at-least-12-characters
ADMIN_BOOTSTRAP_NAME=Administrator
ADMIN_BOOTSTRAP_PHONE=0900000000
```

Chạy `docker compose up -d --build`, đăng nhập thành công, rồi đổi
`ADMIN_BOOTSTRAP_ENABLED=false` và chạy lại container. Bootstrap không ghi mật
khẩu ra log và tự bỏ qua khi đã có ít nhất một ADMIN hoạt động.

## Các lệnh hữu ích

Build và chạy test trực tiếp trên máy yêu cầu JDK 17:

```powershell
.\mvnw.cmd clean package
```

Build lại riêng ứng dụng:

```powershell
docker compose build app
docker compose up -d app
```

Xóa toàn bộ dữ liệu MySQL để khởi tạo mới hoàn toàn:

```powershell
docker compose down -v
docker compose up -d --build
```

Lưu ý: `docker compose down -v` xóa vĩnh viễn database Docker hiện tại.

## Kiến trúc

Backend được chia thành `presentation`, `application`, `domain` và
`infrastructure`. Frontend tách riêng `storefront`, `admin` và `email`.

Xem luồng xử lý, quy tắc dependency và cách tìm lỗi tại
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

Lộ trình chuyển từng chức năng sang REST API được theo dõi tại
[`docs/API_MIGRATION.md`](docs/API_MIGRATION.md).
