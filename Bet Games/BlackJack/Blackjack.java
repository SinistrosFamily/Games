// Still in Development
// Basic Terminal Blackjack Game
import java.util.Scanner;

public class Blackjack {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Your name: ");
        String name = scanner.nextLine();

        Game game = new Game(name, scanner, 500); // starting balance: $500
        game.play();

        scanner.close();
    }
}