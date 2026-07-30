ALTER TABLE orders
    ADD COLUMN note VARCHAR(255);

ALTER TABLE order_payments
    ADD CONSTRAINT ux_payment_provider_external_order
        UNIQUE (provider, external_order_id);

ALTER TABLE order_payments
    ADD CONSTRAINT ux_payment_provider_external_capture
        UNIQUE (provider, external_capture_id);

CREATE INDEX idx_orders_user_order
    ON orders (user_id, order_id);

CREATE INDEX idx_order_details_order
    ON order_details (order_id);
