//Still in development, but the game is playable. 
//The player can place bets, hit or stand, and the dealer will play according to the rules of blackjack.
//The game keeps track of wins, losses, ties, and the player's balance. 
import java.util.Scanner;

public class Blackjack {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Your name: ");
        String name = scanner.nextLine();

        int startingBalance = 100;
        Player player = new Player(name, scanner, startingBalance);
        Dealer dealer = new Dealer();
        System.out.println("Starting balance: $" + startingBalance);

        int playerWins = 0;
        int dealerWins = 0;
        int ties = 0;
        String again = "y";

        // game loop: one round per turn, ends when the player quits or has no money
        while (again.equals("y") && player.getBalance() > 0) {
            askForBet(player, scanner);
            int result = playRound(player, dealer);

            // pay the bet according to the result
            if (result == 2) {
                player.winBlackjack();
                playerWins++;
            } else if (result == 1) {
                player.winBet();
                playerWins++;
            } else if (result == -1) {
                dealerWins++; // the bet was already taken from the balance
            } else {
                player.returnBet();
                ties++;
            }

            System.out.println("Score -> You: " + playerWins + " | Dealer: " + dealerWins + " | Ties: " + ties);
            System.out.println("Balance: $" + player.getBalance());

            if (player.getBalance() > 0) {
                System.out.print("Play again? (y/n): ");
                again = scanner.nextLine().trim().toLowerCase();
            } else {
                System.out.println("You are out of money! Game over.");
            }
        }

        System.out.println("Thanks for playing! Final balance: $" + player.getBalance());
        scanner.close();
    }

    // Asks for a bet until the player types a valid one
    static void askForBet(Player player, Scanner scanner) {
        boolean validBet = false;

        while (!validBet) {
            System.out.println();
            System.out.println("Balance: $" + player.getBalance());
            System.out.print("Your bet: $");

            try {
                int amount = Integer.parseInt(scanner.nextLine().trim());
                player.placeBet(amount); // may throw an exception
                validBet = true;        // only reached if no exception happened

            } catch (InsufficientFundsException e) {
                System.out.println("Error: " + e.getMessage());

            } catch (NumberFormatException e) {
                System.out.println("Error: Please type a whole number.");

            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Plays one round.
    // Returns 2 (player blackjack), 1 (player wins), 0 (tie) or -1 (dealer wins)
    static int playRound(Player player, Dealer dealer) {
        Deck deck = new Deck(); // new shuffled deck every round
        player.clearHand();
        dealer.clearHand();

        // deal two cards to each one
        player.addCard(deck.draw());
        dealer.addCard(deck.draw());
        player.addCard(deck.draw());
        dealer.addCard(deck.draw());

        System.out.println();
        System.out.println(dealer.showHidden());
        System.out.println(player.showHand());

        // check for a natural blackjack
        if (player.hasBlackjack() || dealer.hasBlackjack()) {
            System.out.println(dealer.showHand());
            if (player.hasBlackjack() && dealer.hasBlackjack()) {
                System.out.println("Both have blackjack. Tie!");
                return 0;
            } else if (player.hasBlackjack()) {
                System.out.println("Blackjack! You win 3 to 2!");
                return 2;
            } else {
                System.out.println("Dealer has blackjack. You lose!");
                return -1;
            }
        }

        // player's turn
        while (player.wantsToHit()) {
            player.addCard(deck.draw());
            System.out.println(player.showHand());

            if (player.isBust()) {
                System.out.println("Bust! You lose!");
                return -1;
            }
            if (player.getTotal() == 21) {
                break; // 21 is the best total, no need to ask again
            }
        }

        // dealer's turn
        System.out.println();
        System.out.println("Dealer's turn...");
        System.out.println(dealer.showHand());

        while (dealer.wantsToHit()) {
            dealer.addCard(deck.draw());
            System.out.println("Dealer hits: " + dealer.showHand());
        }

        if (dealer.isBust()) {
            System.out.println("Dealer busts! You win!");
            return 1;
        }

        // compare the totals
        if (player.getTotal() > dealer.getTotal()) {
            System.out.println("You win!");
            return 1;
        } else if (player.getTotal() < dealer.getTotal()) {
            System.out.println("You lose!");
            return -1;
        } else {
            System.out.println("Tie!");
            return 0;
        }
    }
}