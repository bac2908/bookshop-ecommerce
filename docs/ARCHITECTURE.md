# Kiến trúc TMDT Bookshop

## Luồng tổng quát

```text
Browser
  -> presentation (controller, validation HTTP, model/view)
  -> application (use case và nghiệp vụ)
  -> infrastructure (Spring Data, tích hợp bên ngoài)
  -> domain (entity và quy tắc cốt lõi)
  -> MySQL / PayPal / Email

Thymeleaf view
  <- templates/{storefront|admin}/pages
  <- templates/{storefront|admin}/fragments
  <- static/{storefront|admin-assets}
```

URL công khai không phụ thuộc vào vị trí file. Việc đổi tên package, template hoặc
asset không được làm thay đổi endpoint nếu không có yêu cầu nghiệp vụ riêng.

## Backend

Mã backend nằm trong `src/main/java/vn/fs/backend`:

```text
backend/
├── presentation/
│   ├── storefront/    Controller phía khách hàng
│   ├── admin/         Controller và API quản trị
│   └── support/       Model dùng chung cho Thymeleaf
├── application/
│   ├── service/       Use case, transaction và tích hợp nghiệp vụ
│   └── dto/           Dữ liệu đi qua biên application/presentation
├── domain/
│   └── model/         JPA entity và kiểu dữ liệu nghiệp vụ
├── infrastructure/
│   └── persistence/   Spring Data repository
├── security/          Filter và handler xác thực
├── config/            Cấu hình Spring
└── shared/
    └── util/          Tiện ích không chứa nghiệp vụ
```

Quy tắc phụ thuộc:

1. `domain` không phụ thuộc `presentation`, `application` hoặc `infrastructure`.
2. `application` không phụ thuộc `presentation`.
3. Controller chỉ xử lý HTTP/model rồi chuyển công việc nghiệp vụ sang service.
4. Transaction và thay đổi dữ liệu đặt trong application service.
5. Không tạo package chung mơ hồ như `common`, `helper` hoặc `misc`. Đặt tên theo
   trách nhiệm cụ thể.

Một số controller legacy vẫn còn truy vấn repository trực tiếp. Không thêm trường
hợp mới; khi sửa một luồng, chuyển truy vấn đó vào application service tương ứng.

## Frontend

```text
templates/
├── storefront/
│   ├── pages/         Trang cho khách hàng
│   └── fragments/     Header/footer dùng chung
├── admin/
│   ├── pages/         Trang quản trị
│   └── fragments/     Header/navbar/footer quản trị
└── email/             Template email

static/
├── storefront/        CSS, JS, font, ảnh và vendor của storefront
└── admin-assets/      Theme và plugin chỉ dùng cho admin
```

Quy ước:

- Tên template dùng `kebab-case`, ví dụ `product-detail.html`.
- View name trong controller phải phản ánh đúng đường dẫn template, ví dụ
  `storefront/pages/product-detail`.
- Asset storefront dùng URL `/storefront/**`.
- Asset admin dùng URL `/admin-assets/**`.
- Component dùng chung đặt trong `fragments`; không sao chép header/footer vào page.
- Không đặt JavaScript nghiệp vụ trực tiếp vào nhiều page. Tách file theo khu vực
  trong static khi chỉnh sửa luồng đó.

## Cách lần theo lỗi

Ví dụ lỗi trang chi tiết sản phẩm:

1. Tìm endpoint trong `presentation/storefront/ProductDetailController`.
2. Theo service được controller gọi trong `application/service`.
3. Theo repository trong `infrastructure/persistence`.
4. Kiểm tra entity trong `domain/model`.
5. Kiểm tra view `templates/storefront/pages/product-detail.html`.
6. Kiểm tra asset dưới `static/storefront`.

Lỗi admin dùng cùng cách, bắt đầu từ `presentation/admin` và kết thúc ở
`templates/admin/pages`.
