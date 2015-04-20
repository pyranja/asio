-- gui test reference data

DROP TABLE IF EXISTS urls, dummy_table_with_long_name_for_gui_testing_xoxoxoxoxoxoxoxoxoxox;

-- SCHEMA

CREATE TABLE dummy_table_with_long_name_for_gui_testing_xoxoxoxoxoxoxoxoxoxox (
  id_of_very_long_named_table_dummy_iaiaiaiaiaiaiaiaiaiaiaiaiaiaia INT NOT NULL,
  column_with_long_but_reasonable_name VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY(id_of_very_long_named_table_dummy_iaiaiaiaiaiaiaiaiaiaiaiaiaiaia)
);

CREATE TABLE urls (
  id INT NOT NULL,
  link VARCHAR(255),
  comment VARCHAR(255),
  PRIMARY KEY (id)
);

-- DATA

INSERT INTO urls (id, link, comment) VALUES (0, 'http://example.com', 'a valid link');
INSERT INTO urls (id, link, comment) VALUES (1, 'https://example.com', 'a valid ssl link');
INSERT INTO urls (id, link, comment) VALUES (2, null, 'a null value');
INSERT INTO urls (id, link, comment) VALUES (3, 'this is not an url', 'random text');
INSERT INTO urls (id, link, comment) VALUES (4, 'httpillegal', 'starts like a real URL');
INSERT INTO urls (id, link, comment) VALUES (5, 'mailto:test@example.com', 'a send mail link');
INSERT INTO urls (id, link, comment) VALUES (6, 'file://test.txt', 'a file link');
INSERT INTO urls (id, link, comment) VALUES (7, 'http://example.com?val=test me now', 'URL with non-encoded whitespace');
INSERT INTO urls (id, link, comment) VALUES (8, 'http://example.com?val=test%20me%20now', 'URL with encoded whitespace');
INSERT INTO urls (id, link, comment) VALUES (9, '<a href="http://example.com">link</a>', 'a html link tag');
INSERT INTO urls (id, link, comment) VALUES (10, '<script>alert("XSS INJECTION!");</script>', 'script injection attempt');

INSERT INTO dummy_table_with_long_name_for_gui_testing_xoxoxoxoxoxoxoxoxoxox (id_of_very_long_named_table_dummy_iaiaiaiaiaiaiaiaiaiaiaiaiaiaia, column_with_long_but_reasonable_name) VALUES (1, 'long_test-value_hahahahahahahahahahahahahahahahahahahahahahahahahahahahaahahahahahaha');
