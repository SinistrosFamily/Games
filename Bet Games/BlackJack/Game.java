import java.util.Scanner;

// Controls the whole game: bets, rounds, side bets, insurance and payouts
public class Game {

    private Scanner scanner;
    private Player player;
    private Dealer dealer;
    private Deck deck;

    private int handsWon;
    private int handsLost;
    private int handsTied;

    public Game(String playerName, Scanner scanner, int startingBalance) {
        this.scanner = scanner;
        this.player = new Player(playerName, scanner, startingBalance);
        this.dealer = new Dealer();
        this.deck = new Deck(6); // a shoe with 6 decks
        this.handsWon = 0;
        this.handsLost = 0;
        this.handsTied = 0;
    }

    // Game loop: one turn of the loop = one round
    public void play() {
        System.out.println("=== BLACKJACK ===");
        System.out.println("Blackjack pays 3 to 2. Dealer stands on 17. Insurance pays 2 to 1.");
        System.out.println("Pair side bet: mixed pair 5 to 1, colored pair 10 to 1, perfect pair 30 to 1.");
        System.out.println("You can hit, stand, double, split (once) or surrender.");
        System.out.println("Starting balance: $" + player.getBalance());

        String again = "y";

        while (again.equals("y") && player.getBalance() > 0) {
            playRound();

            if (player.getBalance() > 0) {
                again = ask("Play again? (y/n): ");
            } else {
                System.out.println("You are out of money! Game over.");
            }
        }

        System.out.println("Thanks for playing! Final balance: $" + player.getBalance());
    }

    private void playRound() {
        int startBalance = player.getBalance();

        // clean the table
        player.clearHands();
        player.resetSideBets();

        // a new shoe when it is almost empty
        if (deck.needsShuffle()) {
            deck.reshuffle();
            System.out.println();
            System.out.println("The dealer shuffles a new shoe...");
        }

        askForBet();
        askForPairBet();
        dealCards();

        System.out.println();
        System.out.println(dealer.showHidden());
        System.out.println(player.getName() + ": " + player.getFirstHand()
                + " | bet: $" + player.getFirstHand().getBet());

        resolvePairBet();
        offerInsurance();

        // if a blackjack ends the round, there is nothing else to play
        boolean roundOver = checkBlackjacks();

        if (!roundOver) {
            player.playTurn(deck);
            dealerTurn();
            settleHands();
        }

        printRoundSummary(startBalance);
    }

    // Asks for the main bet until the player types a valid one
    private void askForBet() {
        boolean validBet = false;

        while (!validBet) {
            System.out.println();
            System.out.println("Balance: $" + player.getBalance());
            String text = ask("Your bet: $");

            try {
                int amount = Integer.parseInt(text);
                player.placeBet(amount); // may throw an exception
                validBet = true;         // only reached if no exception happened

            } catch (InsufficientFundsException e) {
                System.out.println("Error: " + e.getMessage());

            } catch (NumberFormatException e) {
                System.out.println("Error: Please type a whole number.");

            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Asks for the optional pair side bet (0 = no side bet)
    private void askForPairBet() {
        boolean validBet = false;

        while (!validBet) {
            String text = ask("Pair side bet (0 to skip): $");

            try {
                int amount = Integer.parseInt(text);
                player.placePairBet(amount); // may throw an exception
                validBet = true;

            } catch (InsufficientFundsException e) {
                System.out.println("Error: " + e.getMessage());

            } catch (NumberFormatException e) {
                System.out.println("Error: Please type a whole number.");

            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Deals two cards to each one: player, dealer, player, dealer
    private void dealCards() {
        dealer.clearHands();
        dealer.addHand(new Hand(0)); // the dealer does not bet

        player.getFirstHand().addCard(deck.draw());
        dealer.getFirstHand().addCard(deck.draw());
        player.getFirstHand().addCard(deck.draw());
        dealer.getFirstHand().addCard(deck.draw());
    }

    // The pair side bet is decided right after the first two cards
    private void resolvePairBet() {
        int pairBet = player.getPairBet();
        if (pairBet == 0) {
            return; // no side bet
        }

        int multiplier = player.getFirstHand().getPairMultiplier();

        if (multiplier == 0) {
            System.out.println("Pair bet: no pair. You lose $" + pairBet + ".");
        } else {
            String type = "Mixed pair";
            if (multiplier == 30) {
                type = "Perfect pair";
            } else if (multiplier == 10) {
                type = "Colored pair";
            }
            player.addMoney(pairBet + pairBet * multiplier); // bet back + prize
            System.out.println("Pair bet: " + type + "! Pays " + multiplier
                    + " to 1. You win $" + (pairBet * multiplier) + ".");
        }
    }

    // Insurance is offered when the dealer shows an ace
    private void offerInsurance() {
        if (!dealer.getUpCard().isAce()) {
            return;
        }

        int cost = player.getFirstHand().getBet() / 2; // insurance costs half of the bet
        if (cost <= 0) {
            return;
        }

        String answer = ask("Dealer shows an Ace. Insurance for $" + cost + "? (y/n): ");

        if (answer.equals("y")) {
            try {
                player.placeInsurance(cost);
                System.out.println("Insurance placed: $" + cost);
            } catch (InsufficientFundsException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Checks natural blackjacks. Returns true if the round is over.
    private boolean checkBlackjacks() {
        Hand playerHand = player.getFirstHand();
        Hand dealerHand = dealer.getFirstHand();

        if (dealerHand.isBlackjack()) {
            System.out.println();
            System.out.println(dealer.showHand());
            System.out.println("Dealer has blackjack!");

            if (player.getInsuranceBet() > 0) {
                player.addMoney(player.getInsuranceBet() * 3); // bet back + 2 to 1
                System.out.println("Insurance pays 2 to 1!");
            }

            if (playerHand.isBlackjack()) {
                System.out.println("Both have blackjack. Push!");
                player.addMoney(playerHand.getBet());
                handsTied++;
            } else {
                System.out.println("You lose $" + playerHand.getBet() + ".");
                handsLost++;
            }
            return true;
        }

        if (player.getInsuranceBet() > 0) {
            System.out.println("Dealer does not have blackjack. Insurance is lost.");
        }

        if (playerHand.isBlackjack()) {
            int bet = playerHand.getBet();
            System.out.println("Blackjack! You win 3 to 2!");
            player.addMoney(bet + bet * 3 / 2); // integer division rounds down
            handsWon++;
            return true;
        }

        return false;
    }

    // The dealer only plays if the player still has a hand in the game
    private void dealerTurn() {
        boolean someoneAlive = false;

        for (Hand hand : player.getHands()) {
            if (!hand.isBust() && !hand.isSurrendered()) {
                someoneAlive = true;
            }
        }

        System.out.println();
        if (!someoneAlive) {
            System.out.println("All your hands are out. The dealer does not need to play.");
            return;
        }

        System.out.println("Dealer's turn...");
        System.out.println(dealer.showHand());
        dealer.playTurn(deck);
    }

    // Compares each hand of the player with the dealer and pays the bets
    private void settleHands() {
        Hand dealerHand = dealer.getFirstHand();
        int count = player.getHands().size();

        System.out.println();
        for (int i = 0; i < count; i++) {
            Hand hand = player.getHands().get(i);
            int bet = hand.getBet();
            String label = "";
            if (count > 1) {
                label = "Hand " + (i + 1) + ": ";
            }

            if (hand.isSurrendered()) {
                player.addMoney(bet / 2); // half of the bet comes back
                System.out.println(label + "Surrendered. Half of the bet is returned ($" + (bet / 2) + ").");
                handsLost++;

            } else if (hand.isBust()) {
                System.out.println(label + "Bust. You lose $" + bet + ".");
                handsLost++;

            } else if (dealerHand.isBust() || hand.getTotal() > dealerHand.getTotal()) {
                player.addMoney(bet * 2); // bet back + the same amount
                System.out.println(label + "You win $" + bet + "!");
                handsWon++;

            } else if (hand.getTotal() < dealerHand.getTotal()) {
                System.out.println(label + "Dealer wins. You lose $" + bet + ".");
                handsLost++;

            } else {
                player.addMoney(bet); // tie: the bet is returned
                System.out.println(label + "Push. Your bet is returned.");
                handsTied++;
            }
        }
    }

    private void printRoundSummary(int startBalance) {
        int net = player.getBalance() - startBalance;

        System.out.println();
        if (net > 0) {
            System.out.println("Round result: +$" + net);
        } else if (net < 0) {
            System.out.println("Round result: -$" + (-net));
        } else {
            System.out.println("Round result: $0");
        }
        System.out.println("Hands -> Won: " + handsWon + " | Lost: " + handsLost + " | Ties: " + handsTied);
        System.out.println("Balance: $" + player.getBalance());
    }

    private String ask(String message) {
        System.out.print(message);
        return scanner.nextLine().trim().toLowerCase();
    }
}