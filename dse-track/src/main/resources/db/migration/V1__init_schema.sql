-- Baseline schema, matching the @Entity classes as of this migration.
-- Safe to run against a FRESH database (creates everything). Against an
-- EXISTING dev database created by ddl-auto=update, this is skipped —
-- see spring.flyway.baseline-on-migrate in application.properties.

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS stocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL UNIQUE,
    company_name VARCHAR(150) NOT NULL,
    current_price DECIMAL(15,2),
    last_updated DATETIME,
    sector VARCHAR(100),
    eps_ttm DECIMAL(15,4),
    shares_outstanding BIGINT,
    net_income DECIMAL(18,2),
    total_equity DECIMAL(18,2),
    total_assets DECIMAL(18,2),
    total_liabilities DECIMAL(18,2),
    current_assets DECIMAL(18,2),
    current_liabilities DECIMAL(18,2),
    annual_dividend_per_share DECIMAL(15,4),
    fundamentals_updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS holdings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    shares INT NOT NULL,
    total_paid DECIMAL(15,2) NOT NULL,
    UNIQUE KEY uq_holdings_user_stock (user_id, stock_id),
    CONSTRAINT fk_holdings_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_holdings_stock FOREIGN KEY (stock_id) REFERENCES stocks(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL,
    shares INT NOT NULL,
    total_paid DECIMAL(15,2) NOT NULL,
    sell_price DECIMAL(15,2),
    cost_basis DECIMAL(15,2),
    date DATE NOT NULL,
    notes VARCHAR(255),
    created_at DATETIME,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_transactions_stock FOREIGN KEY (stock_id) REFERENCES stocks(id)
);

CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    condition_type VARCHAR(10) NOT NULL,
    target_price DECIMAL(15,2) NOT NULL,
    triggered BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_at DATETIME,
    created_at DATETIME,
    CONSTRAINT fk_alerts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_alerts_stock FOREIGN KEY (stock_id) REFERENCES stocks(id)
);

CREATE TABLE IF NOT EXISTS dividends (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    amount_per_share DECIMAL(15,4) NOT NULL,
    shares INT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    notes VARCHAR(255),
    created_at DATETIME,
    CONSTRAINT fk_dividends_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_dividends_stock FOREIGN KEY (stock_id) REFERENCES stocks(id)
);

CREATE TABLE IF NOT EXISTS auth_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME,
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
