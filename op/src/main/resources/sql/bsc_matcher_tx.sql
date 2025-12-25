CREATE TABLE IF NOT EXISTS bsc_matcher_tx (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tx_hash VARCHAR(66) NOT NULL,
    block_number BIGINT NOT NULL,
    block_time DATETIME NOT NULL,
    date CHAR(4) NOT NULL,
    address VARCHAR(42) NOT NULL,
    side CHAR(4) NOT NULL,
    value_amount DECIMAL(38, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tx_hash_address_side (tx_hash, address, side),
    KEY idx_block_number (block_number),
    KEY idx_block_time (block_time),
    KEY idx_address (address),
    KEY idx_date (date)
);
