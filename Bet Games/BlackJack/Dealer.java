// The dealer: follows a fixed rule (hits until 17 or more)
public class Dealer extends Participant {

    public Dealer() {
        super("Dealer");
    }

    @Override
    public boolean wantsToHit() {
        return getTotal() < 17;
    }

    // At the start, the player only sees the first card
    public String showHidden() {
        return getName() + ": [" + getCard(0) + ", hidden card]";
    }
}
