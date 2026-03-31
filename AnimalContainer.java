/*
 * Ishaan Kejriwal - AP CSA
 * File: AnimalContainer.java
 * Description: Container and helper operations for managing animal records.
 * Date: 2026-03-31
 */

// Manages a named collection of animals with simple validation and lookup helpers.
import java.util.ArrayList;
import java.util.List;

public class AnimalContainer {
    private final List<Animal> animals;
    private final String containerName;

    // Initializes the container with a name and an empty list.
    public AnimalContainer(String containerName) {
        this.containerName = containerName;
        this.animals = new ArrayList<>();
    }

    // Adds an animal when valid and logs any user-facing validation error.
    public void addAnimal(Animal animal) {
        try {
            if (animal == null) {
                throw new IllegalArgumentException("Animal cannot be null");
            }
            animals.add(animal);
            System.out.println("Successfully added: " + animal.getName());
        } catch (IllegalArgumentException e) {
            System.err.println("Error adding animal: " + e.getMessage());
        }
    }

    // Removes an animal by name and reports when no match is found.
    public void removeAnimal(String name) {
        try {
            boolean removed = animals.removeIf(a -> a.getName().equals(name));
            if (!removed) {
                throw new IllegalArgumentException("Animal with name '" + name + "' not found");
            }
            System.out.println("Successfully removed: " + name);
        } catch (IllegalArgumentException e) {
            System.err.println("Error removing animal: " + e.getMessage());
        }
    }

    // Finds an animal by name and returns null when not found.
    public Animal getAnimal(String name) {
        try {
            return animals.stream()
                    .filter(a -> a.getName().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + name));
        } catch (IllegalArgumentException e) {
            System.err.println("Error retrieving animal: " + e.getMessage());
            return null;
        }
    }

    // Builds a formatted list of all animals for console display.
    public String displayAllAnimals() {
        try {
            if (animals.isEmpty()) {
                throw new IllegalStateException("No animals in container");
            }
            StringBuilder sb = new StringBuilder("Animals in " + containerName + ":\n");
            for (Animal animal : animals) {
                sb.append("- ").append(animal.getName()).append(" (").append(animal.getAge()).append(" years old)\n");
            }
            return sb.toString();
        } catch (IllegalStateException e) {
            System.err.println("Error displaying animals: " + e.getMessage());
            return "No animals available";
        }
    }

    // Returns a defensive copy to protect internal state.
    public List<Animal> getAllAnimals() {
        return new ArrayList<>(animals);
    }

    // Returns a short container summary.
    public String getContainerInfo() {
        return "Container: " + containerName + " | Total Animals: " + animals.size();
    }
}
