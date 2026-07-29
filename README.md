# TMDT Bookshop

Ứng dụng bán sách dùng Java 17, Spring Boot 2.5, Thymeleaf, Spring Security,
Spring Data JPA, MySQL và Flyway.

## Toolchain cố định

- JDK: 17
- Maven Wrapper: 3.3.4
- Maven: 3.9.11
- Lombok: 1.18.46
- Maven Compiler Plugin: 3.15.0

Build sẽ dừng ngay nếu chạy bằng JDK khác Java 17. Wrapper có checksum SHA-256
cho cả Maven distribution và wrapper JAR.

```powershell
.\mvnw.cmd -version
.\mvnw.cmd clean package
```

## Chạy với Docker MySQL

MySQL container dùng cổng `3308` để không xung đột AMPPS hoặc container MySQL khác.

```powershell
docker compose up -d
docker compose ps
.\run-local.ps1 -Database Docker
```

Flyway tự chạy `V1__baseline_schema.sql` khi database Docker còn trống.

## Chạy với AMPPS

Mở AMPPS, bật MySQL và chạy:

```powershell
.\run-local.ps1
```

Script đọc mật khẩu AMPPS vào biến môi trường, kích hoạt profile `dev` và không
ghi mật khẩu vào source hoặc log. Với database AMPPS cũ chưa có lịch sử Flyway,
profile `dev` baseline schema hiện tại tại version 1 rồi mới chạy migration mới.

## Profiles

- `dev`: MySQL local, Flyway bật, cho phép baseline database legacy.
- `test`: H2 trong bộ nhớ, Flyway tạo schema từ đầu, scheduler tắt.
- `prod`: bắt buộc `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; không tự baseline.

Ví dụ production:

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:DB_URL='jdbc:mysql://db-host:3306/bookshop'
$env:DB_USERNAME='bookshop_app'
$env:DB_PASSWORD='use-a-secret-manager'
java -jar target\CT07-1.0.jar
```

Không dùng giá trị development trong production. Mail và PayPal cần các biến
`MAIL_USERNAME`, `MAIL_PASSWORD`, `PAYPAL_CLIENT_ID` và
`PAYPAL_CLIENT_SECRET`.

## Quy tắc Flyway

- Không sửa migration đã được dùng chung.
- Mỗi thay đổi schema tiếp theo tạo file mới như
  `V2__add_product_slug.sql`.
- Production legacy phải được kiểm tra và baseline có chủ đích trước lần deploy
  Flyway đầu tiên.
