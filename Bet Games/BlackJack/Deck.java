import java.util.ArrayList;
import java.util.Collections;

// A shuffled deck of 52 cards
public class Deck{
    private ArrayList<Card> cards;
    public Deck() {
        cards = new ArrayList<>();

        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        int[] values   = {2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11};

        // create every rank for every suit
        for (String suit : suits) {
            for (int i = 0; i < ranks.length; i++) {
                cards.add(new Card(ranks[i], suit, values[i]));
            }
        }

        Collections.shuffle(cards); // mix the cards
    }

    // Takes the last card of the deck
    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("The deck is empty!");
        }
        return cards.remove(cards.size() - 1);
    }
}