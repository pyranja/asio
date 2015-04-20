--- single table for schema fetching
DROP ALL OBJECTS;

CREATE SCHEMA IF NOT EXISTS test;
SET SCHEMA test;

CREATE TABLE sample (
  id INT PRIMARY KEY NOT NULL,
  data VARCHAR(255)
);
