CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(255) PRIMARY KEY,
    sku VARCHAR(255) NOT NULL,
    qty INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    version INT
);
