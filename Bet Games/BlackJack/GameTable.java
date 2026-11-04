import java.util.ArrayList;

// Rules and state of the game. It has no windows: the window only asks it to do things.
public class GameTable {

    public static final int STARTING_BALANCE = 500;

    private Deck deck;
    private Hand dealerHand;
    private ArrayList<Hand> playerHands;    // two hands after a split
    private ArrayList<String> handResults;  // text with the result of each hand
    private ArrayList<String> messages;     // news for the window to show

    private int balance;
    private int pairBet;
    private int insuranceBet;
    private int startBalance;    // balance before the bets of the round
    private int currentHand;     // index of the hand being played
    private boolean holeHidden;  // the dealer's second card is face down
    private Phase phase;

    private int handsWon;
    private int handsLost;
    private int handsTied;

    public GameTable() {
        deck = new Deck(6); // a shoe with 6 decks
        dealerHand = new Hand(0);
        playerHands = new ArrayList<>();
        handResults = new ArrayList<>();
        messages = new ArrayList<>();
        resetGame();

        messages.add("Welcome! You start with $" + balance + ".");
        messages.add("Blackjack pays 3 to 2. Dealer stands on 17. Insurance pays 2 to 1.");
        messages.add("Pair side bet: mixed pair 5 to 1, colored pair 10 to 1, perfect pair 30 to 1.");
        messages.add("Place your bet and press Deal.");
    }

    // ---------- starting and cleaning ----------

    // A completely new game: full balance and empty table
    public void newGame() {
        resetGame();
        deck.reshuffle();
        messages.add("New game! Your balance is $" + balance + ".");
    }

    private void resetGame() {
        balance = STARTING_BALANCE;
        handsWon = 0;
        handsLost = 0;
        handsTied = 0;
        clearTable();
    }

    private void clearTable() {
        dealerHand = new Hand(0); // the dealer does not bet
        playerHands.clear();
        handResults.clear();
        pairBet = 0;
        insuranceBet = 0;
        currentHand = 0;
        holeHidden = false;
        phase = Phase.BETTING;
    }

    // After the results, goes back to the betting
    public void nextRound() {
        if (phase == Phase.ROUND_OVER) {
            clearTable();
            messages.add("Place your bet and press Deal.");
        }
    }

    // ---------- the bets and the deal ----------

    // Takes the bets and deals the cards.
    // Throws InsufficientFundsException if the money is not enough
    // and IllegalArgumentException if a bet is not valid.
    public void startRound(int bet, int pair) throws InsufficientFundsException {
        if (phase != Phase.BETTING) {
            return;
        }
        if (bet <= 0) {
            throw new IllegalArgumentException("The bet must be greater than zero.");
        }
        if (pair < 0) {
            throw new IllegalArgumentException("The pair bet cannot be negative.");
        }
        if (bet > balance) {
            throw new InsufficientFundsException(balance, bet);
        }
        if (pair > balance - bet) { // the pair bet is paid with what is left
            throw new InsufficientFundsException(balance - bet, pair);
        }

        // a new shoe when it is almost empty
        if (deck.needsShuffle()) {
            deck.reshuffle();
            messages.add("The dealer shuffles a new shoe...");
        }

        // take the money
        clearTable();
        startBalance = balance;
        balance -= bet + pair;
        pairBet = pair;
        playerHands.add(new Hand(bet));
        messages.add("--- New round: bet $" + bet + ", pair bet $" + pair + " ---");

        // deal two cards to each one: player, dealer, player, dealer
        Hand playerHand = playerHands.get(0);
        playerHand.addCard(deck.draw());
        dealerHand.addCard(deck.draw());
        playerHand.addCard(deck.draw());
        dealerHand.addCard(deck.draw());
        holeHidden = true;

        resolvePairBet();

        // insurance is offered when the dealer shows an Ace
        if (dealerHand.getCard(0).isAce() && getInsuranceCost() > 0) {
            phase = Phase.INSURANCE;
            messages.add("Dealer shows an Ace. Insurance costs $" + getInsuranceCost() + ".");
        } else {
            checkBlackjacks();
        }
    }

    // The pair side bet is decided right after the first two cards
    private void resolvePairBet() {
        if (pairBet == 0) {
            return; // no side bet
        }

        int multiplier = playerHands.get(0).getPairMultiplier();

        if (multiplier == 0) {
            messages.add("Pair bet: no pair. You lose $" + pairBet + ".");
        } else {
            String type = "Mixed pair";
            if (multiplier == 30) {
                type = "Perfect pair";
            } else if (multiplier == 10) {
                type = "Colored pair";
            }
            balance += pairBet + pairBet * multiplier; // bet back + prize
            messages.add("Pair bet: " + type + "! Pays " + multiplier
                    + " to 1. You win $" + (pairBet * multiplier) + ".");
        }
    }

    // ---------- insurance ----------

    public int getInsuranceCost() {
        return playerHands.get(0).getBet() / 2; // half of the bet
    }

    public void takeInsurance() throws InsufficientFundsException {
        if (phase != Phase.INSURANCE) {
            return;
        }
        int cost = getInsuranceCost();
        if (cost > balance) {
            throw new InsufficientFundsException(balance, cost);
        }
        balance -= cost;
        insuranceBet = cost;
        messages.add("Insurance placed: $" + cost + ".");
        checkBlackjacks();
    }

    public void declineInsurance() {
        if (phase != Phase.INSURANCE) {
            return;
        }
        checkBlackjacks();
    }

    // ---------- natural blackjacks ----------

    // Checks the blackjacks. If nobody has one, the player starts to play.
    private void checkBlackjacks() {
        Hand playerHand = playerHands.get(0);
        int bet = playerHand.getBet();

        if (dealerHand.isBlackjack()) {
            holeHidden = false;
            messages.add("Dealer has blackjack!");

            if (insuranceBet > 0) {
                balance += insuranceBet * 3; // bet back + 2 to 1
                messages.add("Insurance pays 2 to 1!");
            }

            if (playerHand.isBlackjack()) {
                balance += bet;
                handResults.add("PUSH");
                handsTied++;
                messages.add("Both have blackjack. Push!");
            } else {
                handResults.add("LOSE -$" + bet);
                handsLost++;
                messages.add("You lose $" + bet + ".");
            }
            endRound();
            return;
        }

        if (insuranceBet > 0) {
            messages.add("Dealer does not have blackjack. Insurance is lost.");
        }

        if (playerHand.isBlackjack()) {
            holeHidden = false;
            balance += bet + bet * 3 / 2; // integer division rounds down
            handResults.add("BLACKJACK +$" + (bet * 3 / 2));
            handsWon++;
            messages.add("Blackjack! You win 3 to 2!");
            endRound();
            return;
        }

        // nobody has a blackjack: the player plays
        phase = Phase.PLAYING;
        currentHand = 0;
        messages.add("Your turn.");
    }

    // ---------- the moves of the player ----------

    public boolean canDouble() {
        return phase == Phase.PLAYING && playerHands.get(currentHand).getSize() == 2;
    }

    public boolean canSplit() {
        return canDouble() && playerHands.size() == 1 && playerHands.get(currentHand).canSplit();
    }

    public boolean canSurrender() {
        return canDouble() && playerHands.size() == 1;
    }

    public void hit() {
        if (phase != Phase.PLAYING) {
            return;
        }
        playerHands.get(currentHand).addCard(deck.draw());
        moveToNextHand(); // goes on only if this hand is finished (bust or 21)
    }

    public void stand() {
        if (phase != Phase.PLAYING) {
            return;
        }
        currentHand++;
        moveToNextHand();
    }

    public void doubleDown() throws InsufficientFundsException {
        if (!canDouble()) {
            return;
        }
        Hand hand = playerHands.get(currentHand);
        if (hand.getBet() > balance) {
            throw new InsufficientFundsException(balance, hand.getBet());
        }

        balance -= hand.getBet(); // pays the same amount again
        hand.doubleBet();
        hand.addCard(deck.draw());
        messages.add(handName(currentHand) + "Double down! One card only.");
        if (hand.isBust()) {
            messages.add(handName(currentHand) + "Bust!");
        }

        currentHand++; // the turn of this hand ends after a double
        moveToNextHand();
    }

    public void split() throws InsufficientFundsException {
        if (!canSplit()) {
            return;
        }
        Hand hand = playerHands.get(currentHand);
        if (hand.getBet() > balance) {
            throw new InsufficientFundsException(balance, hand.getBet());
        }

        balance -= hand.getBet(); // the new hand needs its own bet
        Hand newHand = new Hand(hand.getBet());
        hand.setFromSplit(true);
        newHand.setFromSplit(true);

        newHand.addCard(hand.removeLastCard()); // the second card goes to the new hand
        hand.addCard(deck.draw());              // each hand gets one new card
        newHand.addCard(deck.draw());

        playerHands.add(currentHand + 1, newHand); // played right after this one
        messages.add("Hand split! Each hand has a bet of $" + hand.getBet() + ".");
        moveToNextHand();
    }

    public void surrender() {
        if (!canSurrender()) {
            return;
        }
        playerHands.get(currentHand).surrender();
        messages.add("You surrendered. Half of the bet comes back.");
        moveToNextHand();
    }

    // ---------- moving through the hands ----------

    // A hand that cannot receive more cards
    private boolean isFinished(Hand hand) {
        boolean splitAce = hand.isFromSplit() && hand.getCard(0).isAce();
        return hand.isBust() || hand.isSurrendered() || hand.getTotal() == 21 || splitAce;
    }

    // Skips the finished hands. When no hand is left, the dealer plays.
    private void moveToNextHand() {
        while (currentHand < playerHands.size()) {
            Hand hand = playerHands.get(currentHand);
            if (!isFinished(hand)) {
                return; // this hand still needs a decision
            }
            announceFinishedHand(hand, currentHand);
            currentHand++;
        }
        finishRound();
    }

    private void announceFinishedHand(Hand hand, int index) {
        if (hand.isBust()) {
            messages.add(handName(index) + "Bust!");
        } else if (hand.getTotal() == 21) {
            messages.add(handName(index) + "21!");
        } else if (hand.isFromSplit() && hand.getCard(0).isAce()) {
            messages.add(handName(index) + "Split aces receive only one card.");
        }
    }

    // "Hand 2: " when there are two hands, nothing when there is only one
    private String handName(int index) {
        if (playerHands.size() > 1) {
            return "Hand " + (index + 1) + ": ";
        }
        return "";
    }

    // ---------- the dealer and the results ----------

    // The dealer plays (if needed), then the bets are paid
    // The player finished. If a hand is still alive, the dealer plays;
    // the window calls dealerStep() again and again (with a Timer).
    private void finishRound() {
        boolean someoneAlive = false;
        for (Hand hand : playerHands) {
            if (!hand.isBust() && !hand.isSurrendered()) {
                someoneAlive = true;
            }
        }

        if (someoneAlive) {
            phase = Phase.DEALER_TURN;
            messages.add("Dealer's turn...");
        } else {
            holeHidden = false;
            messages.add("All your hands are out. The dealer does not play.");
            settleHands();
            endRound();
        }
    }
    // One step of the dealer: reveal the hidden card, or draw one card, or finish.
    // Returns true while there are more steps to do.
    public boolean dealerStep() {
        if (phase != Phase.DEALER_TURN) {
            return false;
        }

        if (holeHidden) {
            holeHidden = false;
            messages.add("Dealer reveals: " + dealerHand);
            return true;
        }

        if (dealerHand.getTotal() < 17) {
            dealerHand.addCard(deck.draw());
            messages.add("Dealer hits: " + dealerHand);
            return true;
        }

        // the dealer is done: the bets are paid
        if (dealerHand.isBust()) {
            messages.add("Dealer busts!");
        } else {
            messages.add("Dealer stands with " + dealerHand.getTotal() + ".");
        }
        settleHands();
        endRound();
        return false;
    }


    // Compares each hand of the player with the dealer and pays the bets
    private void settleHands() {
        for (int i = 0; i < playerHands.size(); i++) {
            Hand hand = playerHands.get(i);
            int bet = hand.getBet();
            String result;

            if (hand.isSurrendered()) {
                balance += bet / 2; // half of the bet comes back
                result = "SURRENDER -$" + (bet - bet / 2);
                handsLost++;

            } else if (hand.isBust()) {
                result = "BUST -$" + bet;
                handsLost++;

            } else if (dealerHand.isBust() || hand.getTotal() > dealerHand.getTotal()) {
                balance += bet * 2; // bet back + the same amount
                result = "WIN +$" + bet;
                handsWon++;

            } else if (hand.getTotal() < dealerHand.getTotal()) {
                result = "LOSE -$" + bet;
                handsLost++;

            } else {
                balance += bet; // tie: the bet is returned
                result = "PUSH";
                handsTied++;
            }

            handResults.add(result);
            messages.add(handName(i) + result);
        }
    }

    private void endRound() {
        phase = Phase.ROUND_OVER;
        holeHidden = false;

        int net = balance - startBalance;
        if (net > 0) {
            messages.add("Round result: +$" + net);
        } else if (net < 0) {
            messages.add("Round result: -$" + (-net));
        } else {
            messages.add("Round result: $0");
        }

        if (balance <= 0) {
            messages.add("You are out of money! Game over.");
        }
    }

    // ---------- information for the window ----------

    public Phase getPhase() {
        return phase;
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

    public int getCurrentHand() {
        return currentHand;
    }

    public Hand getDealerHand() {
        return dealerHand;
    }

    public ArrayList<Hand> getPlayerHands() {
        return playerHands;
    }

    public boolean isHoleHidden() {
        return holeHidden;
    }

    // Result of a hand ("WIN +$10", ...). Empty while the round is not over.
    public String getHandResult(int index) {
        if (index < handResults.size()) {
            return handResults.get(index);
        }
        return "";
    }

    public int getHandsWon() {
        return handsWon;
    }

    public int getHandsLost() {
        return handsLost;
    }

    public int getHandsTied() {
        return handsTied;
    }

    public ArrayList<String> getMessages() {
        return messages;
    }

    public void clearMessages() {
        messages.clear();
    }
}