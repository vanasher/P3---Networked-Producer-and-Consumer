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

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        int c = InputReader.readPositiveInt(scanner, "Number of consumer threads (c): ");
        int q = InputReader.readPositiveInt(scanner, "Max queue length (q): ");

        File uploadsFolder = new File("uploads");
        uploadsFolder.mkdirs();

        VideoQueue queue = new VideoQueue(q);

        for (int i = 1; i <= c; i++) {
            new ConsumerWorker(i, queue, uploadsFolder).start();
        }

        Server server = ServerBuilder.forPort(GRPC_PORT)
                .addService(new UploadService(queue))
                .build()
                .start();
        System.out.println("Consumer listening for uploads on port " + GRPC_PORT);

        WebGui.start(Paths.get("uploads"), GUI_PORT);

        server.awaitTermination();
    }
}