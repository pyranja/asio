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