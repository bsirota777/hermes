CREATE TABLE sender_profiles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  address VARCHAR(255),
  phone_number VARCHAR(20),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_sender_profile_user FOREIGN KEY (user_id) REFERENCES users(id)
);