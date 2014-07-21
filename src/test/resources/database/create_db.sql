-- test database schema

DROP TABLE IF EXISTS person, patient, datetimes;

CREATE TABLE person (
  id INT NOT NULL,
  firstname varchar(255) default NULL,
  lastname varchar(255) default NULL,
  age varchar(50) default NULL,
  postalcode varchar(10) default NULL,
  PRIMARY KEY (id)
);

CREATE TABLE patient (
  id INT NOT NULL,
  name varchar(255) default NULL,
  PRIMARY KEY (id)
);

CREATE TABLE datetimes (
  id INT NOT NULL,
  moment TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE dummy_table_with_long_name_for_gui_testing_xoxoxoxoxoxoxoxoxoxoxoxoxoxoxoxoxoxoxo (
  id_of_very_long_named_table_dummy_iaiaiaiaiaiaiaiaiaiaiaiaiaiaiaiaiaiaiaia INT NOT NULL,
  column_with_long_but_reasonable_name VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY(id_of_very_long_named_table_dummy_iaiaiaiaiaiaiaiaiaiaiaiaiaiaiaiaiaiaiaia)
);

CREATE TABLE urls (
  id INT NOT NULL,
  link VARCHAR(255),
  comment VARCHAR(255),
  PRIMARY KEY (id)
);
