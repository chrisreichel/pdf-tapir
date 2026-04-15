## Why

PDF Tapir has no way to distribute the app to non-developer users. There is no installer, no bundled JVM, and no release pipeline — users would need to clone the repo and run Maven themselves. Native installers solve this by shipping a self-contained executable for each platform.

## What Changes

- Add app icon to the JavaFX stage so it appears in the macOS Dock, task switcher, and window title bar
- Add platform icon files (`.icns`, `.ico`, `.png`) to `packaging/icons/` for use by jpackage
- Add Maven OS-activated profiles that pull in platform-specific JavaFX classifier JARs (required for jpackage bundling)
- Add a `maven-dependency-plugin` configuration to copy all runtime JARs to `target/lib/`
- Add a GitHub Actions release workflow (`release.yml`) triggered on `v*` tags that builds three installers in parallel and attaches them to a GitHub Release
- Remove `-SNAPSHOT` from the pom.xml version (1.0.0); version in pom, tag, and installer filename all match

## Capabilities

### New Capabilities
- `native-installers`: Build and distribute platform-native installers (macOS .dmg, Windows .msi, Linux .deb) via jpackage
- `release-pipeline`: GitHub Actions workflow that automates installer creation and GitHub Release publication on version tags

### Modified Capabilities

## Impact

- `pom.xml`: version change, new profiles, new plugin configuration
- `src/main/java/com/pdftapir/PdfTapirApp.java`: one-line icon stage addition
- New directory: `packaging/icons/` (ICON.icns, ICON.ico, ICON.png)
- New file: `.github/workflows/release.yml`
- No runtime behavior changes; no API or model changes
