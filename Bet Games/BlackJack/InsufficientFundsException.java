// Custom exception: the player tries to bet more than the balance
public class InsufficientFundsException extends Exception {

    public InsufficientFundsException(int balance, int amount) {
        // the message shows the balance and the amount requested
        super("Insufficient funds. Balance: $" + balance + ". Amount: $" + amount + ".");
    }
}