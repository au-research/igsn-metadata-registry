DROP SCHEMA `dbs_igsn`;
CREATE SCHEMA `dbs_igsn`;
DROP USER `igsn_user`@`localhost`;
CREATE USER `igsn_user`@`localhost` IDENTIFIED BY 'igsn_user';
GRANT ALL ON `dbs_igsn`.* TO `igsn_user`@`localhost`;