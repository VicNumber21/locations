CREATE DATABASE locations WITH ENCODING 'UTF8';

\connect locations

CREATE TABLE IF NOT EXISTS locations (
  location_id VARCHAR(256) PRIMARY KEY,
  location_longitude NUMERIC(9, 6) NOT NULL,
  location_latitude NUMERIC(8, 6) NOT NULL,
  location_created TIMESTAMP NOT NULL DEFAULT current_timestamp
);

CREATE USER locator WITH PASSWORD 'locator';
GRANT connect ON DATABASE locations TO locator;
GRANT select, insert, update, delete ON TABLE locations to locator;