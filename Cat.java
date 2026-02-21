// Cat implementation that extends Animal and fulfills the Creature contract.
public class Cat extends Animal implements Creature {
    private String color;
    private boolean isAlive;

    // Creates a cat with name, age, and color.
    public Cat(String name, int age, String color) {
        super(name, age, "Cat");
        this.color = color;
        this.isAlive = true;
    }

    // Returns the cat sound.
    @Override
    public String makeSound() {
        return "Meow! Meow!";
    }

    // Returns a formatted cat description.
    @Override
    public String getDescription() {
        return "Cat - " + getName() + " (Color: " + color + ")";
    }

    // Prints cat details to standard output.
    @Override
    public void displayInfo() {
        System.out.println("Cat Details: " + getDescription() + ", Age: " + getAge() + ", Sound: " + makeSound());
    }

    // Returns whether this cat is alive.
    @Override
    public boolean isAlive() {
        return isAlive;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
