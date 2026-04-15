### Requirement: About screen displays the application version
The About dialog SHALL display the application version number sourced from the Maven POM, so users can identify which release is installed.

#### Scenario: Version is shown on the About screen
- **WHEN** the user opens the About dialog
- **THEN** the application version number is visible (e.g., "1.0.0")

#### Scenario: Version matches the POM version
- **WHEN** the application is built with POM version "X.Y.Z"
- **THEN** the About dialog displays "X.Y.Z"

#### Scenario: Version is displayed near the app name
- **WHEN** the About dialog is open
- **THEN** the version appears in close visual proximity to the "PDF Tapir" title label
