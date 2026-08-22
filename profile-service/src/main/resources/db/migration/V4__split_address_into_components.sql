-- V{next}__split_address_into_components.sql

TRUNCATE TABLE deliveries, sender_profiles, recipient_profiles, driver_profiles RESTART IDENTITY CASCADE;

ALTER TABLE sender_profiles
DROP COLUMN address,
    ADD COLUMN street_number VARCHAR(255) NOT NULL,
    ADD COLUMN street_name VARCHAR(255) NOT NULL,
    ADD COLUMN suburb VARCHAR(255) NOT NULL,
    ADD COLUMN state VARCHAR(255) NOT NULL,
    ADD COLUMN postcode VARCHAR(255) NOT NULL;

ALTER TABLE recipient_profiles
DROP COLUMN address,
    ADD COLUMN street_number VARCHAR(255) NOT NULL,
    ADD COLUMN street_name VARCHAR(255) NOT NULL,
    ADD COLUMN suburb VARCHAR(255) NOT NULL,
    ADD COLUMN state VARCHAR(255) NOT NULL,
    ADD COLUMN postcode VARCHAR(255) NOT NULL;

ALTER TABLE driver_profiles
DROP COLUMN address,
    ADD COLUMN street_number VARCHAR(255) NOT NULL,
    ADD COLUMN street_name VARCHAR(255) NOT NULL,
    ADD COLUMN suburb VARCHAR(255) NOT NULL,
    ADD COLUMN state VARCHAR(255) NOT NULL,
    ADD COLUMN postcode VARCHAR(255) NOT NULL;

ALTER TABLE deliveries
DROP COLUMN pick_up_address,
    DROP COLUMN drop_off_address,
    ADD COLUMN pick_up_street_number VARCHAR(255) NOT NULL,
    ADD COLUMN pick_up_street_name VARCHAR(255) NOT NULL,
    ADD COLUMN pick_up_suburb VARCHAR(255) NOT NULL,
    ADD COLUMN pick_up_state VARCHAR(255) NOT NULL,
    ADD COLUMN pick_up_postcode VARCHAR(255) NOT NULL,
    ADD COLUMN drop_off_street_number VARCHAR(255) NOT NULL,
    ADD COLUMN drop_off_street_name VARCHAR(255) NOT NULL,
    ADD COLUMN drop_off_suburb VARCHAR(255) NOT NULL,
    ADD COLUMN drop_off_state VARCHAR(255) NOT NULL,
    ADD COLUMN drop_off_postcode VARCHAR(255) NOT NULL;