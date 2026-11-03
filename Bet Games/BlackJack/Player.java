import java.util.Scanner;

// The human player: has a balance, places bets and asks the keyboard for cards
public class Player extends Participant {

    private Scanner scanner;
    private int balance;     // money the player has
    private int currentBet;  // bet of the current round

    public Player(String name, Scanner scanner, int startingBalance) {
        super(name); // sends the name to Participant
        this.scanner = scanner;
        this.balance = startingBalance;
        this.currentBet = 0;
    }

    public int getBalance() {
        return balance;
    }

    // Places a bet: the money leaves the balance right away
    public void placeBet(int amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("The bet must be greater than zero.");
        }
        if (amount > balance) {
            throw new InsufficientFundsException(balance, amount);
        }
        currentBet = amount;
        balance -= amount;
    }

    // Win: gets the bet back plus the same amount
    public void winBet() {
        balance += currentBet * 2;
    }

    // Blackjack pays 3 to 2: gets the bet back plus 1.5 times the bet
    // (integer division rounds down, so a bet of 5 wins 7)
    public void winBlackjack() {
        balance += currentBet + currentBet * 3 / 2;
    }

    // Tie: the bet is returned
    public void returnBet() {
        balance += currentBet;
    }

    @Override
    public boolean wantsToHit() {
        while (true) {
            System.out.print("Hit or stand? (h/s): ");
            String answer = scanner.nextLine().trim().toLowerCase();

            if (answer.equals("h")) {
                return true;
            }
            if (answer.equals("s")) {
                return false;
            }
            System.out.println("Please type h or s.");
        }
    }
}