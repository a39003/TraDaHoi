CREATE TABLE members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NULL,
    bank_name VARCHAR(100) NULL,
    bank_bin VARCHAR(20) NULL,
    account_number VARCHAR(50) NULL,
    account_name VARCHAR(150) NULL,
    qr_code_url VARCHAR(500) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE drink_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_date DATE NOT NULL,
    payer_member_id BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_payer FOREIGN KEY (payer_member_id) REFERENCES members(id),
    KEY ix_orders_date (order_date),
    KEY ix_orders_payer (payer_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    consumer_member_id BIGINT NOT NULL,
    drink_name VARCHAR(150) NOT NULL,
    unit_price BIGINT NOT NULL,
    quantity INT NOT NULL,
    line_total BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES drink_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_items_consumer FOREIGN KEY (consumer_member_id) REFERENCES members(id),
    KEY ix_items_order (order_id),
    KEY ix_items_consumer (consumer_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE weekly_settlements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    week_start DATE NOT NULL,
    week_end DATE NOT NULL,
    total_amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    finalized_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlement_week_start (week_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE settlement_balances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    settlement_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    consumed_amount BIGINT NOT NULL,
    paid_amount BIGINT NOT NULL,
    net_amount BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_balances_settlement FOREIGN KEY (settlement_id) REFERENCES weekly_settlements(id) ON DELETE CASCADE,
    CONSTRAINT fk_balances_member FOREIGN KEY (member_id) REFERENCES members(id),
    UNIQUE KEY uk_balance_settlement_member (settlement_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE settlement_transfers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    settlement_id BIGINT NOT NULL,
    debtor_member_id BIGINT NOT NULL,
    creditor_member_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transfers_settlement FOREIGN KEY (settlement_id) REFERENCES weekly_settlements(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfers_debtor FOREIGN KEY (debtor_member_id) REFERENCES members(id),
    CONSTRAINT fk_transfers_creditor FOREIGN KEY (creditor_member_id) REFERENCES members(id),
    KEY ix_transfers_settlement (settlement_id),
    KEY ix_transfers_debtor (debtor_member_id),
    KEY ix_transfers_creditor (creditor_member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_member_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_sender FOREIGN KEY (sender_member_id) REFERENCES members(id),
    KEY ix_chat_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_member_id BIGINT NOT NULL,
    settlement_id BIGINT NULL,
    transfer_id BIGINT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_member FOREIGN KEY (recipient_member_id) REFERENCES members(id),
    CONSTRAINT fk_notifications_settlement FOREIGN KEY (settlement_id) REFERENCES weekly_settlements(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_transfer FOREIGN KEY (transfer_id) REFERENCES settlement_transfers(id) ON DELETE SET NULL,
    KEY ix_notifications_recipient (recipient_member_id, read_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
