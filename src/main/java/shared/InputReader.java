package shared;

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
                System.out.println("That is not a whole number. Try again.");
            }
        }
    }

    public static String readText(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = scanner.nextLine().trim();

            if (!text.isEmpty() && !text.contains(" ")) {
                return text;
            }
            System.out.println("Please enter an address like localhost or 192.168.1.5.");
        }
    }
}