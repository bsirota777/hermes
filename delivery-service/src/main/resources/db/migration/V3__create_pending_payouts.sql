CREATE TABLE pending_payouts (
     id BIGSERIAL PRIMARY KEY,
     user_id BIGINT NOT NULL,
     amount NUMERIC(12,2) NOT NULL,
     type VARCHAR(20) NOT NULL,
     delivery_id BIGINT NOT NULL,
     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
     attempts INT NOT NULL DEFAULT 0,
     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
     last_attempt_at TIMESTAMPTZ
);