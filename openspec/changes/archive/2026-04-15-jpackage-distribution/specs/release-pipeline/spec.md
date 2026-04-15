## ADDED Requirements

### Requirement: Release workflow triggered by version tags
The GitHub Actions release workflow SHALL trigger automatically when a tag matching the pattern `v*` is pushed to the repository.

#### Scenario: Tag push triggers build
- **WHEN** a developer pushes a tag `v1.0.0`
- **THEN** the GitHub Actions workflow SHALL start and build all three platform installers

### Requirement: Parallel builds for all three platforms
The release workflow SHALL build macOS, Windows, and Linux installers in parallel using a matrix strategy so that all three are available without waiting for sequential builds.

#### Scenario: All three jobs run concurrently
- **WHEN** the release workflow is triggered
- **THEN** three jobs (mac, windows, linux) SHALL run in parallel, each on its respective runner

### Requirement: Installers attached to a GitHub Release
The release workflow SHALL create a GitHub Release for the tag and attach all three installer files as downloadable assets.

#### Scenario: Release created with all assets
- **WHEN** all three build jobs complete successfully
- **THEN** a GitHub Release SHALL exist for the tag with `.dmg`, `.msi`, and `.deb` files attached

### Requirement: Build uses platform-specific JavaFX runtime JARs
The Maven build on each platform runner SHALL include the correct platform-specific JavaFX classifier JARs (with native libraries) so that jpackage can bundle a working application.

#### Scenario: Mac runner uses mac JavaFX JARs
- **WHEN** the build runs on the macOS GitHub Actions runner
- **THEN** the JavaFX JARs copied to `target/lib/` SHALL include the platform-native macOS libraries

#### Scenario: Windows runner uses win JavaFX JARs
- **WHEN** the build runs on the Windows GitHub Actions runner
- **THEN** the JavaFX JARs copied to `target/lib/` SHALL include the platform-native Windows libraries

#### Scenario: Linux runner uses linux JavaFX JARs
- **WHEN** the build runs on the Ubuntu GitHub Actions runner
- **THEN** the JavaFX JARs copied to `target/lib/` SHALL include the platform-native Linux libraries
