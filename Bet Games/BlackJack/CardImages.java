import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

// Loads your own card pictures (optional).
// Put PNG files in the folder "cards" with names like Hearts_A.png, Spades_10.png, back.png
public class CardImages {

    private HashMap<String, BufferedImage> images;

    public CardImages(String folder) {
        images = new HashMap<>();

        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

        // looks for the 52 cards and for the back
        for (String suit : suits) {
            for (String rank : ranks) {
                load(folder, suit + "_" + rank);
            }
        }
        load(folder, "back");
    }

    // Reads one file, if it exists
    private void load(String folder, String name) {
        File file = new File(folder, name + ".png");
        if (!file.exists()) {
            return; // no picture: the card will be drawn by the code
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                images.put(name, image);
            }
        } catch (IOException e) {
            System.out.println("Could not read the picture " + file);
        }
    }

    // The picture of a card, or null if there is none
    public BufferedImage get(Card card) {
        return images.get(card.getSuit() + "_" + card.getRank());
    }

    // The picture of the back of the cards, or null if there is none
    public BufferedImage getBack() {
        return images.get("back");
    }

    public int getCount() {
        return images.size();
    }
}