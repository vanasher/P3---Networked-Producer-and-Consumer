package consumer;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;
import shared.InputReader;

public class ConsumerMain {

    private static final int GRPC_PORT = 50051;
    private static final int GUI_PORT = 8080;

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        int c = InputReader.readPositiveInt(scanner, "Number of consumer threads (c): ");
        int q = InputReader.readPositiveInt(scanner, "Max queue length (q): ");

        File uploadsFolder = new File("uploads");
        if (!uploadsFolder.isDirectory() && !uploadsFolder.mkdirs()) {
            System.out.println("Cannot create the uploads folder: " + uploadsFolder.getAbsolutePath());
            return;
        }

        VideoQueue queue = new VideoQueue(q);

        Server server = null;
        try {
            server = ServerBuilder.forPort(GRPC_PORT)
                    .addService(new UploadService(queue))
                    .build()
                    .start();
            System.out.println("Consumer listening for uploads on port " + GRPC_PORT);

            WebGui.start(Paths.get("uploads"), GUI_PORT);
        } catch (IOException e) {
            System.out.println("Could not start. Is another copy of the consumer already running on port "
                    + GRPC_PORT + " or " + GUI_PORT + "?");
            if (server != null) {
                server.shutdownNow();
            }
            return;
        }

        for (int i = 1; i <= c; i++) {
            new ConsumerWorker(i, queue, uploadsFolder).start();
        }

        server.awaitTermination();
    }
}