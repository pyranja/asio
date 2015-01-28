-- Test database and simulated root user for user segregation tests
-- ! MySql specific !

DROP DATABASE IF EXISTS `test_authorization`;
CREATE DATABASE `test_authorization`;

GRANT ALL PRIVILEGES ON `test_authorization`.* TO `authorizer`@`localhost` IDENTIFIED BY 'authorizer' WITH GRANT OPTION;
GRANT CREATE USER ON *.* TO `authorizer`@`localhost`;
GRANT SELECT, INSERT, DELETE, UPDATE ON `mysql`.* TO `authorizer`@`localhost`;
