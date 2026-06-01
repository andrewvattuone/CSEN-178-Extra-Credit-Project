CREATE USER IF NOT EXISTS 'dbproject'@'localhost'
IDENTIFIED BY 'dbproject123';

GRANT ALL PRIVILEGES
ON DBAIHardwareBenchmark.*
TO 'dbproject'@'localhost';

FLUSH PRIVILEGES;