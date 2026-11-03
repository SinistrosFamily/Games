import java.util.ArrayList;
import java.util.Collections;

// A shoe: several decks mixed together
public class Deck {

    private ArrayList<Card> cards;
    private int numberOfDecks;

    public Deck(int numberOfDecks) {
        this.numberOfDecks = numberOfDecks;
        reshuffle();
    }

    // Builds all the cards again and shuffles them
    public void reshuffle() {
        cards = new ArrayList<>();

        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        int[] values   = {2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11};

        // repeat the 52 cards once for each deck in the shoe
        for (int deckNumber = 0; deckNumber < numberOfDecks; deckNumber++) {
            for (String suit : suits) {
                for (int i = 0; i < ranks.length; i++) {
                    cards.add(new Card(ranks[i], suit, values[i]));
                }
            }
        }

        Collections.shuffle(cards); // mix the cards
    }

    // True when only a quarter of the cards is left
    public boolean needsShuffle() {
        return cards.size() < numberOfDecks * 52 / 4;
    }

    public int cardsLeft() {
        return cards.size();
    }

    // Takes the last card of the shoe
    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("The deck is empty!");
        }
        return cards.remove(cards.size() - 1);
    }
}