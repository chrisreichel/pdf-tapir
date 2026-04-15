## MODIFIED Requirements

### Requirement: Native installer produced for Linux (Debian/Ubuntu)
The build system SHALL produce a `.deb` package for Linux that bundles the application and a JRE.

#### Scenario: Linux .deb installs and launches app
- **WHEN** a user installs the `.deb` with `apt install ./pdf-tapir_<version>_amd64.deb`
- **THEN** the app SHALL be launchable without any Java installation

### Requirement: Installer version matches pom.xml version
The version embedded in the installer filename and metadata SHALL match the pom.xml `<version>` value exactly, with no `-SNAPSHOT` suffix in production releases.

#### Scenario: Version consistency across artifacts
- **WHEN** the project version in pom.xml is `1.0.0`
- **THEN** the installers SHALL be named `PDF-Tapir-1.0.0.dmg`, `PDF-Tapir-1.0.0.msi`, and `pdf-tapir_1.0.0_amd64.deb`
