# MySQL Setup for Animal Observation Records

This project stores one observation per row and can load records from MySQL at startup.

Config sources (priority order):
1. Environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`)
2. Local `db.properties` file in the project root

## 1) Create database and table

```sql
CREATE DATABASE IF NOT EXISTS animal_db;
USE animal_db;

CREATE TABLE IF NOT EXISTS animals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    animal_type VARCHAR(80) NOT NULL,
    latitude FLOAT NOT NULL,
    longitude FLOAT NOT NULL,
    observed_at DATETIME NOT NULL,
    duration_minutes INT NOT NULL,
    observation_uuid CHAR(36) NOT NULL UNIQUE,
    revisited BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO animals (
    animal_type,
    latitude,
    longitude,
    observed_at,
    duration_minutes,
    observation_uuid,
    revisited
) VALUES
('Dog', 40.7128, -74.0060, '2026-04-22 07:15:00', 45, UUID(), TRUE),
('Cat', 40.7132, -74.0051, '2026-04-22 09:40:00', 30, UUID(), FALSE),
('Dog', 40.7128, -74.0060, '2026-04-22 16:20:00', 20, UUID(), TRUE);
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

## Field Meaning

- `animal_type`: species/type label (`VARCHAR`)
- `latitude` / `longitude`: observation coordinates (`FLOAT`)
- `observed_at`: observation timestamp (`DATETIME`)
- `duration_minutes`: stay duration in minutes (`INT`)
- `observation_uuid`: unique observation identifier (`CHAR(36)` UUID text)
- `revisited`: whether this spot was revisited (`BOOLEAN`)
