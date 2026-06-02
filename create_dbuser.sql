-- create_dbuser.sql
--
-- Creates the application database user for the AI Hardware Benchmark Tracker.
-- This user is expected to connect from localhost and access the DBAIHardwareBenchmark schema.

CREATE USER IF NOT EXISTS 'dbproject'@'localhost'
IDENTIFIED BY 'dbproject123';

GRANT ALL PRIVILEGES
ON DBAIHardwareBenchmark.*
TO 'dbproject'@'localhost';

FLUSH PRIVILEGES;