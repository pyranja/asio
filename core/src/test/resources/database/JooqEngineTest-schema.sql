--- table with a column of each major datatype, except binary and temporal
DROP ALL OBJECTS;

--- ############################################################################################ ---
--- edge cases for each major datatype, except binary and temporal
CREATE TABLE test (
  id          INT PRIMARY KEY NOT NULL,
  expect      VARCHAR(255)    NOT NULL,
  col_boolean BOOLEAN,
  col_string  VARCHAR(255),
  col_long    INTEGER,
  col_decimal DECIMAL,
  col_double  DOUBLE
);

INSERT INTO test (id, expect, col_boolean, col_string, col_long, col_decimal, col_double)
VALUES (0, 'default', TRUE, 'default', 0, 0, 0)
  , (1, 'null', NULL, NULL, NULL, NULL, NULL)
  , (2, 'negative', FALSE, 'negative', -1, -1, -1)
  , (3, 'positive', TRUE, 'positive', 1, 1, 1)
  , (4, 'common', TRUE, 'common', 123456789, 123.456789, 123.456789);

--- ############################################################################################ ---
--- empty table for update
CREATE TABLE updates (
  id   INT NOT NULL,
  data VARCHAR(255)
);
--- ############################################################################################ ---
