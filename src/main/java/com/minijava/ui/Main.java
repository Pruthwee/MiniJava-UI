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
 *   java -jar target/minijava-ui.jar [port] [frontendDir]
 *   java -cp target/classes com.minijava.ui.Main [port] [frontendDir]
 * </pre>
 *
 * <p>The front-end assets are bundled on the classpath, so the server runs from
 * any working directory with no {@code frontend/} directory required. An optional
 * second argument points at a filesystem directory that is checked before the
 * bundled assets, for live editing during development.</p>
 */
public final class Main {

    private static final int DEFAULT_PORT = 8080;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        // Optional dev override: a filesystem front-end dir, checked before the
        // bundled classpath assets. Only honored when it actually exists.
        Path frontendDir = null;
        if (args.length > 1) {
            Path candidate = Paths.get(args[1]).toAbsolutePath().normalize();
            if (!Files.isDirectory(candidate)) {
                System.err.println("Frontend override directory not found: " + candidate);
                System.exit(1);
            }
            frontendDir = candidate;
        }

        TransformServer server = new TransformServer(port, frontendDir);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        System.out.println("MiniJava-UI is running.");
        System.out.println("  Playground:  http://localhost:" + port + "/");
        System.out.println("  Transform:   POST http://localhost:" + port + "/api/transform");
        System.out.println("  Serving:     " + (frontendDir != null
                ? frontendDir + " (filesystem), then bundled assets"
                : "bundled classpath assets"));
        System.out.println("Press Ctrl+C to stop.");
    }
}
