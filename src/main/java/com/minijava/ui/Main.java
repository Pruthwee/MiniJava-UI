package com.minijava.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry point for MiniJava-UI.
 *
 * <p>Starts the {@link TransformServer}, which serves the UI-transformation
 * playground and the {@code /api/transform} endpoint. Usage:</p>
 * <pre>
 *   java -cp target/classes com.minijava.ui.Main [port] [frontendDir]
 * </pre>
 */
public final class Main {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_FRONTEND = "frontend";

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Path frontendDir = Paths.get(args.length > 1 ? args[1] : DEFAULT_FRONTEND)
                .toAbsolutePath().normalize();

        if (!Files.isDirectory(frontendDir)) {
            System.err.println("Frontend directory not found: " + frontendDir);
            System.err.println("Run from the project root, or pass the frontend path as the 2nd argument.");
            System.exit(1);
        }

        TransformServer server = new TransformServer(port, frontendDir);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        System.out.println("MiniJava-UI is running.");
        System.out.println("  Playground:  http://localhost:" + port + "/");
        System.out.println("  Transform:   POST http://localhost:" + port + "/api/transform");
        System.out.println("  Serving:     " + frontendDir);
        System.out.println("Press Ctrl+C to stop.");
    }
}
