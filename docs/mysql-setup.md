# MySQL Setup for Animal Records

This project can load records from MySQL at startup.

Config sources (priority order):
1. Environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`)
2. Local `db.properties` file in the project root

## 1) Create database and table

```sql
CREATE DATABASE IF NOT EXISTS animal_db;
USE animal_db;

CREATE TABLE IF NOT EXISTS animals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    details VARCHAR(120) NULL
);

INSERT INTO animals (name, age, type, details) VALUES
('Buddy', 5, 'dog', 'Golden Retriever'),
('Whiskers', 3, 'cat', 'Black'),
('Max', 7, 'dog', 'German Shepherd'),
('Luna', 2, 'cat', 'White');
```

## 2) Configure credentials

PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/animal_db"
$env:DB_USER="root"
$env:DB_PASSWORD="your_password"
```

Or edit `db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/animal_db
db.user=root
db.password=your_password
```

## 3) Add MySQL JDBC driver

Place MySQL Connector/J JAR in `lib/` and run:

```powershell
javac -cp ".;lib/*" *.java
java -cp ".;lib/*" Main
```

If DB config is missing or table is empty, the app falls back to in-memory sample records.
