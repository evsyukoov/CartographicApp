use transform_bot;

CREATE TABLE IF Not Exists transform_bot.clients (id INT NOT NULL, count INT, PRIMARY KEY (id));
CREATE TABLE IF NOT Exists transform_bot.coordinate_systems (Type VARCHAR(45), Sk LONGTEXT, Param LONGTEXT, Zone VARCHAR(45));
GRANT ALL PRIVILEGES ON transform_bot.* TO 'admin'@'%' IDENTIFIED BY '1111' WITH GRANT OPTION;
FLUSH PRIVILEGES;