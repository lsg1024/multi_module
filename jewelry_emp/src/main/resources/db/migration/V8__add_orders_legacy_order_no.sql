ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS legacy_order_no VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_legacy_order_no
    ON orders (legacy_order_no);
