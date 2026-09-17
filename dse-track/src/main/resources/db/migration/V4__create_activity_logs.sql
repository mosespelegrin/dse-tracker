CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(30) NOT NULL,
    user_id BIGINT,
    email VARCHAR(100),
    detail VARCHAR(255),
    created_at DATETIME NOT NULL
);

CREATE INDEX idx_activity_logs_created_at ON activity_logs (created_at);
CREATE INDEX idx_activity_logs_event_type ON activity_logs (event_type);
