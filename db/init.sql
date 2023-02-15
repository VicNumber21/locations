CREATE DATABASE locations WITH ENCODING 'UTF8';

\connect locations

CREATE TABLE IF NOT EXISTS locations (
  location_id VARCHAR PRIMARY KEY CHECK(length(location_id) > 0 AND length(location_id) < 256 AND location_id ~ '^[a-zA-Z0-9]+$'),
  location_longitude NUMERIC(9, 6) NOT NULL CHECK(-180 <= location_longitude AND location_longitude <= 180),
  location_latitude NUMERIC(8, 6) NOT NULL CHECK(-90 <= location_latitude AND location_latitude <= 90),
  location_created TIMESTAMP NOT NULL DEFAULT current_timestamp
);

CREATE USER locator WITH PASSWORD 'locator';
GRANT CONNECT ON DATABASE locations TO locator;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE locations TO locator;
