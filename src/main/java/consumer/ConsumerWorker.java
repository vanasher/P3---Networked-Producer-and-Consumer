package consumer;

import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ConsumerWorker extends Thread {

    private final int id;
    private final VideoQueue queue;
    private final File uploadsFolder;

    public ConsumerWorker(int id, VideoQueue queue, File uploadsFolder) {
        this.id = id;
        this.queue = queue;
        this.uploadsFolder = uploadsFolder;
    }

    @Override
    public void run() {
        try {
            while (true) {
                UploadedVideo video = queue.take();
                save(video);
            }
        } catch (InterruptedException e) {
            System.out.println("Consumer " + id + " stopped.");
        }
    }

    private void save(UploadedVideo video) {
        
        String finalName = "producer" + video.getProducerId() + "_" + video.getFileName();
        File tempFile = new File(uploadsFolder, finalName + ".part");
        File finalFile = new File(uploadsFolder, finalName);

        try (FileOutputStream output = new FileOutputStream(tempFile)) {
            for (ByteString chunk : video.getChunks()) {
                chunk.writeTo(output);
            }
        } catch (IOException e) {
            System.out.println("Consumer " + id + " could not save " + finalName);
            tempFile.delete();
            return;
        }

        try {
            Files.move(tempFile.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Consumer " + id + " saved " + finalName);
        } catch (IOException e) {
            System.out.println("Consumer " + id + " could not rename " + finalName);
        }
    }
}