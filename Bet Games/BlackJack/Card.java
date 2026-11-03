// One playing card
public class Card {

    private String rank;  // "2" to "10", "J", "Q", "K", "A"
    private String suit;  // Hearts, Diamonds, Clubs, Spades
    private int value;    // points in blackjack (ace starts as 11)

    public Card(String rank, String suit, int value) {
        this.rank = rank;
        this.suit = suit;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean isAce() {
        return rank.equals("A");
    }

    // How the card is printed, like "K of Hearts"
    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}