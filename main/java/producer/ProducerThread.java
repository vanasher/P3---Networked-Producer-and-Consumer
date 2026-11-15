package producer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ProducerThread extends Thread {

    private static final int CHUNK_SIZE = 1024 * 1024;

    private final int id;
    private final File folder;

    public ProducerThread(int id, File folder) {
        this.id = id;
        this.folder = folder;
    }

    @Override
    public void run() {
        File[] files = folder.listFiles();

        for (File file : files) {
            if (!isVideo(file)) {
                continue;
            }

            try {
                readInChunks(file);
            } catch (IOException e) {
                System.out.println("Producer " + id + " can't read " + file.getName());
            }
        }
    }

    private boolean isVideo(File file) {
        return (
            file.getName().toLowerCase().endsWith(".mp4") ||
            file.getName().toLowerCase().endsWith(".mp4")
        );
    }

    private void readInChunks(File video) throws IOException {
        byte[] buffer = new byte[CHUNK_SIZE];
        int chunkCount = 0;
        long totalBytes = 0;

        try (FileInputStream input = new FileInputStream(video)) {
            int bytesRead = input.read(buffer);

            while (bytesRead != -1) {
                chunkCount++;
                totalBytes += bytesRead;
                bytesRead = input.read(buffer);
            }
        }

        System.out.println("Producer " + id + ": " + video.getName()
                + " -> " + chunkCount + " chunks, " + totalBytes + " bytes");
    }
}