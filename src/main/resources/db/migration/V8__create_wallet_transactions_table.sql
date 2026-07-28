CREATE TABLE wallet_transactions (
  id BIGSERIAL PRIMARY KEY,
  wallet_id BIGINT NOT NULL REFERENCES wallets(id),
  amount NUMERIC(12, 2) NOT NULL,
  type VARCHAR(20) NOT NULL,
  related_delivery_id BIGINT REFERENCES deliveries(id),
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);