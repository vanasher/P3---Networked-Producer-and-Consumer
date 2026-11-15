package producer;

import java.io.File;
import java.util.Scanner;
import shared.InputReader;

public class ProducerMain {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int p = InputReader.readPositiveInt(scanner, "Number of producer threads (p): ");

        for (int i = 1; i <= p; i++) {
            File folder = new File("videos/thread" + i);
            if (!folder.isDirectory()) {
                System.out.println("Missing folder: " + folder.getPath());
                return;
            }
        }

        for (int i = 1; i <= p; i++) {
            File folder = new File("videos/thread" + i);
            ProducerThread producer = new ProducerThread(i, folder);
            producer.start();
        }
    }
}