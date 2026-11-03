// The dealer: follows a fixed rule (hits until 17 or more)
public class Dealer extends Participant {

    public Dealer() {
        super("Dealer");
    }

    // The dealer's first card is the one everybody can see
    public Card getUpCard() {
        return getFirstHand().getCard(0);
    }

    // At the start, the player only sees the first card
    public String showHidden() {
        return getName() + ": [" + getUpCard() + ", hidden card]";
    }

    public String showHand() {
        return getName() + ": " + getFirstHand();
    }

    @Override
    public void playTurn(Deck deck) {
        Hand hand = getFirstHand();

        while (hand.getTotal() < 17) {
            hand.addCard(deck.draw());
            System.out.println("Dealer hits: " + showHand());
        }

        if (hand.isBust()) {
            System.out.println("Dealer busts!");
        } else {
            System.out.println("Dealer stands with " + hand.getTotal() + ".");
        }
    }
}