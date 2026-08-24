CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,

    status VARCHAR(255),

    sender_id BIGINT,
    recipient_id BIGINT,
    driver_id BIGINT,

    pick_up_street_number VARCHAR(255) NOT NULL,
    pick_up_street_name VARCHAR(255) NOT NULL,
    pick_up_suburb VARCHAR(255) NOT NULL,
    pick_up_state VARCHAR(255) NOT NULL,
    pick_up_postcode VARCHAR(255) NOT NULL,

    drop_off_street_number VARCHAR(255) NOT NULL,
    drop_off_street_name VARCHAR(255) NOT NULL,
    drop_off_suburb VARCHAR(255) NOT NULL,
    drop_off_state VARCHAR(255) NOT NULL,
    drop_off_postcode VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    delivery_fee NUMERIC(12, 2) NOT NULL,
    driver_commission_rate NUMERIC(5, 4) NOT NULL,

    pick_up_latitude DOUBLE PRECISION,
    pick_up_longitude DOUBLE PRECISION,
    drop_off_latitude DOUBLE PRECISION,
    drop_off_longitude DOUBLE PRECISION,

    qr_code_token VARCHAR(255) NOT NULL UNIQUE,

    version BIGINT
);