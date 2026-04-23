/*
 * Ishaan Kejriwal - AP CSA
 * File: MySQLAnimalRepository.java
 * Description: MySQL data access layer for animal records.
 * Date: 2026-03-31
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class MySQLAnimalRepository {
    // JDBC connection URL.
    private final String dbUrl;
    // Database username.
    private final String dbUser;
    // Database password.
    private final String dbPassword;

    // Creates a repository bound to a single JDBC configuration.
    public MySQLAnimalRepository(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    // Builds a repository from environment variables, with db.properties as fallback.
    public static MySQLAnimalRepository fromEnvironment() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (isBlank(url) || isBlank(user) || isBlank(password)) {
            Properties fileProps = loadPropertiesFile("db.properties");
            if (fileProps != null) {
                if (isBlank(url)) {
                    url = fileProps.getProperty("db.url");
                }
                if (isBlank(user)) {
                    user = fileProps.getProperty("db.user");
                }
                if (isBlank(password)) {
                    password = fileProps.getProperty("db.password");
                }
            }
        }

        if (isBlank(url) || isBlank(user) || isBlank(password)) {
            return null;
        }

        return new MySQLAnimalRepository(url.trim(), user.trim(), password.trim());
    }

    // Reads all animal rows from MySQL and maps them into domain objects.
    public List<Animal> fetchAllAnimals() throws SQLException {
        List<Animal> animals = new ArrayList<>();
        String sql = "SELECT id, animal_type, latitude, longitude, observed_at, duration_minutes, observation_uuid, revisited "
            + "FROM animals ORDER BY observed_at ASC, id ASC";

        ensureMySqlDriverLoaded();

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String animalType = resultSet.getString("animal_type");
                float latitude = resultSet.getFloat("latitude");
                float longitude = resultSet.getFloat("longitude");
                Timestamp observedAtTs = resultSet.getTimestamp("observed_at");
                LocalDateTime observedAt = observedAtTs == null ? LocalDateTime.now() : observedAtTs.toLocalDateTime();
                int durationMinutes = resultSet.getInt("duration_minutes");
                String uuidText = resultSet.getString("observation_uuid");
                UUID observationUuid = isBlank(uuidText) ? UUID.randomUUID() : UUID.fromString(uuidText);
                boolean revisited = resultSet.getBoolean("revisited");

                Animal animal = new Animal(
                        animalType,
                        latitude,
                        longitude,
                        observedAt,
                        durationMinutes,
                        observationUuid,
                        revisited
                );
                animal.setId(id);
                animals.add(animal);
            }
        }

        return animals;
    }

    // Inserts an animal and returns it with generated ID populated when available.
    public Animal insertAnimal(Animal animal) throws SQLException {
        ensureMySqlDriverLoaded();

        String sql = "INSERT INTO animals (animal_type, latitude, longitude, observed_at, duration_minutes, observation_uuid, revisited) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (animal.getObservationUuid() == null) {
                animal.setObservationUuid(UUID.randomUUID());
            }
            if (animal.getObservedAt() == null) {
                animal.setObservedAt(LocalDateTime.now());
            }

            statement.setString(1, animal.getAnimalType());
            statement.setFloat(2, animal.getLatitude());
            statement.setFloat(3, animal.getLongitude());
            statement.setTimestamp(4, Timestamp.valueOf(animal.getObservedAt()));
            statement.setInt(5, animal.getDurationMinutes());
            statement.setString(6, animal.getObservationUuid().toString());
            statement.setBoolean(7, animal.isRevisited());
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert did not affect any rows");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    animal.setId(generatedKeys.getInt(1));
                }
            }

            return animal;
        }
    }

    // Updates an existing animal row by primary key.
    public void updateAnimal(Animal animal) throws SQLException {
        if (animal.getId() == null) {
            throw new SQLException("Cannot update animal without ID");
        }

        ensureMySqlDriverLoaded();

        String sql = "UPDATE animals SET animal_type = ?, latitude = ?, longitude = ?, observed_at = ?, "
                + "duration_minutes = ?, observation_uuid = ?, revisited = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (animal.getObservationUuid() == null) {
                animal.setObservationUuid(UUID.randomUUID());
            }
            if (animal.getObservedAt() == null) {
                animal.setObservedAt(LocalDateTime.now());
            }

            statement.setString(1, animal.getAnimalType());
            statement.setFloat(2, animal.getLatitude());
            statement.setFloat(3, animal.getLongitude());
            statement.setTimestamp(4, Timestamp.valueOf(animal.getObservedAt()));
            statement.setInt(5, animal.getDurationMinutes());
            statement.setString(6, animal.getObservationUuid().toString());
            statement.setBoolean(7, animal.isRevisited());
            statement.setInt(8, animal.getId());
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new SQLException("No record updated for id=" + animal.getId());
            }
        }
    }

    // Deletes an animal row by primary key.
    public void deleteAnimalById(int id) throws SQLException {
        ensureMySqlDriverLoaded();

        String sql = "DELETE FROM animals WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new SQLException("No record deleted for id=" + id);
            }
        }
    }

    // Ensures the animals table exists and has an AUTO_INCREMENT primary key on id.
    public void ensureIdAutoIncrementPrimaryKey() throws SQLException {
        ensureMySqlDriverLoaded();

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement statement = connection.createStatement()) {

            String createTableSql = "CREATE TABLE IF NOT EXISTS animals ("
                    + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                    + "animal_type VARCHAR(80) NOT NULL, "
                    + "latitude FLOAT NOT NULL, "
                    + "longitude FLOAT NOT NULL, "
                    + "observed_at DATETIME NOT NULL, "
                    + "duration_minutes INT NOT NULL, "
                    + "observation_uuid CHAR(36) NOT NULL UNIQUE, "
                    + "revisited BOOLEAN NOT NULL DEFAULT FALSE"
                    + ")";
            statement.execute(createTableSql);

            ensureColumnExists(statement, "animal_type", "VARCHAR(80) NOT NULL");
            ensureColumnExists(statement, "latitude", "FLOAT NOT NULL DEFAULT 0");
            ensureColumnExists(statement, "longitude", "FLOAT NOT NULL DEFAULT 0");
            ensureColumnExists(statement, "observed_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumnExists(statement, "duration_minutes", "INT NOT NULL DEFAULT 0");
            ensureColumnExists(statement, "observation_uuid", "CHAR(36) NULL");
            ensureColumnExists(statement, "revisited", "BOOLEAN NOT NULL DEFAULT FALSE");

            statement.execute("UPDATE animals SET observation_uuid = UUID() WHERE observation_uuid IS NULL OR observation_uuid = ''");
            statement.execute("ALTER TABLE animals MODIFY COLUMN observation_uuid CHAR(36) NOT NULL");

            if (!uniqueIndexExists(connection, "animals", "ux_animals_observation_uuid")) {
                statement.execute("CREATE UNIQUE INDEX ux_animals_observation_uuid ON animals (observation_uuid)");
            }
        }
    }

    // Adds a missing column for incremental schema upgrades.
    private void ensureColumnExists(Statement statement, String columnName, String columnSql) throws SQLException {
        boolean exists;
        try (ResultSet rs = statement.executeQuery("SHOW COLUMNS FROM animals LIKE '" + columnName + "'")) {
            exists = rs.next();
        }

        if (!exists) {
            statement.execute("ALTER TABLE animals ADD COLUMN " + columnName + " " + columnSql);
        }
    }

    // Checks whether an index already exists on the given table.
    private boolean uniqueIndexExists(Connection connection, String tableName, String indexName) throws SQLException {
        String sql = "SHOW INDEX FROM " + tableName + " WHERE Key_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Returns true when a text value is null or blank.
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // Loads database settings from a standard Java properties file.
    private static Properties loadPropertiesFile(String filePath) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
            return properties;
        } catch (IOException e) {
            return null;
        }
    }

    // Verifies the MySQL JDBC driver is present on the classpath.
    private static void ensureMySqlDriverLoaded() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found. Add mysql-connector-j jar to classpath.", e);
        }
    }
}
