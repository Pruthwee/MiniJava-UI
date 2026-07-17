# MiniJava-UI

A small **UI-transformation testing application**: an HTML/CSS/JS playground front end
paired with a Java backend that applies the same transformations. It exists to test and
demonstrate UI-transformation flows end-to-end &mdash; you describe a UI as a JSON config,
apply a set of transformations, and inspect the *before* and *after* rendered output, run
either in the browser or by the Java engine.

It is a companion to
[`mini-java-app`](https://github.com/Pruthwee/mini-java-app) and follows the same spirit:
a compact, self-contained Java demo (Apache-2.0 licensed) used to exercise an agent-driven
flow &mdash; here, UI transformation rather than code upgrade / containerization.

## What it does

A **UI config** is a JSON document describing a UI as a list of components:

```json
{
  "title": "Login",
  "theme": "light",
  "components": [
    { "type": "heading", "text": "Sign in" },
    { "type": "label",   "text": "Username" },
    { "type": "input",   "name": "username", "placeholder": "you@example.com" },
    { "type": "button",  "text": "Log In" }
  ]
}
```

The **transformation engine** rewrites that config and renders it to HTML. Four
transformations ship in the box:

| Transformation | Effect |
| --- | --- |
| `uppercase-labels` | Uppercases the text of `label` and `heading` components |
| `dark-theme` | Switches the config's theme to `dark` |
| `mark-inputs-required` | Adds `required` to every `input` |
| `add-aria-labels` | Derives a human-readable `aria-label` for each `input` from its `name` |

The engine is implemented **twice, in lock-step**: once in Java
(`UiTransformer`) and once in JavaScript (`frontend/js/app.js`). The playground can run
transformations in the browser *or* delegate to the Java backend over
`POST /api/transform`, so you can confirm a UI transformation behaves identically on both
sides. That parity check is the point of the app.

## Project structure

```
MiniJava-UI/
├── pom.xml                       # Maven build (app has zero third-party deps; JUnit is test-only)
├── LICENSE                       # Apache License 2.0
└── src/
    ├── main/java/com/minijava/ui/
    │   ├── Main.java              # Entry point: starts the HTTP server
    │   ├── TransformServer.java   # Serves the bundled front end + POST /api/transform (JDK HttpServer)
    │   ├── UiTransformer.java     # Core: apply transforms, render config -> HTML
    │   └── json/MiniJson.java     # Tiny dependency-free JSON parser/serializer
    ├── main/resources/frontend/   # Static UI-transformation playground, bundled onto the classpath
    │   ├── index.html
    │   ├── css/styles.css
    │   └── js/app.js              # Browser mirror of the Java engine
    └── test/java/com/minijava/ui/
        └── UiTransformerTest.java # JUnit 5 tests for the transformation engine
```

The front-end assets live under `src/main/resources/` so they are packaged into the
jar and served from the classpath. The server therefore starts from **any** working
directory &mdash; `java -jar` works no matter where you run it from.

## Requirements

- **JDK 17+** (the app uses only the standard library).
- **Maven** is optional &mdash; convenient, but the project also builds with plain `javac`.

## Build & run

### With Maven

```sh
mvn package                       # compiles, runs tests, builds the runnable target/minijava-ui.jar
java -jar target/minijava-ui.jar  # starts the server on http://localhost:8080/ (from any directory)
```

### Without Maven (JDK only)

```sh
# Compile the Java sources
mkdir -p target/classes
javac -d target/classes $(find src/main/java -name '*.java')

# Copy the bundled front-end assets onto the classpath
# (Maven does this automatically; do it by hand for the plain-javac path)
cp -R src/main/resources/. target/classes/

# Run the server (default port 8080)
java -cp target/classes com.minijava.ui.Main
# Optional: java -cp target/classes com.minijava.ui.Main 9000
```

The front end is served from the classpath, so the server does not depend on the
current working directory. Pass a second argument only if you want to live-edit
assets from a filesystem directory during development, e.g.
`java -cp target/classes com.minijava.ui.Main 8080 src/main/resources/frontend`.

Then open **http://localhost:8080/** and use the playground:

1. Edit the UI config JSON (or click **Load sample**).
2. Tick the transformations to apply.
3. Choose the engine &mdash; **browser (JavaScript)** or **Java backend**.
4. Click **Transform** to see the *before*/*after* preview and the generated HTML.

### Front end only

The playground also works as a static page (browser engine only). Open
`src/main/resources/frontend/index.html` directly in a browser; the "Use Java backend"
option simply requires the server to be running.

## The Java UI-transformation API

```sh
curl -X POST http://localhost:8080/api/transform \
  -H 'Content-Type: application/json' \
  -d '{
    "config": {
      "title": "Login",
      "components": [
        { "type": "label", "text": "Username" },
        { "type": "input", "name": "user_name" }
      ]
    },
    "transforms": ["uppercase-labels", "add-aria-labels"]
  }'
```

Response:

```json
{
  "applied": ["uppercase-labels", "add-aria-labels"],
  "config": { "...": "the rewritten UI config" },
  "html": "<section class=\"ui-card theme-light\"> ... </section>"
}
```

## Tests

`UiTransformerTest` covers rendering, each transformation, composition order, unknown-name
handling, and HTML escaping:

```sh
mvn test
```

## License

Apache License 2.0 &mdash; see [`LICENSE`](LICENSE).
