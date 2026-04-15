## Context

PDF Escroto is a JavaFX 21 application built with Maven. It has no existing packaging or CI/CD infrastructure. Distribution currently requires cloning the repo and running `mvn javafx:run`. The goal is to produce self-contained native installers that bundle the JVM so end users need no prerequisites.

The key complexity is that JavaFX Maven artifacts come in two forms: a platform-neutral API JAR (no native code) and platform-specific JARs with classifiers (`mac`, `mac-aarch64`, `win`, `linux`). The `javafx-maven-plugin` injects the correct platform JAR at dev time, but jpackage needs the native JARs present in the dependency folder at build time. This must be solved explicitly.

## Goals / Non-Goals

**Goals:**
- Produce `.dmg` (macOS), `.msi` (Windows), and `.deb` (Linux) installers via jpackage
- Bundle the JVM — no Java installation required on the end user's machine
- Set the app icon in the JavaFX stage (Dock, task switcher, window)
- Automate installer creation via GitHub Actions on `v*` tag pushes
- Attach installers to a GitHub Release automatically
- Installer version matches pom.xml version matches git tag (no SNAPSHOT in production)

**Non-Goals:**
- Code signing or notarization (deferred)
- Auto-update mechanism
- Linux `.rpm` packaging (can build from source)
- Windows `.exe` (`.msi` is sufficient)
- macOS `.pkg` (`.dmg` is sufficient)

## Decisions

### D1: Platform-specific JavaFX JARs via Maven profiles (not jlink)

**Chosen:** Maven OS-activated profiles that declare JavaFX dependencies with platform classifiers. Each GitHub Actions runner activates the matching profile automatically based on `os.name`.

**Alternative considered:** Download the JavaFX SDK jmods separately in CI and use `jlink` to create a minimal runtime, then `jpackage --runtime-image`. This gives smaller installers but adds significant CI complexity (downloading jmods, configuring module descriptors, handling non-modular third-party deps like PDFBox that need `--add-modules`).

**Rationale:** OS profiles are a one-time pom.xml change; the CI workflow stays simple. Installer size is acceptable (~120–150 MB with bundled JRE).

### D2: jpackage invoked directly in GitHub Actions, not via Maven plugin

**Chosen:** Shell `jpackage` command in the workflow YAML.

**Alternative considered:** `com.github.akman:jpackage-maven-plugin`. Adds a Maven plugin dependency and version to maintain; the plugin adds little value over a direct CLI call since all parameters are platform-specific anyway.

**Rationale:** jpackage ships with JDK 14+. GitHub Actions `actions/setup-java` provides JDK 21 which includes it. A direct shell command is transparent and easier to debug.

### D3: Dependency layout — app JAR + all deps in single `target/lib/` folder

**Chosen:** `maven-dependency-plugin` copies all runtime dependencies to `target/lib/`. The app JAR is also copied there. jpackage uses `--input target/lib/` with `--main-jar pdf-escroto-<version>.jar`.

**Rationale:** jpackage's `--input` approach works correctly with platform-specific JavaFX JARs because the native libs are embedded in those JARs and extracted by the JVM at runtime — no special jpackage handling needed.

### D4: Icon conversion done locally and committed, not in CI

**Chosen:** Convert `ICON.png` → `ICON.icns` and `ICON.ico` locally, commit both to `packaging/icons/`. CI uses these pre-converted files.

**Alternative considered:** Convert in CI using ImageMagick. Adds CI dependency; conversion is a one-time operation per icon change, not per build.

**Rationale:** Icon assets are static design artifacts. Committing them is appropriate. CI stays lean.

### D5: GitHub Release created by the workflow on tag push

**Chosen:** Use `softprops/action-gh-release` action (well-maintained, widely used) to create the release and attach all three installers in a single step from the matrix.

**Rationale:** Each matrix job uploads its artifact; the release action runs in each job and upserts the release. First job creates it, subsequent jobs attach to it.

## Risks / Trade-offs

- **Maven profile activation on ARM Mac (M1/M2):** The mac-aarch64 classifier is needed for Apple Silicon runners. GitHub Actions `macos-latest` has been ARM since late 2024. Both `mac` (Intel) and `mac-aarch64` (ARM) profiles may be needed, or a single profile that detects arch.
  → Mitigation: Declare both profiles; activate on `mac` family and additionally check `os.arch` for `aarch64`.

- **Windows runner available disk space:** jpackage + JRE bundling on Windows can use ~1 GB of temp space. GitHub Actions hosted runners have ~14 GB free.
  → Low risk, no mitigation needed.

- **pom.xml version without SNAPSHOT:** Removing SNAPSHOT means `mvn package` on the main branch always produces a release-versioned JAR. Developers need to remember to bump the version after tagging.
  → Acceptable for the current project scale.

## Migration Plan

1. Update pom.xml: remove SNAPSHOT, add profiles and dependency plugin config
2. Convert ICON.png → ICON.icns (macOS) and ICON.ico (Windows), commit to `packaging/icons/`
3. Add one-line icon to `PdfEscrotoApp.java`
4. Add `.github/workflows/release.yml`
5. Tag `v1.0.0` and push to trigger the first release

No rollback needed — all changes are additive or in new files.
