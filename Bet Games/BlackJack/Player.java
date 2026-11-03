import java.util.Scanner;

// The human player: has a balance, places bets and chooses what to do with each hand
public class Player extends Participant {

    private Scanner scanner;
    private int balance;       // money the player has
    private int pairBet;       // pair side bet of the current round
    private int insuranceBet;  // insurance bet of the current round

    public Player(String name, Scanner scanner, int startingBalance) {
        super(name); // sends the name to Participant
        this.scanner = scanner;
        this.balance = startingBalance;
        this.pairBet = 0;
        this.insuranceBet = 0;
    }

    public int getBalance() {
        return balance;
    }

    public int getPairBet() {
        return pairBet;
    }

    public int getInsuranceBet() {
        return insuranceBet;
    }

    // Gives money to the player (used for payouts)
    public void addMoney(int amount) {
        balance += amount;
    }

    // Takes money from the balance, or throws if there is not enough
    private void takeMoney(int amount) throws InsufficientFundsException {
        if (amount > balance) {
            throw new InsufficientFundsException(balance, amount);
        }
        balance -= amount;
    }

    // Main bet: creates the player's first hand
    public void placeBet(int amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("The bet must be greater than zero.");
        }
        takeMoney(amount);
        addHand(new Hand(amount));
    }

    // Pair side bet (0 means no side bet)
    public void placePairBet(int amount) throws InsufficientFundsException {
        if (amount < 0) {
            throw new IllegalArgumentException("The bet cannot be negative.");
        }
        takeMoney(amount);
        pairBet = amount;
    }

    // Insurance bet
    public void placeInsurance(int amount) throws InsufficientFundsException {
        takeMoney(amount);
        insuranceBet = amount;
    }

    public void resetSideBets() {
        pairBet = 0;
        insuranceBet = 0;
    }

    // Plays every hand of the player, one after the other
    @Override
    public void playTurn(Deck deck) {
        // normal for loop, because the list can grow when the player splits
        for (int i = 0; i < getHands().size(); i++) {
            playHand(getHands().get(i), i, deck);
        }
    }

    private void playHand(Hand hand, int index, Deck deck) {
        while (true) {
            showHand(hand, index);

            if (hand.isBust()) {
                System.out.println("Bust!");
                return;
            }
            if (hand.getTotal() == 21) {
                System.out.println("21!");
                return;
            }
            // split aces receive only one card
            if (hand.isFromSplit() && hand.getCard(0).isAce()) {
                System.out.println("Split aces receive only one card.");
                return;
            }

            // special moves are only allowed with the first two cards
            boolean firstMove = hand.getSize() == 2;
            boolean canSplit = firstMove && getHands().size() == 1 && hand.canSplit();
            boolean canSurrender = firstMove && getHands().size() == 1;

            String menu = "(h)it, (s)tand";
            if (firstMove) {
                menu += ", (d)ouble";
            }
            if (canSplit) {
                menu += ", s(p)lit";
            }
            if (canSurrender) {
                menu += ", su(r)render";
            }

            String answer = ask(menu + ": ");

            if (answer.equals("h")) {
                hand.addCard(deck.draw());

            } else if (answer.equals("s")) {
                return;

            } else if (answer.equals("d") && firstMove) {
                try {
                    takeMoney(hand.getBet()); // pays the same amount again
                    hand.doubleBet();
                    hand.addCard(deck.draw());
                    System.out.println("Double down! You get one card only.");
                    showHand(hand, index);
                    if (hand.isBust()) {
                        System.out.println("Bust!");
                    }
                    return; // the turn of this hand ends after a double
                } catch (InsufficientFundsException e) {
                    System.out.println("Error: " + e.getMessage());
                }

            } else if (answer.equals("p") && canSplit) {
                try {
                    takeMoney(hand.getBet()); // the new hand needs its own bet
                    splitHand(hand, index, deck);
                } catch (InsufficientFundsException e) {
                    System.out.println("Error: " + e.getMessage());
                }

            } else if (answer.equals("r") && canSurrender) {
                hand.surrender();
                System.out.println("You surrendered this hand.");
                return;

            } else {
                System.out.println("Invalid option. Choose one of the letters in parentheses.");
            }
        }
    }

    // Splits one hand into two hands, each one with the same bet
    private void splitHand(Hand hand, int index, Deck deck) {
        Hand newHand = new Hand(hand.getBet());
        hand.setFromSplit(true);
        newHand.setFromSplit(true);

        newHand.addCard(hand.removeLastCard()); // the second card goes to the new hand
        hand.addCard(deck.draw());              // each hand gets one new card
        newHand.addCard(deck.draw());

        addHand(index + 1, newHand);            // the new hand is played right after this one
        System.out.println("Hand split! Each hand has a bet of $" + hand.getBet() + ".");
    }

    private void showHand(Hand hand, int index) {
        System.out.println();
        String label = getName();
        if (getHands().size() > 1) {
            label += " (hand " + (index + 1) + " of " + getHands().size() + ")";
        }
        System.out.println(label + ": " + hand + " | bet: $" + hand.getBet());
    }

    private String ask(String message) {
        System.out.print(message);
        return scanner.nextLine().trim().toLowerCase();
    }
}