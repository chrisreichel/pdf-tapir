# Repository Guidelines

## Project Structure & Module Organization
This is a Maven-based Java 21 desktop app for editing PDFs with JavaFX. Application code lives in `src/main/java/com/pdftapir`, split by responsibility:

- `ui/` for JavaFX windows, canvas, toolbar, and panels
- `service/` for PDF loading, rendering, saving, and coordinate mapping
- `model/` for document, page, and annotation types
- `command/` for undoable actions

Static assets live in `src/main/resources/com/pdftapir/`, including `style.css`. Tests mirror the package layout under `src/test/java/com/pdftapir`. Build output goes to `target/` and should not be committed.

## Build, Test, and Development Commands
- `mvn javafx:run` starts the JavaFX editor locally.
- `mvn test` runs the JUnit 5 test suite.
- `mvn package` compiles, tests, and builds the runnable JAR in `target/`.
- `java -jar target/pdf-tapir-1.0.0-SNAPSHOT.jar` launches the packaged app after `mvn package`.

Use Java 21+ and Maven 3.8+ to match the project configuration in `pom.xml`.

## Coding Style & Naming Conventions
Follow the existing Java style: 4-space indentation, braces on the same line, and clear package boundaries. Use `PascalCase` for classes and enums, `camelCase` for methods and fields, and lowercase package names such as `com.pdftapir.ui`.

Keep UI classes focused on interaction and layout, and push PDF manipulation into `service/` or `command/`. Add brief Javadoc or inline comments only where behavior is not obvious.

## Testing Guidelines
Tests use JUnit 5 via Maven Surefire. Name test classes `*Test` and keep them in the matching package path under `src/test/java`. Prefer focused unit tests for command and service logic, and add round-trip tests when changing PDF persistence behavior.

Run `mvn test` before opening a PR. There is no explicit coverage gate, so contributors are expected to add regression tests for bug fixes and new behavior.

## Commit & Pull Request Guidelines
Recent history uses short Conventional Commit prefixes such as `feat:`, `fix:`, and `docs:` with imperative summaries. Keep that format, for example: `fix: preserve image annotation bounds on save`.

PRs should include a concise description, linked issue or task when applicable, test evidence, and screenshots or screen recordings for visible JavaFX UI changes.
