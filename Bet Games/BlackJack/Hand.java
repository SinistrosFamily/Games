import java.util.ArrayList;

// One hand of cards with its bet (a player can have two hands after a split)
public class Hand {

    private ArrayList<Card> cards;
    private int bet;
    private boolean fromSplit;    // true if this hand was created by a split
    private boolean surrendered;  // true if the player gave up this hand

    public Hand(int bet) {
        this.cards = new ArrayList<>();
        this.bet = bet;
        this.fromSplit = false;
        this.surrendered = false;
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public Card getCard(int index) {
        return cards.get(index);
    }

    public int getSize() {
        return cards.size();
    }

    // Removes and returns the last card (used when splitting)
    public Card removeLastCard() {
        return cards.remove(cards.size() - 1);
    }

    public int getBet() {
        return bet;
    }

    // Double down: the bet becomes twice as big
    public void doubleBet() {
        bet = bet * 2;
    }

    public boolean isFromSplit() {
        return fromSplit;
    }

    public void setFromSplit(boolean fromSplit) {
        this.fromSplit = fromSplit;
    }

    public boolean isSurrendered() {
        return surrendered;
    }

    public void surrender() {
        surrendered = true;
    }

    // Total points: an ace counts 11, but becomes 1 if the total goes over 21
    public int getTotal() {
        int total = 0;
        int aces = 0;

        for (Card card : cards) {
            total += card.getValue();
            if (card.isAce()) {
                aces++;
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10; // ace changes from 11 to 1
            aces--;
        }
        return total;
    }

    public boolean isBust() {
        return getTotal() > 21;
    }

    // Blackjack = 21 with only two cards (a 21 after a split does not count)
    public boolean isBlackjack() {
        return cards.size() == 2 && getTotal() == 21 && !fromSplit;
    }

    // Two cards with the same value can be split (10 and K, for example)
    public boolean canSplit() {
        return cards.size() == 2 && cards.get(0).getValue() == cards.get(1).getValue();
    }

    // Pair side bet: returns the payout multiplier, or 0 if there is no pair
    public int getPairMultiplier() {
        Card first = cards.get(0);
        Card second = cards.get(1);

        if (!first.getRank().equals(second.getRank())) {
            return 0;  // no pair
        }
        if (first.getSuit().equals(second.getSuit())) {
            return 30; // perfect pair: same rank and same suit
        }
        if (first.isRed() == second.isRed()) {
            return 10; // colored pair: same rank and same color
        }
        return 5;      // mixed pair: same rank, different colors
    }

    @Override
    public String toString() {
        return cards + " (total: " + getTotal() + ")";
    }
}