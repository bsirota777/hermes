ALTER TABLE wallets ADD COLUMN stripe_account_id VARCHAR(255);
ALTER TABLE wallets ADD COLUMN stripe_payouts_enabled BOOLEAN NOT NULL DEFAULT false;
