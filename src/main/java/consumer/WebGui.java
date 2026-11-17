package consumer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Browser GUI for the consumer. Shows every video in the uploads folder, plays the first 10 seconds
 * on mouse hover, and plays the full video on click.
 *
 * The GUI only reads the uploads folder, so it does not depend on the consumer's queue or threads.
 * Start it from the consumer with WebGui.start(Paths.get("uploads"), 8080),
 * or alone with: java -cp target/classes consumer.WebGui [uploadsFolder] [port]
 */
public class WebGui {

    private static final Map<String, String> TYPES = Map.of(
            "mp4", "video/mp4",
            "m4v", "video/mp4",
            "webm", "video/webm",
            "mov", "video/quicktime");

    private static final Pattern RANGE = Pattern.compile("bytes=(\\d*)-(\\d*)");

    private final Path folder;

    private WebGui(Path folder) {
        this.folder = folder.toAbsolutePath().normalize();
    }

    /** Starts the GUI web server on its own threads and returns at once. */
    public static HttpServer start(Path uploadsFolder, int port) throws IOException {
        Files.createDirectories(uploadsFolder);
        WebGui gui = new WebGui(uploadsFolder);
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", gui::page);
        server.createContext("/api/videos", gui::list);
        server.createContext("/videos/", gui::video);
        // Each browser tab streams several videos at once, so each request gets its own thread.
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("GUI running at http://localhost:" + port + " (folder: " + gui.folder + ")");
        return server;
    }

    public static void main(String[] args) throws IOException {
        Path folder = Paths.get(args.length > 0 ? args[0] : "uploads");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        start(folder, port);
    }

    // GET / : the page itself.
    private void page(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) {
            sendText(ex, 404, "Not found");
            return;
        }
        try (InputStream in = WebGui.class.getResourceAsStream("/gui/index.html")) {
            byte[] html = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, html.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(html);
            }
        }
    }

    // GET /api/videos : JSON list of the videos in the folder, newest first.
    private void list(HttpExchange ex) throws IOException {
        StringBuilder json = new StringBuilder("[");
        java.io.File[] files = folder.toFile().listFiles((dir, name) -> typeOf(name) != null);
        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(java.io.File::lastModified).reversed());
            for (java.io.File f : files) {
                if (json.length() > 1) json.append(',');
                json.append("{\"name\":\"").append(jsonEscape(f.getName()))
                        .append("\",\"size\":").append(f.length())
                        .append(",\"modified\":").append(f.lastModified()).append('}');
            }
        }
        byte[] body = json.append(']').toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }

    // GET /videos/<name> : the video file. Supports Range requests, which browsers need to seek in a video.
    private void video(HttpExchange ex) throws IOException {
        String raw = ex.getRequestURI().getRawPath().substring("/videos/".length());
        String name = URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
        Path file = folder.resolve(name).normalize();
        // Only plain file names inside the folder. This blocks "../" paths to other files on the machine.
        if (name.contains("/") || name.contains("\\") || !file.getParent().equals(folder)
                || typeOf(name) == null || !Files.isRegularFile(file)) {
            sendText(ex, 404, "Not found");
            return;
        }

        long size = Files.size(file);
        long start = 0;
        long end = size - 1;
        int status = 200;
        String range = ex.getRequestHeaders().getFirst("Range");
        if (range != null) {
            Matcher m = RANGE.matcher(range.trim());
            if (!m.matches() || (m.group(1).isEmpty() && m.group(2).isEmpty())) {
                ex.getResponseHeaders().set("Content-Range", "bytes */" + size);
                sendText(ex, 416, "Bad range");
                return;
            }
            if (m.group(1).isEmpty()) { // "bytes=-500" means the last 500 bytes
                start = Math.max(0, size - Long.parseLong(m.group(2)));
            } else {
                start = Long.parseLong(m.group(1));
                if (!m.group(2).isEmpty()) end = Math.min(end, Long.parseLong(m.group(2)));
            }
            if (start > end || start >= size) {
                ex.getResponseHeaders().set("Content-Range", "bytes */" + size);
                sendText(ex, 416, "Bad range");
                return;
            }
            status = 206;
            ex.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + size);
        }

        long length = end - start + 1;
        ex.getResponseHeaders().set("Content-Type", typeOf(name));
        ex.getResponseHeaders().set("Accept-Ranges", "bytes");
        ex.sendResponseHeaders(status, length);
        try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r");
             OutputStream out = ex.getResponseBody()) {
            in.seek(start);
            byte[] buffer = new byte[64 * 1024];
            long left = length;
            while (left > 0) {
                int n = in.read(buffer, 0, (int) Math.min(buffer.length, left));
                if (n < 0) break;
                out.write(buffer, 0, n);
                left -= n;
            }
        } catch (IOException e) {
            // The browser closed the connection (for example, the mouse left a preview). This is normal.
        }
    }

    private static String typeOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? null : TYPES.get(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String jsonEscape(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.toString();
    }

    private static void sendText(HttpExchange ex, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
