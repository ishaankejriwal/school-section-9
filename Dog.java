// Dog implementation that extends Animal and fulfills the Creature contract.
public class Dog extends Animal implements Creature {
    private String breed;
    private boolean isAlive;

    // Creates a dog with name, age, and breed.
    public Dog(String name, int age, String breed) {
        super(name, age, breed);
        this.breed = breed;
        this.isAlive = true;
    }

    // Returns the dog sound.
    @Override
    public String makeSound() {
        return "Woof! Woof!";
    }

    // Returns a formatted dog description.
    @Override
    public String getDescription() {
        return "Dog - " + getName() + " (Breed: " + breed + ")";
    }

    // Prints dog details to standard output.
    @Override
    public void displayInfo() {
        System.out.println("Dog Details: " + getDescription() + ", Age: " + getAge() + ", Sound: " + makeSound());
    }

    // Returns whether this dog is alive.
    @Override
    public boolean isAlive() {
        return isAlive;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }
}
