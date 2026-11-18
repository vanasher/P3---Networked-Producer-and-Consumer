package consumer;

import com.google.protobuf.ByteString;
import generated.MediaUploadGrpc;
import generated.UploadResult;
import generated.VideoChunk;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UploadService extends MediaUploadGrpc.MediaUploadImplBase {

    private final VideoQueue queue;

    public UploadService(VideoQueue queue) {
        this.queue = queue;
    }

    @Override
    public StreamObserver<VideoChunk> upload(StreamObserver<UploadResult> responseObserver) {

        return new StreamObserver<VideoChunk>() {

            private int producerId;
            private String fileName;
            private final List<ByteString> chunks = new ArrayList<>();
            private boolean rejected = false;

            @Override
            public void onNext(VideoChunk chunk) {
                if (rejected) {
                    return;
                }

                if (fileName == null) {
                    producerId = chunk.getProducerId();
                    fileName = new File(chunk.getFileName()).getName();

                    if (queue.isFull()) {
                        rejected = true;
                        turnAway();
                        return;
                    }
                }

                chunks.add(chunk.getData());
            }

            @Override
            public void onError(Throwable error) {
                if (!rejected) {
                    System.out.println("Upload of " + fileName + " failed: " + error.getMessage());
                }
                chunks.clear();
            }

            @Override
            public void onCompleted() {
                if (rejected) {
                    return;
                }

                if (fileName == null) {
                    reply(false, false, "No data received");
                    return;
                }

                boolean accepted = queue.offer(new UploadedVideo(producerId, fileName, chunks));

                if (accepted) {
                    System.out.println("Queued " + fileName + " from producer " + producerId);
                    reply(true, false, "Queued");
                } else {
                    turnAway();
                }
            }

            private void turnAway() {
                System.out.println("Turned away " + fileName + " from producer " + producerId + " (queue full)");
                reply(false, true, "Queue is full");
            }

            private void reply(boolean accepted, boolean queueFull, String message) {
                UploadResult result = UploadResult.newBuilder()
                        .setAccepted(accepted)
                        .setQueueFull(queueFull)
                        .setMessage(message)
                        .build();
                responseObserver.onNext(result);
                responseObserver.onCompleted();
            }
        };
    }
}