-- Baseline generated from the current JPA model.
-- Existing legacy schemas are marked at V1 by the dev profile; new schemas run it.

CREATE TABLE cart_items (
    cart_item_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    last_touch DATETIME(6),
    quantity INTEGER NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (cart_item_id)
) ENGINE=InnoDB;

CREATE TABLE carts (
    cart_id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6),
    status VARCHAR(255) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (cart_id)
) ENGINE=InnoDB;

CREATE TABLE categories (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    category_image VARCHAR(255),
    category_name VARCHAR(255),
    status BIT,
    PRIMARY KEY (category_id)
) ENGINE=InnoDB;

CREATE TABLE comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    content VARCHAR(255),
    rate_date DATE,
    rating DOUBLE PRECISION,
    order_detail_id BIGINT,
    product_id BIGINT,
    user_id BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE favorites (
    favorite_id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT,
    user_id BIGINT,
    PRIMARY KEY (favorite_id)
) ENGINE=InnoDB;

CREATE TABLE login_ip_lock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    failed_attempt INTEGER NOT NULL,
    ip VARCHAR(45) NOT NULL,
    last_failed_at DATETIME(6),
    locked_until DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE nxb (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    status BIT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE order_payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME(6),
    currency VARCHAR(3) NOT NULL,
    exchange_rate DECIMAL(18,6),
    external_capture_id VARCHAR(255),
    external_order_id VARCHAR(255),
    message VARCHAR(255),
    method VARCHAR(255),
    payer_email VARCHAR(255),
    payment_time DATETIME(6),
    provider VARCHAR(255) NOT NULL,
    refund_amount DECIMAL(12,2),
    refund_currency VARCHAR(3),
    refund_id VARCHAR(255),
    refunded_at DATETIME(6),
    status VARCHAR(255),
    updated_at DATETIME(6),
    order_id BIGINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE order_details (
    order_detail_id BIGINT NOT NULL AUTO_INCREMENT,
    price DOUBLE PRECISION,
    quantity INTEGER NOT NULL,
    order_id BIGINT,
    product_id BIGINT,
    PRIMARY KEY (order_detail_id)
) ENGINE=InnoDB;

CREATE TABLE orders (
    order_id BIGINT NOT NULL AUTO_INCREMENT,
    address VARCHAR(255),
    amount DOUBLE PRECISION,
    order_date DATE,
    paypal_capture_id VARCHAR(255),
    phone VARCHAR(255),
    status INTEGER NOT NULL,
    user_id BIGINT,
    PRIMARY KEY (order_id)
) ENGINE=InnoDB;

CREATE TABLE products (
    product_id BIGINT NOT NULL AUTO_INCREMENT,
    description VARCHAR(255),
    discount INTEGER NOT NULL,
    entered_date DATE,
    price DOUBLE PRECISION NOT NULL,
    product_image VARCHAR(255),
    product_name VARCHAR(255),
    quantity INTEGER NOT NULL,
    status BIT,
    category_id BIGINT,
    nxb_id BIGINT,
    PRIMARY KEY (product_id)
) ENGINE=InnoDB;

CREATE TABLE role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE user (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    avatar VARCHAR(255),
    email VARCHAR(254) NOT NULL,
    failed_attempt INTEGER NOT NULL,
    last_login_at DATETIME(6),
    locked_until DATETIME(6),
    name VARCHAR(50) NOT NULL,
    password VARCHAR(60) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    register_date DATE,
    status BIT NOT NULL,
    username VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB;

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
) ENGINE=InnoDB;

ALTER TABLE cart_items
    ADD CONSTRAINT ux_cart_product UNIQUE (cart_id, product_id);
CREATE INDEX idx_ip ON login_ip_lock (ip);
ALTER TABLE user ADD CONSTRAINT ux_user_email UNIQUE (email);
ALTER TABLE user ADD CONSTRAINT ux_user_username UNIQUE (username);

ALTER TABLE cart_items
    ADD CONSTRAINT FKpcttvuq4mxppo8sxggjtn5i2c
    FOREIGN KEY (cart_id) REFERENCES carts (cart_id);
ALTER TABLE cart_items
    ADD CONSTRAINT FK1re40cjegsfvw58xrkdp6bac6
    FOREIGN KEY (product_id) REFERENCES products (product_id);
ALTER TABLE carts
    ADD CONSTRAINT FKpay9408fi1tlnkqv3fhetr6hy
    FOREIGN KEY (user_id) REFERENCES user (user_id);
ALTER TABLE comments
    ADD CONSTRAINT FKfwepd0s8syqc9s06xnqa7mdwf
    FOREIGN KEY (order_detail_id) REFERENCES order_details (order_detail_id);
ALTER TABLE comments
    ADD CONSTRAINT FK6uv0qku8gsu6x1r2jkrtqwjtn
    FOREIGN KEY (product_id) REFERENCES products (product_id);
ALTER TABLE comments
    ADD CONSTRAINT FKqi14bvepnwtjbbaxm7m4v44yg
    FOREIGN KEY (user_id) REFERENCES user (user_id);
ALTER TABLE favorites
    ADD CONSTRAINT FK6sgu5npe8ug4o42bf9j71x20c
    FOREIGN KEY (product_id) REFERENCES products (product_id);
ALTER TABLE favorites
    ADD CONSTRAINT FK1uphh0glinnprjknyl68k1hw1
    FOREIGN KEY (user_id) REFERENCES user (user_id);
ALTER TABLE order_payments
    ADD CONSTRAINT FK3s9vxneb3dk3plhpv9s213so0
    FOREIGN KEY (order_id) REFERENCES orders (order_id);
ALTER TABLE order_details
    ADD CONSTRAINT FKjyu2qbqt8gnvno9oe9j2s2ldk
    FOREIGN KEY (order_id) REFERENCES orders (order_id);
ALTER TABLE order_details
    ADD CONSTRAINT FK4q98utpd73imf4yhttm3w0eax
    FOREIGN KEY (product_id) REFERENCES products (product_id);
ALTER TABLE orders
    ADD CONSTRAINT FKel9kyl84ego2otj2accfd8mr7
    FOREIGN KEY (user_id) REFERENCES user (user_id);
ALTER TABLE products
    ADD CONSTRAINT FKog2rp4qthbtt2lfyhfo32lsw9
    FOREIGN KEY (category_id) REFERENCES categories (category_id);
ALTER TABLE products
    ADD CONSTRAINT FKp94s74eab94uf5uvkw9maod14
    FOREIGN KEY (nxb_id) REFERENCES nxb (id);
ALTER TABLE users_roles
    ADD CONSTRAINT FKt4v0rrweyk393bdgt107vdx0x
    FOREIGN KEY (role_id) REFERENCES role (id);
ALTER TABLE users_roles
    ADD CONSTRAINT FKgd3iendaoyh04b95ykqise6qh
    FOREIGN KEY (user_id) REFERENCES user (user_id);
