-- test table : urls

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
