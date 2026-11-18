package producer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import shared.InputReader;

public class ProducerMain {

    private static final int GRPC_PORT = 50051;

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        int p = InputReader.readPositiveInt(scanner, "Number of producer threads (p): ");
        String host = InputReader.readText(scanner, "Consumer address (e.g. localhost): ");

        for (int i = 1; i <= p; i++) {
            File folder = new File("videos/thread" + i);
            if (!folder.isDirectory()) {
                System.out.println("Missing folder: " + folder.getPath());
                return;
            }
        }

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, GRPC_PORT)
                .usePlaintext()
                .build();

        List<ProducerThread> producers = new ArrayList<>();
        for (int i = 1; i <= p; i++) {
            File folder = new File("videos/producer" + i);
            ProducerThread producer = new ProducerThread(i, folder, channel);
            producers.add(producer);
            producer.start();
        }

        for (ProducerThread producer : producers) {
            producer.join();
        }

        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("All producers finished.");
    }
}