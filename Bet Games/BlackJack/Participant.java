import java.util.ArrayList;

// Abstract class: anything that plays with cards (player or dealer)
public abstract class Participant {

    private String name;
    private ArrayList<Hand> hands; // a player can have two hands after a split

    public Participant(String name) {
        this.name = name;
        this.hands = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public ArrayList<Hand> getHands() {
        return hands;
    }

    public Hand getFirstHand() {
        return hands.get(0);
    }

    public void addHand(Hand hand) {
        hands.add(hand);
    }

    // Adds a hand in a given position of the list (used by the split)
    public void addHand(int position, Hand hand) {
        hands.add(position, hand);
    }

    public void clearHands() {
        hands.clear();
    }

    // Each type of participant plays its turn in its own way
    public abstract void playTurn(Deck deck);
}