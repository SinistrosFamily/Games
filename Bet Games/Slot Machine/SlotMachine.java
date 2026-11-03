// STILL IN DEVELOPMENT
import java.util.Random;
import java.util.Scanner;
public class SlotMachine {
    static Scanner scanner = new Scanner(System.in);
    static Random random = new Random();
    public static void main(String[] args) {
        double balance=100;
        double bet,payout;
        String[] row;
        String again;
        WelcomeMsg();
        while (balance>0){
            System.out.printf("Current Balance: $%,.2f\n",balance);
            System.out.print("Enter an bet: ");
            bet = scanner.nextDouble();
            scanner.nextLine();
            if(bet>balance){
                System.out.println("Insufficient funds!");
                continue;
            }
            else if(bet <=0){
                System.out.println("Bet must be greater than 0!");
                continue;
            }
            else{
                balance-=bet;
            }
            System.out.println("Spining...");
            row = spinRow();
            pRow(row);
            payout = getPayout(row,bet);
            if(payout>0){
                System.out.printf("You won $%.2f!\n",payout);
                balance+=payout;
            }
            else{
                System.out.println("You lost this round!");
            }
            System.out.print("Do you want to play again? (Y/N):");
            again = scanner.nextLine().toUpperCase();
            if(!again.equals("Y")){
                break;
            }
        };
        System.out.println("Thanks for playing!");
        scanner.close();
    }
    static void WelcomeMsg(){
        System.out.println("******************************");
        System.out.println("       Java Slot Machine!     ");
        System.out.println("  Symbols:  !  @  #  $  *  &  ");
        System.out.println("******************************\n");}
    static String[] spinRow(){
        //String[] symbols = {"💎","💠","💲","⭐","❤️","🪨"};
        String[] symbols = {"!","@","#","$","*","&"};
        String[] row = new String[3];
        for(int i=0;i<3;i++){
            row[i] = symbols[random.nextInt(symbols.length)];;
        }
        return row;}
    static void pRow(String[] row){
        System.out.println("**************");
        System.out.println(" "+String.join(" | ",row));
        System.out.println("**************");}
    static double getPayout(String[] row, double bet){
        if(row[0].equals(row[1]) && row[1].equals(row[2])){
            return switch(row[0]){
                case "!" -> bet * 2.5;
                case "@" -> bet * 5;
                case "#" -> bet * 7;
                case "$" -> bet * 10;
                case "*" -> bet * 15;
                case "&" -> bet * 20;
                default -> 0;
            };}
        else if(row[0].equals(row[1])){
            return switch(row[0]){
                case "!" -> bet * 1.25;
                case "@" -> bet * 2.5;
                case "#" -> bet * 3.5;
                case "$" -> bet * 5;
                case "*" -> bet * 7.5;
                case "&" -> bet * 10;
                default -> 0;
            };}
        else if(row[0].equals(row[2])){
            return switch(row[0]){
                case "!" -> bet * 1.25;
                case "@" -> bet * 2.5;
                case "#" -> bet * 3.5;
                case "$" -> bet * 5;
                case "*" -> bet * 7.5;
                case "&" -> bet * 10;
                default -> 0;
            };}
        else if(row[1].equals(row[2])){
            return switch(row[1]){
                case "!" -> bet * 1.25;
                case "@" -> bet * 2.5;
                case "#" -> bet * 3.5;
                case "$" -> bet * 5;
                case "*" -> bet * 7.5;
                case "&" -> bet * 10;
                default -> 0;
            };}
        return 0;
    }

}
