ALTER TABLE categories
    ADD CONSTRAINT ux_category_name UNIQUE (category_name);

ALTER TABLE nxb
    ADD CONSTRAINT ux_publisher_name UNIQUE (name);

ALTER TABLE role
    ADD CONSTRAINT ux_role_name UNIQUE (name);

CREATE INDEX idx_orders_status_date
    ON orders (status, order_date);

CREATE INDEX idx_user_status_register
    ON user (status, register_date);
