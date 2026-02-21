import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "SELECT id, name, age, type, details FROM animals ORDER BY id";

        ensureMySqlDriverLoaded();

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                int age = resultSet.getInt("age");
                String type = resultSet.getString("type");
                String details = resultSet.getString("details");

                Animal animal = mapRowToAnimal(name, age, type, details);
                animal.setId(id);
                animals.add(animal);
            }
        }

        return animals;
    }

    // Inserts an animal and returns it with generated ID populated when available.
    public Animal insertAnimal(Animal animal) throws SQLException {
        ensureMySqlDriverLoaded();

        String sql = "INSERT INTO animals (name, age, type, details) VALUES (?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String type = resolveType(animal);
            String details = resolveDetails(animal);

            statement.setString(1, animal.getName());
            statement.setInt(2, animal.getAge());
            statement.setString(3, type);
            statement.setString(4, details);
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

        String sql = "UPDATE animals SET name = ?, age = ?, type = ?, details = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            String type = resolveType(animal);
            String details = resolveDetails(animal);

            statement.setString(1, animal.getName());
            statement.setInt(2, animal.getAge());
            statement.setString(3, type);
            statement.setString(4, details);
            statement.setInt(5, animal.getId());
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
                    + "name VARCHAR(100) NOT NULL, "
                    + "age INT NOT NULL, "
                    + "type VARCHAR(20) NOT NULL, "
                    + "details VARCHAR(120) NULL"
                    + ")";
            statement.execute(createTableSql);

            boolean hasIdColumn = false;
            try (ResultSet idColumnRs = statement.executeQuery("SHOW COLUMNS FROM animals LIKE 'id'")) {
                hasIdColumn = idColumnRs.next();
            }

            if (!hasIdColumn) {
                statement.execute("ALTER TABLE animals ADD COLUMN id INT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST");
                return;
            }

            statement.execute("ALTER TABLE animals MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT");

            boolean hasPrimaryKey = false;
            try (ResultSet pkRs = statement.executeQuery("SHOW INDEX FROM animals WHERE Key_name = 'PRIMARY'")) {
                hasPrimaryKey = pkRs.next();
            }

            if (!hasPrimaryKey) {
                statement.execute("ALTER TABLE animals ADD PRIMARY KEY (id)");
            }
        }
    }

    // Maps a database row to the matching domain subtype.
    private Animal mapRowToAnimal(String name, int age, String type, String details) {
        String safeType = type == null ? "" : type.trim().toLowerCase();
        String safeDetails = details == null || details.isBlank() ? "Unknown" : details.trim();

        if ("dog".equals(safeType)) {
            return new Dog(name, age, safeDetails);
        }

        if ("cat".equals(safeType)) {
            return new Cat(name, age, safeDetails);
        }

        return new Dog(name, age, "Mixed Breed");
    }

    // Resolves the stored type label from the concrete animal instance.
    private String resolveType(Animal animal) {
        if (animal instanceof Dog) {
            return "Dog";
        }

        if (animal instanceof Cat) {
            return "Cat";
        }

        String species = animal.getSpecies();
        return isBlank(species) ? "Dog" : species.trim();
    }

    // Resolves the stored details value from subtype-specific fields.
    private String resolveDetails(Animal animal) {
        if (animal instanceof Dog) {
            String breed = ((Dog) animal).getBreed();
            return isBlank(breed) ? "Mixed Breed" : breed.trim();
        }

        if (animal instanceof Cat) {
            String color = ((Cat) animal).getColor();
            return isBlank(color) ? "Unknown" : color.trim();
        }

        return "Unknown";
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
