# TMDT Bookshop

Ứng dụng web bán sách viết bằng Java 17, Spring Boot 2.5, Thymeleaf, Spring Security,
Spring Data JPA và MySQL.

## Chạy local với AMPPS

1. Mở AMPPS và bật MySQL, bảo đảm database `bookshop` tồn tại.
2. Mở PowerShell tại thư mục dự án.
3. Chạy:

   ```powershell
   .\run-local.ps1
   ```

4. Mở `http://localhost:8080`.

Script đọc mật khẩu MySQL từ cấu hình AMPPS vào biến môi trường và không ghi mật
khẩu vào source hoặc log. Dùng `.\run-local.ps1 -SkipBuild` nếu JAR đã được build.

Mail và PayPal chỉ hoạt động khi thiết lập các biến `MAIL_USERNAME`,
`MAIL_PASSWORD`, `PAYPAL_CLIENT_ID` và `PAYPAL_CLIENT_SECRET`. Xem `.env.example`
để biết toàn bộ tên biến; không commit giá trị thật.

## Build và test

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

Test sử dụng H2 trong bộ nhớ, không kết nối hoặc thay đổi database MySQL local.
