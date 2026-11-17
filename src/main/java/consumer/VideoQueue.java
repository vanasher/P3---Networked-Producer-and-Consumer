package consumer;

import java.util.LinkedList;

public class VideoQueue {

    private final LinkedList<UploadedVideo> videos = new LinkedList<>();
    private final int capacity;

    public VideoQueue(int capacity) {
        this.capacity = capacity;
    }

    public synchronized boolean offer(UploadedVideo video) {
        if (videos.size() >= capacity) {
            return false;
        }
        videos.addLast(video);
        notify();
        return true;
    }
    
    public synchronized boolean isFull() {
        return videos.size() >= capacity;
    }

    public synchronized UploadedVideo take() throws InterruptedException {
        while (videos.isEmpty()) {
            wait();
        }
        return videos.removeFirst();
    }
}