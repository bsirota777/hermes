-- e.g. V12__add_delivery_coordinates.sql (adjust version to your next available)
ALTER TABLE deliveries
    ADD COLUMN pick_up_latitude DOUBLE PRECISION,
    ADD COLUMN pick_up_longitude DOUBLE PRECISION,
    ADD COLUMN drop_off_latitude DOUBLE PRECISION,
    ADD COLUMN drop_off_longitude DOUBLE PRECISION;