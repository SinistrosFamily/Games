import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

// Loads your own card pictures (optional).
// Put PNG files in the folder "cards" with names like Hearts_A.png, Spades_10.png, back.png
// The folder "cards" can be in the folder where the game starts,
// or next to BlackJack.exe (or next to the .jar file).
public class CardImages {

    private HashMap<String, BufferedImage> images;
    private String foundIn; // the folder where the pictures were found

    public CardImages(String folder) {
        images = new HashMap<>();
        foundIn = "";

        // the places where we look for the folder, in this order
        File[] places = getPlaces(folder);

        for (File place : places) {
            loadAll(place);
            if (!images.isEmpty()) {
                foundIn = place.getAbsolutePath();
                break; // found pictures: no need to look in the other places
            }
        }
    }

    // A text for the game log: tells if the pictures were found and where
    public String getReport() {
        if (images.isEmpty()) {
            return "Card pictures: none found (optional: put PNG files in a folder called cards next to the game).";
        }
        return "Card pictures loaded: " + images.size() + " files from " + foundIn;
    }

    // The folders to look in
    private File[] getPlaces(String folder) {
        File programFolder = getProgramFolder();

        if (programFolder.isDirectory() && programFolder.getName().equals("app")
                && programFolder.getParentFile() != null) {
            // made with jpackage: BlackJack.exe is in the folder above "app"
            return new File[] {
                new File(folder),
                new File(programFolder.getParentFile(), folder),
                new File(programFolder, folder)
            };
        }
        return new File[] {
            new File(folder),
            new File(programFolder, folder)
        };
    }

    // The folder where the program is (where the .jar or the .class files are)
    private File getProgramFolder() {
        try {
            File location = new File(CardImages.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (location.isFile()) {
                return location.getParentFile(); // the program is a .jar file
            }
            return location; // the program is a folder with .class files
        } catch (Exception e) {
            return new File(".");
        }
    }

    // Looks for the 52 cards and for the back in one folder
    private void loadAll(File folder) {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

        for (String suit : suits) {
            for (String rank : ranks) {
                load(folder, suit + "_" + rank);
            }
        }
        load(folder, "back");
    }

    // Reads one file, if it exists
    private void load(File folder, String name) {
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