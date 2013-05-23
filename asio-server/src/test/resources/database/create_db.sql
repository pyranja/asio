-- test database schema

CREATE TABLE person (
  id INT NOT NULL,
  firstname varchar(255) default NULL,
  lastname varchar(255) default NULL,
  age varchar(50) default NULL,
  postalcode varchar(10) default NULL,
  PRIMARY KEY (id)
);