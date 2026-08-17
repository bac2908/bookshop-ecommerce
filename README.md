# TMDT Bookshop

Ứng dụng bán sách dùng Java 17, Spring Boot 3.5.16, Thymeleaf, Spring Security,
Spring Data JPA, MySQL 8.4 và Flyway.

## Chạy dự án bằng một lệnh

Yêu cầu duy nhất: Docker Desktop đang chạy.

```powershell
.\scripts\dev.ps1
```

Script sẽ build hai container, chờ MySQL khỏe và chỉ báo thành công sau khi trang web
trả HTTP 200. Khi image không thay đổi, có thể khởi động nhanh bằng
`.\scripts\dev.ps1 -NoBuild`.

Lệnh Docker Compose tương đương là `docker compose up -d --build`.

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

## Kiểm tra chất lượng

Yêu cầu JDK 17 và Docker Desktop. Chạy cùng quality gate với CI bằng một lệnh:

```powershell
.\scripts\check.ps1
```

Lệnh này từ chối file runtime/secret bị theo dõi, chạy toàn bộ 45 test trên H2 và
MySQL Testcontainers, sau đó kiểm tra JaCoCo tối thiểu 54% line và 27% branch.
Báo cáo coverage nằm tại `target/site/jacoco/index.html`.

GitHub Actions chạy Maven quality gate và Docker Compose smoke test độc lập cho mọi
pull request và lần push vào `main`. Dependabot kiểm tra Maven, GitHub Actions và
Docker hằng tuần. Chi tiết xem [`docs/QUALITY_GATES.md`](docs/QUALITY_GATES.md).

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

Frontend dùng thống nhất Bootstrap 4 và jQuery 3.6.0. Các chức năng đã migrate
thay đổi dữ liệu qua `window.bookshopApi` và `/api/v1/**`; MVC controller chỉ
render trang cho các lát nghiệp vụ đó.

Xem luồng xử lý, quy tắc dependency và cách tìm lỗi tại
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

Lộ trình chuyển từng chức năng sang REST API được theo dõi tại
[`docs/API_MIGRATION.md`](docs/API_MIGRATION.md).
