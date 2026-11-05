import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

// The green table: draws the deck and the cards of the dealer and of the player.
// New cards slide in from the deck.
public class CardPanel extends JPanel {

    private static final long serialVersionUID = 1L; // only to avoid a warning in VS Code

    public static final int DEAL_STAGGER = 150;  // milliseconds between the cards of a deal

    private static final int CARD_WIDTH = 80;
    private static final int CARD_HEIGHT = 112;
    private static final int CARD_GAP = 90;      // distance between the cards of a hand
    private static final int SLIDE_TIME = 350;   // milliseconds a card takes to arrive
    private static final int DECK_Y = 45;        // the deck is at the top right

    private GameTable table;
    private CardImages cardImages;
    private HashMap<Card, Long> startTimes; // the moment each card starts to slide
    private boolean animating;              // true while a card is still moving

    public CardPanel(GameTable table, CardImages cardImages) {
        this.table = table;
        this.cardImages = cardImages;
        this.startTimes = new HashMap<>();

        setPreferredSize(new Dimension(640, 450));
        setBackground(new Color(0, 110, 50)); // green felt

        // about 60 times per second: if a card is moving, draw the table again
        Timer animationTimer = new Timer(16, event -> {
            if (animating) {
                repaint();
            }
        });
        animationTimer.start();
    }

    // Swing calls this method every time the panel must be drawn
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // paints the green background
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        animating = false;     // drawing the cards sets it to true again if one is still moving
        registerNewCards();

        drawDeck(g2);
        drawDealer(g2);
        drawPlayer(g2);
        drawSideBets(g2);
    }

    // ---------- the animation ----------

    // Gives a start time to each card that was not on the table before.
    // Several new cards start one after the other (DEAL_STAGGER).
    private void registerNewCards() {
        if (table.getPlayerHands().isEmpty()) {
            startTimes.clear(); // empty table: forget the old cards
        }

        int newCards = 0;
        for (Hand hand : table.getPlayerHands()) {
            for (int i = 0; i < hand.getSize(); i++) {
                newCards = register(hand.getCard(i), newCards);
            }
        }
        Hand dealerHand = table.getDealerHand();
        for (int i = 0; i < dealerHand.getSize(); i++) {
            newCards = register(dealerHand.getCard(i), newCards);
        }
    }

    // Returns how many new cards we have found so far
    private int register(Card card, int newCards) {
        if (startTimes.containsKey(card)) {
            return newCards;
        }
        startTimes.put(card, System.currentTimeMillis() + (long) newCards * DEAL_STAGGER);
        return newCards + 1;
    }

    // Draws a card in its place, or on its way from the deck
    private void drawCardAnimated(Graphics2D g2, Card card, int x, int y, boolean fullyVisible, boolean faceDown) {
        long elapsed = System.currentTimeMillis() - startTimes.get(card);

        if (elapsed < 0) { // not its turn yet: it is still in the deck
            animating = true;
            return;
        }

        int drawX = x;
        int drawY = y;
        if (elapsed < SLIDE_TIME) { // on its way
            animating = true;
            double progress = (double) elapsed / SLIDE_TIME;
            double smooth = 1 - (1 - progress) * (1 - progress); // starts fast, arrives slowly
            int deckX = getDeckX();
            drawX = (int) (deckX + (x - deckX) * smooth);
            drawY = (int) (DECK_Y + (y - DECK_Y) * smooth);
        }

        if (faceDown) {
            drawCardBack(g2, drawX, drawY);
        } else {
            drawCard(g2, card, drawX, drawY, fullyVisible);
        }
    }

    private int getDeckX() {
        return getWidth() - CARD_WIDTH - 24;
    }

    // A small pile of cards at the top right
    private void drawDeck(Graphics2D g2) {
        for (int i = 2; i >= 0; i--) {
            drawCardBack(g2, getDeckX() - i * 2, DECK_Y - i * 2);
        }
    }

    // ---------- the cards of each one ----------

    private void drawDealer(Graphics2D g2) {
        Hand dealerHand = table.getDealerHand();
        int cardsY = 45;

        // title above the cards
        String title = "Dealer";
        if (dealerHand.getSize() > 0 && !table.isHoleHidden()) {
            title += " - " + dealerHand.getTotal();
            if (dealerHand.isBust()) {
                title += " (bust)";
            }
        }
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        drawCentered(g2, title, 0, getWidth(), 28);

        // the cards, centered
        int gap = getGap(dealerHand.getSize(), getWidth());
        int startX = getStartX(dealerHand.getSize(), gap, 0, getWidth());

        for (int i = 0; i < dealerHand.getSize(); i++) {
            boolean faceDown = i == 1 && table.isHoleHidden(); // the hidden card
            drawCardAnimated(g2, dealerHand.getCard(i), startX + i * gap, cardsY,
                    isFullyVisible(i, dealerHand.getSize(), gap), faceDown);
        }
    }

    private void drawPlayer(Graphics2D g2) {
        ArrayList<Hand> hands = table.getPlayerHands();

        if (hands.isEmpty()) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 24));
            drawCentered(g2, "Place your bet and press Deal", 0, getWidth(), getHeight() / 2 + 40);
            return;
        }

        // the hands stay at the bottom, with room for two lines of text below them
        int cardsY = getHeight() - CARD_HEIGHT - 80;

        // one hand uses the whole width, two hands share it
        int areaWidth = getWidth();
        if (hands.size() > 1) {
            areaWidth = getWidth() / 2;
        }

        for (int i = 0; i < hands.size(); i++) {
            Hand hand = hands.get(i);
            int areaX = i * areaWidth;

            int gap = getGap(hand.getSize(), areaWidth);
            int startX = getStartX(hand.getSize(), gap, areaX, areaWidth);
            int handWidth = CARD_WIDTH + (hand.getSize() - 1) * gap;

            // yellow frame around the hand being played
            if (table.getPhase() == Phase.PLAYING && i == table.getCurrentHand()) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(startX - 8, cardsY - 8, handWidth + 16, CARD_HEIGHT + 16, 16, 16);
            }

            for (int c = 0; c < hand.getSize(); c++) {
                drawCardAnimated(g2, hand.getCard(c), startX + c * gap, cardsY,
                        isFullyVisible(c, hand.getSize(), gap), false);
            }

            // text under the cards
            String title = "Bet $" + hand.getBet() + " - total " + hand.getTotal();
            if (hands.size() > 1) {
                title = "Hand " + (i + 1) + " - " + title;
            }
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            drawCentered(g2, title, areaX, areaWidth, cardsY + CARD_HEIGHT + 26);

            // result of the hand, when the round is over
            String result = table.getHandResult(i);
            if (!result.isEmpty()) {
                if (result.startsWith("WIN") || result.startsWith("BLACKJACK")) {
                    g2.setColor(Color.YELLOW);
                } else if (result.startsWith("PUSH")) {
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(new Color(255, 160, 160)); // light red
                }
                g2.setFont(new Font("SansSerif", Font.BOLD, 20));
                drawCentered(g2, result, areaX, areaWidth, cardsY + CARD_HEIGHT + 52);
            }
        }
    }

    // Small text in the top right corner with the side bets of the round
    private void drawSideBets(Graphics2D g2) {
        if (table.getPlayerHands().isEmpty()) {
            return;
        }
        String text = "";
        if (table.getPairBet() > 0) {
            text += "Pair bet: $" + table.getPairBet() + "   ";
        }
        if (table.getInsuranceBet() > 0) {
            text += "Insurance: $" + table.getInsuranceBet();
        }
        g2.setColor(new Color(220, 255, 220));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, getWidth() - metrics.stringWidth(text) - 16, 24);
    }

    // ---------- small helpers for the positions ----------

    // Distance between the cards: they get closer when the hand has many cards
    private int getGap(int numberOfCards, int areaWidth) {
        if (numberOfCards <= 1) {
            return CARD_GAP;
        }
        int maxGap = (areaWidth - 40 - CARD_WIDTH) / (numberOfCards - 1);
        return Math.min(CARD_GAP, maxGap);
    }

    // X of the first card, so the whole hand is centered in its area
    private int getStartX(int numberOfCards, int gap, int areaX, int areaWidth) {
        int handWidth = CARD_WIDTH + Math.max(numberOfCards - 1, 0) * gap;
        return areaX + (areaWidth - handWidth) / 2;
    }

    // The last card is always fully visible. The others only when the cards do not overlap.
    private boolean isFullyVisible(int index, int numberOfCards, int gap) {
        return index == numberOfCards - 1 || gap >= CARD_WIDTH;
    }

    // Draws a text centered in an area (from areaX, with areaWidth)
    private void drawCentered(Graphics2D g2, String text, int areaX, int areaWidth, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        int x = areaX + (areaWidth - metrics.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }

    // ---------- drawing a card (change this part to change how the cards look) ----------

    private void drawCard(Graphics2D g2, Card card, int x, int y, boolean fullyVisible) {
        // if there is a picture file for this card, use it
        BufferedImage picture = cardImages.get(card);
        if (picture != null) {
            g2.drawImage(picture, x, y, CARD_WIDTH, CARD_HEIGHT, null);
            return;
        }

        // white card with a gray border
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);

        // hearts and diamonds are red, clubs and spades are black
        if (card.isRed()) {
            g2.setColor(new Color(200, 0, 0));
        } else {
            g2.setColor(Color.BLACK);
        }

        String symbol = getSuitSymbol(card.getSuit());

        // rank and small suit in the corner (this part is always visible)
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.drawString(card.getRank(), x + 7, y + 23);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g2.drawString(symbol, x + 7, y + 44);

        // big suit in the middle (only when it is not covered by another card)
        if (fullyVisible) {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 44));
            FontMetrics metrics = g2.getFontMetrics();
            int symbolX = x + (CARD_WIDTH - metrics.stringWidth(symbol)) / 2;
            g2.drawString(symbol, symbolX, y + 82);
        }
    }

    // The back of a card (face down)
    private void drawCardBack(Graphics2D g2, int x, int y) {
        // if there is a picture file for the back, use it
        BufferedImage picture = cardImages.getBack();
        if (picture != null) {
            g2.drawImage(picture, x, y, CARD_WIDTH, CARD_HEIGHT, null);
            return;
        }

        g2.setColor(new Color(30, 60, 160));
        g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
        g2.setColor(new Color(110, 140, 220));
        g2.drawRoundRect(x + 8, y + 8, CARD_WIDTH - 16, CARD_HEIGHT - 16, 6, 6);
    }

    // The symbols are written with unicode codes, so the file works in any encoding
    private String getSuitSymbol(String suit) {
        if (suit.equals("Hearts")) {
            return "\u2665";
        } else if (suit.equals("Diamonds")) {
            return "\u2666";
        } else if (suit.equals("Clubs")) {
            return "\u2663";
        } else {
            return "\u2660"; // Spades
        }
    }
}