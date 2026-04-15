## ADDED Requirements

### Requirement: User can print the current PDF via the OS print dialog
The application SHALL provide a "Print…" menu item in the File menu that opens the operating system's native print dialog for the current document.

#### Scenario: Print menu item appears in File menu
- **WHEN** the application is running
- **THEN** a "Print…" menu item is visible in the File menu

#### Scenario: Print is disabled when no document is open
- **WHEN** no document is currently loaded
- **THEN** the "Print…" menu item is disabled

#### Scenario: Print is enabled when a document is open
- **WHEN** a document is currently loaded
- **THEN** the "Print…" menu item is enabled

#### Scenario: Print opens the OS native print dialog
- **WHEN** the user selects "Print…"
- **THEN** the operating system's native print dialog appears

#### Scenario: Print sends all pages to the printer on confirm
- **WHEN** the user confirms the print dialog
- **THEN** all pages of the current document are sent to the selected printer

#### Scenario: Cancelling the print dialog does nothing
- **WHEN** the user cancels the print dialog
- **THEN** no print job is submitted and the application remains in its current state

#### Scenario: Print error shows an error message
- **WHEN** the print job fails (e.g. printer unavailable)
- **THEN** an error alert is shown to the user describing the failure
