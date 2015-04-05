-- Remove test database and users.
-- ! MySql specific !

DROP DATABASE IF EXISTS `test_authorization`;

-- delete all created users (prefix is hardcoded!)
SET @users = NULL;
SELECT GROUP_CONCAT('\'', `user`, '\'@\'', `host`, '\'') INTO @users
FROM `mysql`.`user` WHERE `user` LIKE '_:%' OR `user` = 'authorizer';

SET @drop_command = CONCAT('DROP USER ', @users);
PREPARE drop_statement FROM @drop_command;
EXECUTE drop_statement;
DEALLOCATE PREPARE drop_statement;
