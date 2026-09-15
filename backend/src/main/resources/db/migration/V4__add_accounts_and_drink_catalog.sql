ALTER TABLE members
    ADD COLUMN password_hash VARCHAR(100) NULL,
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    ADD COLUMN reset_code_hash VARCHAR(64) NULL,
    ADD COLUMN reset_code_expires_at DATETIME(6) NULL;

CREATE TABLE drink_catalog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    price BIGINT NOT NULL,
    icon VARCHAR(20) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_drink_catalog_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_auth_sessions_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    UNIQUE KEY uk_auth_sessions_token (token_hash),
    KEY ix_auth_sessions_member (member_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO drink_catalog (name, price, icon, active, created_at, updated_at) VALUES
    ('Tra da', 5000, '🥃', TRUE, NOW(6), NOW(6)),
    ('Tra tac', 10000, '🍋', TRUE, NOW(6), NOW(6)),
    ('Ca phe sua', 20000, '☕', TRUE, NOW(6), NOW(6)),
    ('Nuoc suoi', 8000, '💧', TRUE, NOW(6), NOW(6)),
    ('Bac xiu', 22000, '🥛', TRUE, NOW(6), NOW(6));
