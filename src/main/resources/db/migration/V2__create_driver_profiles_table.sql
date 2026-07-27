CREATE TABLE driver_profiles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  address VARCHAR(255) NOT NULL,
  phone_number VARCHAR(20) NOT NULL,
  licence_number VARCHAR(50) NOT NULL,
  vehicle_plate VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_driver_profile_user FOREIGN KEY (user_id) REFERENCES users(id)
);