package producer;

import com.google.protobuf.ByteString;
import generated.MediaUploadGrpc;
import generated.UploadResult;
import generated.VideoChunk;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ProducerThread extends Thread {

    private static final int CHUNK_SIZE = 1024 * 1024;          
    private static final int RETRY_DELAY_MS = 3000;    

    private final int id;
    private final File folder;
    private final MediaUploadGrpc.MediaUploadStub stub;

    public ProducerThread(int id, File folder, ManagedChannel channel) {
        this.id = id;
        this.folder = folder;
        this.stub = MediaUploadGrpc.newStub(channel);
    }

    @Override
    public void run() {
        File[] files = folder.listFiles();

        if (files == null) {
            System.out.println("Producer " + id + " cannot read folder " + folder.getPath());
            return;
        }

        for (File file : files) {
            if (!isVideo(file)) {
                continue;
            }

            try {
                sendWithRetry(file);
            } catch (IOException e) {
                System.out.println("Producer " + id + " could not read " + file.getName());
            } catch (InterruptedException e) {
                System.out.println("Producer " + id + " stopped.");
                return;
            }
        }
    }

    private boolean isVideo(File file) {
        return file.getName().toLowerCase().endsWith(".mp4");
    }

        // sends one video, waiting and trying again for as long as the consumer's queue is full
    private void sendWithRetry(File video) throws IOException, InterruptedException {
        while (true) {
            Reply reply = sendVideo(video);

            if (reply.accepted) {
                System.out.println("Producer " + id + ": " + video.getName() + " was accepted");
                return;
            }

            if (!reply.queueFull) {
                System.out.println("Producer " + id + ": " + video.getName() + " was dropped (" + reply.message + ")");
                return;
            }

            System.out.println("Producer " + id + ": queue is full, waiting to retry " + video.getName());
            Thread.sleep(RETRY_DELAY_MS);
        }
    }

    private Reply sendVideo(File video) throws IOException, InterruptedException {
        Reply reply = new Reply();
        StreamObserver<VideoChunk> request = stub.upload(reply);
        ClientCallStreamObserver<VideoChunk> flow = (ClientCallStreamObserver<VideoChunk>) request;

        byte[] buffer = new byte[CHUNK_SIZE];

        try (FileInputStream input = new FileInputStream(video)) {
            int bytesRead = input.read(buffer);

            while (bytesRead != -1 && !reply.isFinished()) {
                
                while (!flow.isReady() && !reply.isFinished()) {
                    Thread.sleep(10);
                }
                if (reply.isFinished()) {
                    break;
                }

                VideoChunk chunk = VideoChunk.newBuilder()
                        .setProducerId(id)
                        .setFileName(video.getName())
                        .setData(ByteString.copyFrom(buffer, 0, bytesRead))
                        .build();
                request.onNext(chunk);

                bytesRead = input.read(buffer);
            }
        }

        if (!reply.isFinished()) {
            request.onCompleted();
        }
        reply.waitUntilFinished();

        return reply;
    }

    private static class Reply implements StreamObserver<UploadResult> {

        private final CountDownLatch finished = new CountDownLatch(1);
        private volatile boolean accepted = false;
        private volatile boolean queueFull = false;
        private volatile String message = "No reply";

        @Override
        public void onNext(UploadResult result) {
            accepted = result.getAccepted();
            queueFull = result.getQueueFull();
            message = result.getMessage();
        }

        @Override
        public void onError(Throwable error) {
            message = "Connection problem: " + error.getMessage();
            finished.countDown();
        }

        @Override
        public void onCompleted() {
            finished.countDown();
        }

        boolean isFinished() {
            return finished.getCount() == 0;
        }

        void waitUntilFinished() throws InterruptedException {
            finished.await();
        }
    }
}