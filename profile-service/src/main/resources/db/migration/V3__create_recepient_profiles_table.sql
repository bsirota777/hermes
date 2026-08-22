CREATE TABLE recipient_profiles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  street_number VARCHAR(255) NOT NULL,
  street_name VARCHAR(255) NOT NULL,
  suburb VARCHAR(255) NOT NULL,
  state VARCHAR(255) NOT NULL,
  postcode VARCHAR(255) NOT NULL,
  phone_number VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
