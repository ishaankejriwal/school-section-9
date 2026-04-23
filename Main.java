/*
 * Ishaan Kejriwal - AP CSA
 * File: Main.java
 * Description: Application entry point and startup flow.
 * Date: 2026-03-31
 */

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

// Entry point for bootstrapping data sources and launching the desktop UI.
public class Main {
    public static void main (String[] args) {
        try {
            // Create the in-memory container used by both console output and GUI.
            AnimalContainer animalContainer = new AnimalContainer("Daily Habitat Tracker");

            // Load animals from MySQL when credentials are available.
            boolean loadedFromDatabase = loadAnimalsFromMySQL(animalContainer);

            // Use sample data when the database is unavailable or empty.
            if (!loadedFromDatabase) {
                animalContainer.addAnimal(new Animal(
                    "Dog",
                    40.7128f,
                    -74.0060f,
                    LocalDateTime.now().minusHours(6),
                    45,
                    UUID.randomUUID(),
                    true
                ));
                animalContainer.addAnimal(new Animal(
                    "Cat",
                    40.7132f,
                    -74.0051f,
                    LocalDateTime.now().minusHours(4),
                    30,
                    UUID.randomUUID(),
                    false
                ));
                animalContainer.addAnimal(new Animal(
                    "Dog",
                    40.7128f,
                    -74.0060f,
                    LocalDateTime.now().minusHours(1),
                    20,
                    UUID.randomUUID(),
                    true
                ));
            }

            // Print a quick summary in the console for immediate feedback.
            System.out.println("===== Animal Observation Tracking =====\n");
            System.out.println(animalContainer.getContainerInfo() + "\n");
            System.out.println(animalContainer.displayAllAnimals());

            System.out.println("===== Observation UUIDs =====");
            for (Animal animal : animalContainer.getAllAnimals()) {
                System.out.println(animal.getAnimalType() + " -> " + animal.getObservationUuid());
            }

            // Start the Swing application window.
            AnimalGUI gui = new AnimalGUI(animalContainer);
            gui.setVisible(true);

        } catch (RuntimeException e) {
            System.err.println("Critical error in main: " + e.getMessage());
        }
    }

    // Tries to load records from MySQL and returns true only when at least one row is loaded.
    private static boolean loadAnimalsFromMySQL(AnimalContainer animalContainer) {
        try {
            MySQLAnimalRepository repository = MySQLAnimalRepository.fromEnvironment();
            if (repository == null) {
                System.out.println("MySQL not configured. Set DB_URL, DB_USER, DB_PASSWORD to enable DB records.");
                return false;
            }

            repository.ensureIdAutoIncrementPrimaryKey();

            int loaded = 0;
            for (Animal animal : repository.fetchAllAnimals()) {
                animalContainer.addAnimal(animal);
                loaded++;
            }

            if (loaded == 0) {
                System.out.println("MySQL connected, but no rows found in animals table.");
                return false;
            }

            System.out.println("Loaded " + loaded + " record(s) from MySQL.");
            return true;
        } catch (SQLException | RuntimeException e) {
            System.err.println("MySQL load failed: " + e.getMessage());
            return false;
        }
    }
}
