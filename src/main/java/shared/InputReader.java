package shared;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class InputReader {

    public static int readPositiveInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = scanner.nextLine().trim();

            try {
                int number = Integer.parseInt(text);
                if (number >= 1) {
                    return number;
                }
                System.out.println("Please enter a number that is at least 1.");
            } catch (NumberFormatException e) {
                if (text.matches("-?\\d+")) {
                    System.out.println("That number is out of range. Try again.");
                } else {
                    System.out.println("That is not a whole number. Try again.");
                }
            }
        }
    }

    public static String readHost(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = scanner.nextLine().trim();

            if (text.isEmpty() || text.contains(" ")) {
                System.out.println("Please enter an address with no spaces, like localhost or 192.168.1.5.");
                continue;
            }

            try {
                InetAddress.getByName(text);
                return text;
            } catch (UnknownHostException e) {
                System.out.println("Cannot find that address. Try again.");
            }
        }
    }
}