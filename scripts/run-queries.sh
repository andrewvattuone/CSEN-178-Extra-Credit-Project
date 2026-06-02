#!/usr/bin/env zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR/src"

JDBC_JAR="../mysql-connector-j-9.7.0/mysql-connector-j-9.7.0.jar"

if [[ ! -f "$JDBC_JAR" ]]; then
  echo "Error: MySQL connector JAR not found at $JDBC_JAR"
  exit 1
fi

echo "Compiling javaqueries..."
javac -cp .:$JDBC_JAR javaqueries.java

echo "Running predefined queries..."
java -cp .:$JDBC_JAR javaqueries
