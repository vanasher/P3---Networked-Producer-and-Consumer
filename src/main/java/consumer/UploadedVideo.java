package consumer;

import com.google.protobuf.ByteString;
import java.util.List;

public class UploadedVideo {

    private final int producerId;
    private final String fileName;
    private final List<ByteString> chunks;

    public UploadedVideo(int producerId, String fileName, List<ByteString> chunks) {
        this.producerId = producerId;
        this.fileName = fileName;
        this.chunks = chunks;
    }

    public int getProducerId() {
        return producerId;
    }

    public String getFileName() {
        return fileName;
    }

    public List<ByteString> getChunks() {
        return chunks;
    }
}