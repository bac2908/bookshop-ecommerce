INSERT INTO categories (category_image, category_name, status)
SELECT 'vanhoc.jpg', 'Văn học', 1
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_name = 'Văn học'
);

INSERT INTO categories (category_image, category_name, status)
SELECT 'tieuthuyet.jpg', 'Tiểu thuyết', 1
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_name = 'Tiểu thuyết'
);

INSERT INTO categories (category_image, category_name, status)
SELECT 'trinhtham.jpg', 'Trinh thám', 1
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_name = 'Trinh thám'
);

INSERT INTO categories (category_image, category_name, status)
SELECT 'khoahoc.jpg', 'Khoa học', 1
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_name = 'Khoa học'
);

INSERT INTO categories (category_image, category_name, status)
SELECT 'chinhtri.jpg', 'Chính trị - pháp luật', 1
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_name = 'Chính trị - pháp luật'
);

INSERT INTO categories (category_image, category_name, status)
SELECT 'xahoi.jpg', 'Xã hội - lịch sử', 1
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_name = 'Xã hội - lịch sử'
);

INSERT INTO nxb (name, status)
SELECT 'NXB Trẻ', 1
WHERE NOT EXISTS (
    SELECT 1 FROM nxb WHERE name = 'NXB Trẻ'
);

INSERT INTO nxb (name, status)
SELECT 'NXB Kim Đồng', 1
WHERE NOT EXISTS (
    SELECT 1 FROM nxb WHERE name = 'NXB Kim Đồng'
);

INSERT INTO nxb (name, status)
SELECT 'NXB Văn Học', 1
WHERE NOT EXISTS (
    SELECT 1 FROM nxb WHERE name = 'NXB Văn Học'
);

INSERT INTO nxb (name, status)
SELECT 'NXB Hội Nhà Văn', 1
WHERE NOT EXISTS (
    SELECT 1 FROM nxb WHERE name = 'NXB Hội Nhà Văn'
);

INSERT INTO nxb (name, status)
SELECT 'NXB Thế Giới', 1
WHERE NOT EXISTS (
    SELECT 1 FROM nxb WHERE name = 'NXB Thế Giới'
);

INSERT INTO nxb (name, status)
SELECT 'NXB Lao Động', 1
WHERE NOT EXISTS (
    SELECT 1 FROM nxb WHERE name = 'NXB Lao Động'
);

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Một câu chuyện giàu cảm xúc về con người, gia đình và những lựa chọn trong cuộc sống.',
    10, CURRENT_DATE, 85000, 'bo-cau-chung-mai-vom.jpg',
    'Bồ Câu Chung Mái Vòm', 100, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Trẻ'
WHERE c.category_name = 'Văn học'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Bồ Câu Chung Mái Vòm'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Tác phẩm văn học hiện đại với những lát cắt chân thực và sâu sắc về cuộc sống.',
    5, CURRENT_DATE, 99000, 'canh-dong-bat-tan.jpg',
    'Cánh Đồng Bất Tận', 80, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Trẻ'
WHERE c.category_name = 'Văn học'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Cánh Đồng Bất Tận'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Câu chuyện trong trẻo về tuổi thơ, nghịch cảnh và sức mạnh của tình yêu thương.',
    15, CURRENT_DATE, 120000, 'cay-cam-ngot-cua-toi.jpg',
    'Cây Cam Ngọt Của Tôi', 70, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Văn Học'
WHERE c.category_name = 'Tiểu thuyết'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Cây Cam Ngọt Của Tôi'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Một hành trình khám phá bản thân, vượt qua nỗi sợ và tìm lại niềm hy vọng.',
    10, CURRENT_DATE, 75000, 'chay-tron-mat-troi.jpg',
    'Chạy Trốn Mặt Trời', 60, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Kim Đồng'
WHERE c.category_name = 'Tiểu thuyết'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Chạy Trốn Mặt Trời'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Tuyển tập truyện ngắn kinh điển về lòng nhân ái, tình bạn và sự hy sinh.',
    0, CURRENT_DATE, 67000, 'chiec-la-cuoi-cung.jpg',
    'Chiếc Lá Cuối Cùng', 90, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Văn Học'
WHERE c.category_name = 'Văn học'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Chiếc Lá Cuối Cùng'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Chuyến tàu trở về tuổi thơ qua những trò chơi và suy nghĩ hồn nhiên của trẻ nhỏ.',
    20, CURRENT_DATE, 88000, 'cho-toi-mot-ve-ve-tuoi-tho.jpg',
    'Cho Tôi Một Vé Đi Tuổi Thơ', 100, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Trẻ'
WHERE c.category_name = 'Văn học'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Cho Tôi Một Vé Đi Tuổi Thơ'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Bức tranh sinh động về thiên nhiên và con người vùng đất phương Nam.',
    10, CURRENT_DATE, 125000, 'dat-rung-phuong-nam.jpg',
    'Đất Rừng Phương Nam', 65, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Kim Đồng'
WHERE c.category_name = 'Xã hội - lịch sử'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Đất Rừng Phương Nam'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Những câu chuyện bí ẩn kết nối quá khứ và hiện tại tại một tiệm tạp hóa đặc biệt.',
    15, CURRENT_DATE, 110000, 'dieu-ki-dieu-cua-tiem-tap-hoa.jpg',
    'Điều Kỳ Diệu Của Tiệm Tạp Hóa', 75, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Hội Nhà Văn'
WHERE c.category_name = 'Tiểu thuyết'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Điều Kỳ Diệu Của Tiệm Tạp Hóa'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Hành trình theo đuổi ước mơ và lắng nghe tiếng gọi sâu thẳm của trái tim.',
    10, CURRENT_DATE, 89000, 'nha-gia-kim.jpg',
    'Nhà Giả Kim', 100, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Hội Nhà Văn'
WHERE c.category_name = 'Tiểu thuyết'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Nhà Giả Kim'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Tiểu thuyết trào phúng đặc sắc phản ánh xã hội Việt Nam đầu thế kỷ hai mươi.',
    15, CURRENT_DATE, 96000, 'so-do.jpg',
    'Số Đỏ', 85, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Văn Học'
WHERE c.category_name = 'Văn học'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Số Đỏ'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Tác phẩm hiện thực kinh điển về cuộc sống nông thôn Việt Nam trước Cách mạng.',
    10, CURRENT_DATE, 87000, 'tat-den.jpg',
    'Tắt Đèn', 70, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Văn Học'
WHERE c.category_name = 'Xã hội - lịch sử'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Tắt Đèn'
  );

INSERT INTO products (
    description, discount, entered_date, price, product_image,
    product_name, quantity, status, category_id, nxb_id
)
SELECT
    'Những trang viết nhẹ nhàng, trong sáng về kỷ niệm ngày đầu tiên đến trường.',
    5, CURRENT_DATE, 65000, 'toi-di-hoc.jpg',
    'Tôi Đi Học', 100, 1, c.category_id, n.id
FROM categories c
JOIN nxb n ON n.name = 'NXB Kim Đồng'
WHERE c.category_name = 'Văn học'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_name = 'Tôi Đi Học'
  );
