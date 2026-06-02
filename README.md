# AI Hardware Benchmark Tracker

This repository contains a small Java-based web application backed by MySQL.
The app serves a static frontend and provides CRUD/query APIs for a hardware benchmark dataset.

## Repository layout

- `index.html` — frontend page served by the Java server
- `schema.sql` — database schema creation script
- `seed.sql` — sample data load script
- `create_dbuser.sql` — creates the MySQL user used by the app
- `queries.sql` — example SQL queries for the benchmark dataset
- `mysql-connector-j-9.7.0/` — MySQL JDBC driver distribution
- `src/` — Java source code and compiled class files

## Prerequisites

- JDK 17 or later
- MySQL server

## Setup

1. Start MySQL:

```zsh
brew services start mysql
```

2. Create the database user and load the schema/data:

```zsh
cd "/Users/rileyheike/Documents/Academics/Spring 2026/CSEN 178/CSEN-178-Extra-Credit-Project"
mysql -u root -p < create_dbuser.sql
mysql -u root -p < schema.sql
mysql -u root -p < seed.sql
```

If the root account uses socket authentication, use:

```zsh
sudo mysql < create_dbuser.sql
sudo mysql < schema.sql
sudo mysql < seed.sql
```

3. Compile the Java sources:

```zsh
cd src
javac -cp .:../mysql-connector-j-9.7.0/mysql-connector-j-9.7.0.jar *.java
```

4. Run the server:

```zsh
cd src
java -cp .:../mysql-connector-j-9.7.0/mysql-connector-j-9.7.0.jar SimpleServer
```

5. Open the app:

```text
http://localhost:8080
```

## Notes

- The app currently uses hard-coded JDBC configuration in `src/DBConnection.java`:
  - URL: `jdbc:mysql://localhost:3306/DBAIHardwareBenchmark`
  - User: `dbproject`
  - Password: `dbproject123`
- `src/SimpleServer.java` serves static files from the repository root and exposes REST APIs under `/api/`.

## Recommended improvements

- Add a build tool like Maven or Gradle
- Externalize database configuration
- Remove compiled `.class` files from source control
- Add test coverage
