## ADDED Requirements

### Requirement: App icon displayed in OS task switcher and Dock
The application SHALL display the ICON.png image as its window and Dock icon so that the OS task switcher and window manager can identify it visually.

#### Scenario: Icon visible in macOS Dock
- **WHEN** the application is running on macOS
- **THEN** the Dock and Cmd+Tab switcher SHALL show the ICON.png icon for the app

#### Scenario: Icon visible in Windows taskbar
- **WHEN** the application is running on Windows
- **THEN** the taskbar and Alt+Tab switcher SHALL show the ICON.png icon for the app

### Requirement: Native installer produced for macOS
The build system SHALL produce a `.dmg` installer for macOS that bundles the application and a JRE so no prior Java installation is required.

#### Scenario: macOS installer installs and launches app
- **WHEN** a user opens the `.dmg` and drags the app to Applications
- **THEN** the app SHALL launch without requiring any Java installation

#### Scenario: macOS installer uses app icon
- **WHEN** the `.dmg` installer is created
- **THEN** the app bundle inside SHALL display ICON.icns as its icon in Finder and Dock

### Requirement: Native installer produced for Windows
The build system SHALL produce a `.msi` installer for Windows that bundles the application and a JRE so no prior Java installation is required.

#### Scenario: Windows installer installs and launches app
- **WHEN** a user runs the `.msi` installer
- **THEN** the app SHALL be installed and launchable without any Java installation

#### Scenario: Windows installer uses app icon
- **WHEN** the `.msi` installer is created
- **THEN** the installed app SHALL display ICON.ico as its shortcut and window icon

### Requirement: Native installer produced for Linux (Debian/Ubuntu)
The build system SHALL produce a `.deb` package for Linux that bundles the application and a JRE.

#### Scenario: Linux .deb installs and launches app
- **WHEN** a user installs the `.deb` with `apt install ./pdf-escroto_<version>_amd64.deb`
- **THEN** the app SHALL be launchable without any Java installation

### Requirement: Installer version matches pom.xml version
The version embedded in the installer filename and metadata SHALL match the pom.xml `<version>` value exactly, with no `-SNAPSHOT` suffix in production releases.

#### Scenario: Version consistency across artifacts
- **WHEN** the project version in pom.xml is `1.0.0`
- **THEN** the installers SHALL be named `PDF-Escroto-1.0.0.dmg`, `PDF-Escroto-1.0.0.msi`, and `pdf-escroto_1.0.0_amd64.deb`
