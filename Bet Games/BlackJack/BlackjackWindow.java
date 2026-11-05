import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

// The window: shows the table, the log and the buttons.
// All the rules are in GameTable. Here we only call it and show the result.
public class BlackjackWindow extends JFrame {

    private static final long serialVersionUID = 1L; // only to avoid a warning in VS Code

    // the dealer speed: milliseconds between the dealer's cards
    private static final String[] SPEED_NAMES = {"Slow", "Normal", "Fast"};
    private static final int[] SPEED_DELAYS = {1200, 800, 400};
    private static final String[] BALANCE_OPTIONS = {"100", "250", "500", "1000", "5000", "10000"};

    private GameTable table;
    private CardPanel cardPanel;
    private SoundPlayer sound;
    private Timer dealerTimer;    // repeats the dealer's steps
    private int speedIndex;       // 0 = Slow, 1 = Normal, 2 = Fast
    private boolean dialogOpen;   // true while the settings window is open
    private int lastCardCount;    // to know when new cards appear
    private Phase lastPhase;      // to know when the round ends

    private JLabel balanceLabel;
    private JLabel statsLabel;
    private JLabel errorLabel;
    private JTextArea logArea;

    private JTextField betField;
    private JTextField pairField;
    private JButton settingsButton;
    private JButton dealButton;
    private JButton hitButton;
    private JButton standButton;
    private JButton doubleButton;
    private JButton splitButton;
    private JButton surrenderButton;
    private JButton insuranceButton;
    private JButton noInsuranceButton;
    private JButton nextButton;

    public BlackjackWindow() {
        super("Blackjack"); // the title of the window

        table = new GameTable();
        sound = new SoundPlayer();
        CardImages cardImages = new CardImages("cards"); // your own pictures, if the folder exists
        cardPanel = new CardPanel(table, cardImages);
        speedIndex = 1;
        lastCardCount = 0;
        lastPhase = Phase.BETTING;

        // BorderLayout: top, center, right and bottom
        add(createTopBar(), BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        add(createLogArea(), BorderLayout.EAST);
        add(createControls(), BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE); // closing the window ends the program
        setResizable(false);                     // keep this line before pack()
        pack();                                  // the window gets the size it needs
        setLocationRelativeTo(null);             // center on the screen
        getRootPane().setDefaultButton(dealButton); // the Enter key presses Deal

        // the timer calls onDealerTick() again and again, with the speed of the settings
        dealerTimer = new Timer(SPEED_DELAYS[speedIndex], event -> onDealerTick());
        addKeyboardShortcuts();

        logArea.append(cardImages.getReport() + "\n"); // tells if the card pictures were found
        
        refresh();
    }

    // ---------- building the screen ----------

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        balanceLabel = new JLabel();
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        statsLabel = new JLabel("", JLabel.CENTER);

        settingsButton = new JButton("Settings");
        settingsButton.addActionListener(event -> onSettings());

        bar.add(balanceLabel, BorderLayout.WEST);
        bar.add(statsLabel, BorderLayout.CENTER);
        bar.add(settingsButton, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane createLogArea() {
        logArea = new JTextArea();
        logArea.setEditable(false);   // the player cannot type here
        logArea.setFocusable(false);  // no blinking cursor in the log
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(logArea); // adds a scroll bar when needed
        scroll.setPreferredSize(new Dimension(260, 100));
        scroll.setBorder(BorderFactory.createTitledBorder("Game log"));
        return scroll;
    }

    private JPanel createControls() {
        JPanel controls = new JPanel(new GridLayout(4, 1));

        // row 0: error messages
        errorLabel = new JLabel(" ", JLabel.CENTER);
        errorLabel.setForeground(new Color(190, 0, 0));

        // row 1: the bets
        betField = new JTextField("10", 5);
        pairField = new JTextField("0", 5);
        dealButton = new JButton("Deal");
        dealButton.addActionListener(event -> onDeal());

        JPanel betRow = new JPanel(new FlowLayout());
        betRow.add(new JLabel("Bet: $"));
        betRow.add(betField);
        betRow.add(new JLabel("Pair side bet: $"));
        betRow.add(pairField);
        betRow.add(dealButton);

        // row 2: the moves of the player
        hitButton = new JButton("Hit (H)");
        hitButton.addActionListener(event -> onHit());
        standButton = new JButton("Stand (S)");
        standButton.addActionListener(event -> onStand());
        doubleButton = new JButton("Double (D)");
        doubleButton.addActionListener(event -> onDouble());
        splitButton = new JButton("Split (P)");
        splitButton.addActionListener(event -> onSplit());
        surrenderButton = new JButton("Surrender (R)");
        surrenderButton.addActionListener(event -> onSurrender());

        JPanel moveRow = new JPanel(new FlowLayout());
        moveRow.add(hitButton);
        moveRow.add(standButton);
        moveRow.add(doubleButton);
        moveRow.add(splitButton);
        moveRow.add(surrenderButton);

        // row 3: insurance and next round
        insuranceButton = new JButton("Insurance (I)");
        insuranceButton.addActionListener(event -> onInsurance());
        noInsuranceButton = new JButton("No insurance (N)");
        noInsuranceButton.addActionListener(event -> onNoInsurance());
        nextButton = new JButton("Next round (Enter)");
        nextButton.addActionListener(event -> onNext());

        JPanel extraRow = new JPanel(new FlowLayout());
        extraRow.add(insuranceButton);
        extraRow.add(noInsuranceButton);
        extraRow.add(nextButton);

        controls.add(errorLabel);
        controls.add(betRow);
        controls.add(moveRow);
        controls.add(extraRow);
        return controls;
    }

    // ---------- what happens when a button is clicked ----------

    private void onDeal() {
        clearError();
        try {
            int bet = Integer.parseInt(betField.getText().trim());

            // an empty pair field means "no side bet"
            String pairText = pairField.getText().trim();
            int pair = 0;
            if (!pairText.isEmpty()) {
                pair = Integer.parseInt(pairText);
            }

            table.startRound(bet, pair); // may throw an exception

        } catch (NumberFormatException e) {
            showError("Please type whole numbers in the bet fields.");

        } catch (InsufficientFundsException e) {
            showError(e.getMessage());

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
        refresh();
    }

    private void onHit() {
        clearError();
        table.hit();
        refresh();
    }

    private void onStand() {
        clearError();
        table.stand();
        refresh();
    }

    private void onDouble() {
        clearError();
        try {
            table.doubleDown();
        } catch (InsufficientFundsException e) {
            showError(e.getMessage());
        }
        refresh();
    }

    private void onSplit() {
        clearError();
        try {
            table.split();
        } catch (InsufficientFundsException e) {
            showError(e.getMessage());
        }
        refresh();
    }

    private void onSurrender() {
        clearError();
        table.surrender();
        refresh();
    }

    private void onInsurance() {
        clearError();
        try {
            table.takeInsurance();
        } catch (InsufficientFundsException e) {
            showError(e.getMessage());
        }
        refresh();
    }

    private void onNoInsurance() {
        clearError();
        table.declineInsurance();
        refresh();
    }

    private void onNext() {
        clearError();
        if (table.getBalance() <= 0) {
            table.newGame(); // no money left: start again
        } else {
            table.nextRound();
        }
        refresh();
        betField.requestFocusInWindow(); // ready to type the next bet
        betField.selectAll();
    }

    // Opens the settings window
    private void onSettings() {
        JComboBox<String> balanceBox = new JComboBox<>(BALANCE_OPTIONS);
        balanceBox.setSelectedItem(String.valueOf(table.getInitialBalance()));
        JComboBox<String> speedBox = new JComboBox<>(SPEED_NAMES);
        speedBox.setSelectedIndex(speedIndex);
        JCheckBox soundBox = new JCheckBox("On", sound.isEnabled());

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.add(new JLabel("Starting balance ($):"));
        panel.add(balanceBox);
        panel.add(new JLabel("Dealer speed:"));
        panel.add(speedBox);
        panel.add(new JLabel("Sound effects:"));
        panel.add(soundBox);
        panel.add(new JLabel("A new balance starts a new game."));

        dialogOpen = true; // the keyboard shortcuts sleep while the window is open
        int answer = JOptionPane.showConfirmDialog(this, panel, "Settings",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        dialogOpen = false;

        if (answer == JOptionPane.OK_OPTION) {
            int balance = Integer.parseInt((String) balanceBox.getSelectedItem());
            applySettings(balance, speedBox.getSelectedIndex(), soundBox.isSelected());
        }
    }

    // Uses the chosen settings
    private void applySettings(int initialBalance, int newSpeedIndex, boolean soundOn) {
        sound.setEnabled(soundOn);

        speedIndex = newSpeedIndex;
        dealerTimer.setDelay(SPEED_DELAYS[speedIndex]);
        dealerTimer.setInitialDelay(SPEED_DELAYS[speedIndex]);

        // a different starting balance starts a new game
        if (initialBalance != table.getInitialBalance()) {
            table.setInitialBalance(initialBalance);
            table.newGame();
        }
        refresh();
    }

    // Called by the timer: the dealer does one step (reveal, hit or finish)
    private void onDealerTick() {
        boolean moreSteps = table.dealerStep();
        if (!moreSteps) {
            dealerTimer.stop(); // the dealer is done
        }
        refresh();
    }

    // ---------- keyboard shortcuts ----------

    // Listens to the keys of the whole window
    private void addKeyboardShortcuts() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (event.getID() == KeyEvent.KEY_PRESSED && !dialogOpen) {
                return handleKey(event.getKeyCode()); // true = the key was used, nobody else gets it
            }
            return false; // false = the key goes on as usual (typing in the text fields)
        });
    }

    // Returns true if the key pressed a button
    private boolean handleKey(int keyCode) {
        if (keyCode == KeyEvent.VK_H) {
            return press(hitButton);
        } else if (keyCode == KeyEvent.VK_S) {
            return press(standButton);
        } else if (keyCode == KeyEvent.VK_D) {
            return press(doubleButton);
        } else if (keyCode == KeyEvent.VK_P) {
            return press(splitButton);
        } else if (keyCode == KeyEvent.VK_R) {
            return press(surrenderButton);
        } else if (keyCode == KeyEvent.VK_I) {
            return press(insuranceButton);
        } else if (keyCode == KeyEvent.VK_N) {
            return press(noInsuranceButton);
        } else if (keyCode == KeyEvent.VK_ENTER) {
            return press(nextButton); // not used during the betting: the Deal button gets Enter
        }
        return false;
    }

    // Clicks the button, but only if it is enabled. Returns true if it clicked.
    private boolean press(JButton button) {
        if (button.isEnabled()) {
            button.doClick();
            return true;
        }
        return false;
    }

    // ---------- updating the screen ----------

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    // Called after every click: shows the news and updates labels, buttons and table
    private void refresh() {
        // new messages go to the log
        for (String message : table.getMessages()) {
            logArea.append(message + "\n");
        }
        table.clearMessages();
        logArea.setCaretPosition(logArea.getDocument().getLength()); // scroll to the end

        balanceLabel.setText("Balance: $" + table.getBalance());
        statsLabel.setText("Hands - Won: " + table.getHandsWon()
                + " | Lost: " + table.getHandsLost()
                + " | Ties: " + table.getHandsTied());

        updateButtons();
        cardPanel.repaint(); // draws the table again
        playSounds();

        // the dealer's turn starts: the timer plays it step by step
        if (table.getPhase() == Phase.DEALER_TURN && !dealerTimer.isRunning()) {
            dealerTimer.start();
        }
    }

    // Plays the sounds for what just happened on the table
    private void playSounds() {
        // new cards on the table: one card sound for each (the same rhythm as the animation)
        int cards = countCards();
        if (cards > lastCardCount) {
            sound.playCards(cards - lastCardCount, CardPanel.DEAL_STAGGER);
        }
        lastCardCount = cards;

        // the round just ended: a sound for the result
        Phase phase = table.getPhase();
        if (phase == Phase.ROUND_OVER && lastPhase != Phase.ROUND_OVER) {
            if (table.getHandResult(0).startsWith("BLACKJACK")) {
                sound.playBlackjack();
            } else if (table.getRoundNet() > 0) {
                sound.playWin();
            } else if (table.getRoundNet() < 0) {
                sound.playLose();
            } else {
                sound.playPush();
            }
        }
        lastPhase = phase;
    }

    // All the cards that are on the table now
    private int countCards() {
        int cards = table.getDealerHand().getSize();
        for (Hand hand : table.getPlayerHands()) {
            cards += hand.getSize();
        }
        return cards;
    }

    // Each button is enabled only when its move is allowed
    private void updateButtons() {
        Phase phase = table.getPhase();
        boolean betting = phase == Phase.BETTING;
        boolean playing = phase == Phase.PLAYING;

        betField.setEnabled(betting);
        pairField.setEnabled(betting);
        dealButton.setEnabled(betting);
        settingsButton.setEnabled(betting || phase == Phase.ROUND_OVER);

        hitButton.setEnabled(playing);
        standButton.setEnabled(playing);
        doubleButton.setEnabled(table.canDouble());
        splitButton.setEnabled(table.canSplit());
        surrenderButton.setEnabled(table.canSurrender());

        boolean insurance = phase == Phase.INSURANCE;
        insuranceButton.setEnabled(insurance);
        noInsuranceButton.setEnabled(insurance);
        if (insurance) {
            insuranceButton.setText("Insurance $" + table.getInsuranceCost() + " (I)");
        } else {
            insuranceButton.setText("Insurance (I)");
        }

        nextButton.setEnabled(phase == Phase.ROUND_OVER);
        if (table.getBalance() <= 0) {
            nextButton.setText("New game $" + table.getInitialBalance() + " (Enter)");
        } else {
            nextButton.setText("Next round (Enter)");
        }
    }
}