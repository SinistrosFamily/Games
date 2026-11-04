import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// Run this class to play the game in a window
public class SwingBlackjack {

    public static void main(String[] args) {
        // use the look of the operating system (buttons look like Windows)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Could not use the system look. Using the default one.");
        }

        // Swing windows are created in the "event thread", the thread that handles clicks
        SwingUtilities.invokeLater(() -> {
            BlackjackWindow window = new BlackjackWindow();
            window.setVisible(true); // show the window
        });
    }
}