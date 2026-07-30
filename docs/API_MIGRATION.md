# Lộ trình chuyển sang API-first

## Nguyên tắc

Dự án chuyển theo từng lát nghiệp vụ, không viết lại toàn bộ một lần:

- Thymeleaf tiếp tục đảm nhiệm render trang trong thời gian chuyển đổi.
- MVC controller và REST controller chỉ điều phối; nghiệp vụ nằm trong
  application service dùng chung.
- Frontend chỉ gọi `/api/v1/**`, không truy cập database.
- Mỗi chức năng chỉ bỏ luồng MVC cũ sau khi API mới có đủ test và giao diện đã
  chuyển hoàn toàn.
- API v1 giữ hợp đồng tương thích; thay đổi phá vỡ hợp đồng phải sang version mới.

## Các giai đoạn

| Giai đoạn | Phạm vi | Trạng thái |
|---|---|---|
| 1 | Session auth, CSRF, current user, logout, fetch client dùng chung | Hoàn thành |
| 2 | Giỏ hàng và yêu thích | Hoàn thành |
| 3 | Checkout, tồn kho, đơn hàng và quyền xem/hủy đơn | Hoàn thành |
| 4 | Quản trị người dùng, danh mục, báo cáo | Hoàn thành |
| 5 | Gỡ endpoint MVC mutation trùng lặp và dọn JavaScript cũ | Tiếp theo |

## Tiêu chuẩn hoàn thành cho mỗi lát nghiệp vụ

1. Hợp đồng request/response và mã lỗi được ghi trong OpenAPI.
2. Controller không chứa business logic và không trả JPA entity trực tiếp.
3. Service được dùng chung trong giai đoạn MVC/API cùng tồn tại.
4. Có test happy path, validation, chưa đăng nhập, sai quyền và CSRF.
5. Frontend dùng duy nhất `window.bookshopApi` để gọi API.
6. Regression test cũ vẫn xanh.
7. Chỉ sau đó mới xóa endpoint/form/JavaScript MVC không còn người dùng.

## Quyết định kiến trúc giai đoạn 1

- Dùng session cookie same-origin thay vì JWT vì frontend và backend hiện được
  triển khai cùng một origin.
- Dùng synchronizer CSRF token lưu trong session và gửi qua custom header.
- Session ID được đổi sau khi đăng nhập để chống session fixation.
- Không trả entity `User` ra ngoài; `/auth/me` chỉ trả dữ liệu an toàn và roles.
- Bộ đếm khóa tài khoản/IP được dùng chung cho cả form login và API login.
- Logout API trả `204`; logout MVC vẫn redirect về trang chủ để không phá giao
  diện hiện hữu.

## Quyết định kiến trúc giai đoạn 2

- Giỏ hàng trả một snapshot nhất quán gồm items, số mặt hàng, tổng số lượng và
  tổng tiền; giao diện không phải ghép kết quả từ nhiều endpoint.
- API không âm thầm cắt số lượng theo tồn kho. Yêu cầu vượt kho trả
  `409 INSUFFICIENT_STOCK`.
- Mutation giỏ hàng khóa theo người dùng trong transaction để tránh lost update
  hoặc tạo hai giỏ ACTIVE khi có request đồng thời.
- `PUT /favorites/{productId}` và `DELETE /favorites/{productId}` là idempotent.
- Database có unique constraint `(user_id, product_id)` để chặn favorite trùng
  ngay cả khi có request đồng thời.
- JavaScript bắt các form mutation hiện hữu và gọi API. Form MVC được giữ làm
  progressive fallback cho tới giai đoạn dọn endpoint cũ.

## Quyết định kiến trúc giai đoạn 3

- Trạng thái đơn hàng được chuẩn hóa bằng `OrderStatus`; mã số cũ vẫn được giữ
  trong database để tương thích dữ liệu và template hiện tại.
- Tồn kho được giữ đúng một lần khi tạo đơn. Hoàn tất/giao hàng không trừ lần
  nữa; hủy đơn hợp lệ hoàn tồn kho đúng một lần.
- Checkout khóa người dùng và cập nhật tồn kho bằng câu lệnh nguyên tử trong
  transaction. Nếu một sản phẩm hết hoặc thay đổi, toàn bộ order/detail rollback
  và giỏ hàng được giữ nguyên.
- PayPal order bên ngoài được tạo trước, sau đó fingerprint giỏ hàng được kiểm
  tra lại trong transaction trước khi tạo đơn nội bộ.
- Callback `GET /pay/success` và `GET /pay/cancel` chỉ render trang xử lý, không
  đổi dữ liệu. Capture/hủy chỉ chạy qua `POST` có session, CSRF và kiểm tra chủ
  sở hữu.
- Capture PayPal idempotent; database có unique constraint cho external order ID
  và capture ID.
- Email xác nhận chỉ được gửi sau khi transaction commit; lỗi mail không rollback
  đơn đã tạo hoặc thanh toán đã hoàn tất.
- MVC fallback và REST controller dùng cùng `CheckoutService`/`OrderService`;
  không còn đường hủy đơn ghi thẳng trạng thái và bỏ qua hoàn kho.

## Quyết định kiến trúc giai đoạn 4

- API quản trị nằm dưới `/api/v1/admin/**`. User/role và báo cáo doanh số yêu
  cầu `ADMIN`; catalog và dashboard cho phép `ADMIN` hoặc `STAFF`.
- Method security được bật bằng `@EnableMethodSecurity`; `@PreAuthorize` giờ là
  lớp bảo vệ thứ hai bên cạnh URL rules của Spring Security.
- Cập nhật quyền khóa bản ghi user, yêu cầu ít nhất một role, từ chối role không
  tồn tại, chặn admin tự vô hiệu hóa/tự hạ quyền và bảo vệ admin hoạt động cuối
  cùng.
- Database mới không có mật khẩu ADMIN mặc định. Cơ chế bootstrap quản trị viên
  đầu tiên chỉ chạy khi được bật rõ ràng bằng biến môi trường, yêu cầu mật khẩu
  tối thiểu 12 ký tự và không ghi mật khẩu vào log.
- Phân trang user thực hiện hai bước: page ID tại database rồi fetch roles theo
  ID. Không phân trang collection trong bộ nhớ và không trả password hash.
- Category dùng multipart với part JSON `data` và part ảnh `file`; NXB dùng
  JSON. Validation trùng tên nằm trong application service và V6 thêm unique
  constraint tại database.
- Ẩn/hiện category hoặc NXB là soft state idempotent và đồng bộ trạng thái các
  sản phẩm con. Không xóa cứng catalog chỉ vì chưa có sản phẩm tham chiếu.
- Dashboard trả một snapshot nhất quán gồm summary và các series. Báo cáo chỉ
  tính đơn `COMPLETED`; đơn `CANCELLED`/`REFUNDED` không còn làm tăng doanh thu
  hoặc số lượng bán.
- Form admin hiện tại được JavaScript bridge sang API nhưng vẫn giữ POST MVC làm
  progressive fallback cho tới giai đoạn 5.
