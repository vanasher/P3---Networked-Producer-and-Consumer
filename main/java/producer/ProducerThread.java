package producer;

import java.io.File;

public class ProducerThread extends Thread {

    private final int id;
    private final File folder;

    public ProducerThread(int id, File folder) {
        this.id = id;
        this.folder = folder;
    }

    @Override
    public void run() {
        File[] videos = folder.listFiles();

        // just printing video file name to cli for now
        for (File video : videos) {
            System.out.println("Producer " + id + " found: " + video.getName());
        }
    }
}