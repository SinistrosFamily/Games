import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

// The window: shows the table, the log and the buttons.
// All the rules are in GameTable. Here we only call it and show the result.
public class BlackjackWindow extends JFrame {

    private static final long serialVersionUID = 1L; // only to avoid a warning in VS Code

    private GameTable table;
    private CardPanel cardPanel;

    private JLabel balanceLabel;
    private JLabel statsLabel;
    private JLabel errorLabel;
    private JTextArea logArea;

    private JTextField betField;
    private JTextField pairField;
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
        cardPanel = new CardPanel(table);

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

        refresh();
    }

    // ---------- building the screen ----------

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        balanceLabel = new JLabel();
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        statsLabel = new JLabel();

        bar.add(balanceLabel, BorderLayout.WEST);
        bar.add(statsLabel, BorderLayout.EAST);
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
        hitButton = new JButton("Hit");
        hitButton.addActionListener(event -> onHit());
        standButton = new JButton("Stand");
        standButton.addActionListener(event -> onStand());
        doubleButton = new JButton("Double");
        doubleButton.addActionListener(event -> onDouble());
        splitButton = new JButton("Split");
        splitButton.addActionListener(event -> onSplit());
        surrenderButton = new JButton("Surrender");
        surrenderButton.addActionListener(event -> onSurrender());

        JPanel moveRow = new JPanel(new FlowLayout());
        moveRow.add(hitButton);
        moveRow.add(standButton);
        moveRow.add(doubleButton);
        moveRow.add(splitButton);
        moveRow.add(surrenderButton);

        // row 3: insurance and next round
        insuranceButton = new JButton("Insurance");
        insuranceButton.addActionListener(event -> onInsurance());
        noInsuranceButton = new JButton("No insurance");
        noInsuranceButton.addActionListener(event -> onNoInsurance());
        nextButton = new JButton("Next round");
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
    }

    // Each button is enabled only when its move is allowed
    private void updateButtons() {
        Phase phase = table.getPhase();
        boolean betting = phase == Phase.BETTING;
        boolean playing = phase == Phase.PLAYING;

        betField.setEnabled(betting);
        pairField.setEnabled(betting);
        dealButton.setEnabled(betting);

        hitButton.setEnabled(playing);
        standButton.setEnabled(playing);
        doubleButton.setEnabled(table.canDouble());
        splitButton.setEnabled(table.canSplit());
        surrenderButton.setEnabled(table.canSurrender());

        boolean insurance = phase == Phase.INSURANCE;
        insuranceButton.setEnabled(insurance);
        noInsuranceButton.setEnabled(insurance);
        if (insurance) {
            insuranceButton.setText("Insurance ($" + table.getInsuranceCost() + ")");
        } else {
            insuranceButton.setText("Insurance");
        }

        nextButton.setEnabled(phase == Phase.ROUND_OVER);
        if (table.getBalance() <= 0) {
            nextButton.setText("New game ($" + GameTable.STARTING_BALANCE + ")");
        } else {
            nextButton.setText("Next round");
        }
    }
}