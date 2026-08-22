CREATE TABLE deliveries (
  id BIGSERIAL PRIMARY KEY,
  status VARCHAR(50) NOT NULL,

  sender_profile_id BIGINT NOT NULL,
  recipient_profile_id BIGINT NOT NULL,
  driver_profile_id BIGINT,
  pick_up_address VARCHAR(255) NOT NULL,
  drop_off_address VARCHAR(255) NOT NULL,

  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
);