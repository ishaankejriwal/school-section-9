// Entry point for bootstrapping data sources and launching the desktop UI.
public class Main {
    public static void main (String[] args) {
        try {
            // Create the in-memory container used by both console output and GUI.
            AnimalContainer animalContainer = new AnimalContainer("Pet Sanctuary");

            // Load animals from MySQL when credentials are available.
            boolean loadedFromDatabase = loadAnimalsFromMySQL(animalContainer);

            // Use sample data when the database is unavailable or empty.
            if (!loadedFromDatabase) {
                animalContainer.addAnimal(new Dog("Buddy", 5, "Golden Retriever"));
                animalContainer.addAnimal(new Cat("Whiskers", 3, "Black"));
                animalContainer.addAnimal(new Dog("Max", 7, "German Shepherd"));
                animalContainer.addAnimal(new Cat("Luna", 2, "White"));
            }

            // Print a quick summary in the console for immediate feedback.
            System.out.println("===== Animal Management System =====\n");
            System.out.println(animalContainer.getContainerInfo() + "\n");
            System.out.println(animalContainer.displayAllAnimals());

            // Show polymorphic behavior by calling subclass-specific sounds.
            System.out.println("===== Animal Sounds =====");
            for (Animal animal : animalContainer.getAllAnimals()) {
                System.out.println(animal.getName() + " says: " + animal.makeSound());
            }

            // Start the Swing application window.
            AnimalGUI gui = new AnimalGUI(animalContainer);
            gui.setVisible(true);

        } catch (Exception e) {
            System.err.println("Critical error in main: " + e.getMessage());
            e.printStackTrace();
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
        } catch (Exception e) {
            System.err.println("MySQL load failed: " + e.getMessage());
            return false;
        }
    }
}
