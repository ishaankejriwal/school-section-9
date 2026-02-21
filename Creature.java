// Interface that defines behavior shared by concrete creature implementations.
public interface Creature {
    // Returns a concise text summary for display and logs.
    String getDescription();

    // Prints or renders the current creature details.
    void displayInfo();

    // Indicates whether the creature is currently alive.
    boolean isAlive();
}
