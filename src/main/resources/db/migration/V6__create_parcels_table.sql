CREATE TABLE parcels (
  id BIGSERIAL PRIMARY KEY,
  delivery_id BIGINT NOT NULL REFERENCES deliveries(id),
  length_cm NUMERIC(6,2) NOT NULL CHECK (length_cm > 0),
  width_cm NUMERIC(6,2) NOT NULL CHECK (width_cm > 0),
  height_cm NUMERIC(6,2) NOT NULL CHECK (height_cm > 0),
  weight_kg NUMERIC(5,2) NOT NULL CHECK (weight_kg > 0 AND weight_kg <= 10.00),
  declared_value NUMERIC(10,2) NOT NULL CHECK (declared_value >= 0),
  insured BOOLEAN NOT NULL DEFAULT FALSE,
  insured_value NUMERIC(10,2) CHECK (insured_value >= 0),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);