--- table with a column of each major datatype, except binary and temporal

DROP TABLE IF EXISTS test;

CREATE TABLE test (
  id INT PRIMARY KEY NOT NULL,
  expect VARCHAR(255) NOT NULL,
  col_boolean BOOLEAN,
  col_string VARCHAR(255),
  col_long INTEGER,
  col_decimal DECIMAL,
  col_double DOUBLE
);

DROP TABLE IF EXISTS updates;

CREATE TABLE updates (
  id INT NOT NULL,
  data VARCHAR(255)
);
