# Quality gates

## Kiểm tra local

Yêu cầu JDK 17 và Docker Desktop đang chạy:

```powershell
.\scripts\check.ps1
```

Script thực hiện theo thứ tự:

1. Từ chối file build, log, upload, runtime, `.env` và credential literal trong cấu
   hình nếu chúng xuất hiện trong source hiện tại.
2. Chạy Maven Wrapper 3.9.11 với `clean verify`.
3. Chạy test kiến trúc, unit, H2 và MySQL 8.4 Testcontainers.
4. Sinh báo cáo JaCoCo tại `target/site/jacoco/index.html`.
5. Làm build thất bại nếu coverage thấp hơn 54% line hoặc 27% branch.

Không hạ coverage gate khi test bị thiếu. Hãy bổ sung test rồi tăng dần hai property
`coverage.line.minimum` và `coverage.branch.minimum` trong `pom.xml`.

## GitHub Actions

Workflow `.github/workflows/ci.yml` chạy khi có pull request, push vào `main` hoặc
được gọi thủ công:

- `Maven verify (JDK 17)` chạy repository hygiene, toàn bộ test và JaCoCo gate.
- `Docker Compose smoke test` build image thật, khởi động MySQL/Flyway, kiểm tra trang
  chủ và `/api/v1/products`, sau đó luôn dọn container/volume tạm của runner.

Workflow sử dụng token chỉ đọc, không cần mail/PayPal/database secret và ghim GitHub
Action bằng commit SHA. Không đổi sang `pull_request_target` cho việc build mã từ PR.

## Quy tắc repository

Được commit:

- mã nguồn, migration Flyway, test và tài liệu;
- `.env.example` với giá trị local giả lập;
- Maven Wrapper kèm checksum.

Không được commit:

- `.env` hoặc credential thật;
- `target/`, `bin/`, log, dump, file crash;
- `upload/` và dữ liệu MySQL/runtime;
- artifact JAR/WAR sinh ra từ build.
