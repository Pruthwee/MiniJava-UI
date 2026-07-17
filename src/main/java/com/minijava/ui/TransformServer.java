package com.minijava.ui;

import com.minijava.ui.json.MiniJson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A tiny HTTP server that serves the MiniJava-UI front end and exposes the Java
 * UI-transformation engine over HTTP.
 *
 * <p>Built entirely on the JDK's {@code com.sun.net.httpserver} so the whole demo
 * runs with no third-party dependencies. Endpoints:</p>
 * <ul>
 *   <li>{@code GET /} and static files &rarr; the {@code frontend/} playground.</li>
 *   <li>{@code POST /api/transform} &rarr; run the transforms server-side and
 *       return the rewritten config plus rendered HTML as JSON.</li>
 * </ul>
 */
public final class TransformServer {

    private final int port;
    private final Path frontendDir;
    private final UiTransformer transformer = new UiTransformer();
    private HttpServer server;

    public TransformServer(int port, Path frontendDir) {
        this.port = port;
        this.frontendDir = frontendDir;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/transform", this::handleTransform);
        server.createContext("/", this::handleStatic);
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ---- POST /api/transform -------------------------------------------------

    private void handleTransform(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"use POST\"}");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> request = MiniJson.parseObject(body);

            Object config = request.get("config");
            if (config == null) {
                sendJson(exchange, 400, "{\"error\":\"missing 'config'\"}");
                return;
            }
            String configJson = UiTransformer.toJson(config);

            List<String> transforms = new ArrayList<>();
            Object rawTransforms = request.get("transforms");
            if (rawTransforms instanceof List) {
                for (Object t : (List<?>) rawTransforms) {
                    if (t != null) {
                        transforms.add(t.toString());
                    }
                }
            }

            UiTransformer.Result result = transformer.transform(configJson, transforms);
            StringBuilder json = new StringBuilder();
            json.append('{');
            json.append("\"applied\":[");
            for (int i = 0; i < result.applied.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                json.append('"').append(MiniJson.escape(result.applied.get(i))).append('"');
            }
            json.append("],");
            json.append("\"config\":").append(UiTransformer.toJson(result.config)).append(',');
            json.append("\"html\":\"").append(MiniJson.escape(result.html)).append('"');
            json.append('}');
            sendJson(exchange, 200, json.toString());
        } catch (RuntimeException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + MiniJson.escape(String.valueOf(ex.getMessage())) + "\"}");
        }
    }

    // ---- Static files --------------------------------------------------------

    private void handleStatic(HttpExchange exchange) throws IOException {
        String rawPath = exchange.getRequestURI().getPath();
        String relative = rawPath.equals("/") ? "index.html" : rawPath.substring(1);

        Path target = frontendDir.resolve(relative).normalize();
        if (!target.startsWith(frontendDir) || !Files.isRegularFile(target)) {
            byte[] notFound = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(404, notFound.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound);
            }
            return;
        }

        byte[] bytes = Files.readAllBytes(target);
        exchange.getResponseHeaders().set("Content-Type", contentType(relative));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (path.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (path.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        return "application/octet-stream";
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Utility for tests / callers that just want to drain a stream. */
    static String read(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
}
