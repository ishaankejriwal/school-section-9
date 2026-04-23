# Animal Observation Tracker (Java + Swing + MySQL)

A desktop Java application for tracking where animals repeatedly settle during the day:
- Swing UI for browsing and CRUD operations on observation records
- Optional MySQL persistence
- Automatic fallback to in-memory sample observations

## Project Structure

```text
cmdr/
├─ Animal.java
├─ Cat.java
├─ Dog.java
├─ Creature.java
├─ AnimalContainer.java
├─ MySQLAnimalRepository.java
├─ AnimalGUI.java
├─ Main.java
├─ db.properties.example
├─ docs/
│  └─ mysql-setup.md
├─ lib/
├─ .gitignore
└─ README.md
```

## Naming Conventions Used

- Java classes/interfaces: `PascalCase` (already applied)
- Methods and fields: `camelCase` (already applied)
- Constants: `UPPER_SNAKE_CASE` (already applied)
- Documentation files: `kebab-case` in `docs/`
- Sensitive local config is ignored from Git

## Prerequisites

- Java 17+ (or compatible JDK)
- MySQL server (optional)
- MySQL Connector/J JAR in `lib/`

## Run Locally

Compile:

```powershell
javac -cp ".;lib/*" *.java
```

Run:

```powershell
java -cp ".;lib/*" Main
```

## Observation Data Model

Each row represents one observation and includes:
- `animal_type` (`VARCHAR`)
- `latitude` (`FLOAT`)
- `longitude` (`FLOAT`)
- `observed_at` (`DATETIME`)
- `duration_minutes` (`INT`)
- `observation_uuid` (`UUID` stored as `CHAR(36)`)
- `revisited` (`BOOLEAN`)

## Database Configuration

1. Copy `db.properties.example` to `db.properties`
2. Add your real credentials
3. Follow the setup guide in `docs/mysql-setup.md`

If MySQL credentials are missing/unavailable, the app still runs with sample in-memory observation records.

## GitHub Readiness Notes

- Build artifacts (`*.class`) are excluded via `.gitignore`
- Local secrets (`db.properties`) are excluded via `.gitignore`
- Setup documentation has been standardized and moved under `docs/`
