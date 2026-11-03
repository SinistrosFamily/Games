import java.util.ArrayList;

// Abstract class: anything that holds cards (player or dealer)
public abstract class Participant {

    private String name;
    private ArrayList<Card> hand; // cards in the hand

    public Participant(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public Card getCard(int index) {
        return hand.get(index);
    }

    public void clearHand() {
        hand.clear();
    }

    // Total points: an ace counts 11, but becomes 1 if the total goes over 21
    public int getTotal() {
        int total = 0;
        int aces = 0;

        for (Card card : hand) {
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

    // Blackjack = 21 with only two cards
    public boolean hasBlackjack() {
        return hand.size() == 2 && getTotal() == 21;
    }

    public String showHand() {
        return name + ": " + hand + " (total: " + getTotal() + ")";
    }

    // Each type of participant decides in its own way
    public abstract boolean wantsToHit();
}