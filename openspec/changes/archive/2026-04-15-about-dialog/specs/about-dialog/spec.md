## ADDED Requirements

### Requirement: Help menu present in menu bar
The application menu bar SHALL include a **Help** menu as its rightmost entry.

#### Scenario: Help menu visible
- **WHEN** the application window is open
- **THEN** a "Help" menu SHALL be visible as the last item in the menu bar

### Requirement: About menu item opens About dialog
The Help menu SHALL contain an "About PDF Tapir…" item that opens the About dialog.

#### Scenario: About item triggers dialog
- **WHEN** the user clicks Help → About PDF Tapir…
- **THEN** a modal About dialog SHALL open, blocking interaction with the main window until dismissed

### Requirement: About dialog displays application identity
The About dialog SHALL display the application name, icon, and source URL.

#### Scenario: Name and icon shown
- **WHEN** the About dialog is open
- **THEN** it SHALL display the text "PDF Tapir" as the application name and the application icon scaled to approximately 64×64 pixels

#### Scenario: Source URL is a clickable hyperlink
- **WHEN** the About dialog is open
- **THEN** it SHALL display `https://github.com/chrisreichel/pdf-tapir` as a clickable hyperlink that opens in the system's default browser

### Requirement: About dialog displays authorship and license
The About dialog SHALL identify the developer and the license under which the software is distributed.

#### Scenario: Developer and license shown
- **WHEN** the About dialog is open
- **THEN** it SHALL display "Christian Reichel" as the developer name and "AGPL-3.0" as the license

### Requirement: About dialog displays warranty disclaimer
The About dialog SHALL include a plain-language warranty disclaimer making clear the software is provided as-is.

#### Scenario: Disclaimer text present
- **WHEN** the About dialog is open
- **THEN** it SHALL display a disclaimer stating that the software is provided "as is" without warranty of any kind, and that the author is not liable for any damages arising from its use

### Requirement: About dialog is dismissible
The About dialog SHALL provide a way to close it and return to the main window.

#### Scenario: Close button dismisses dialog
- **WHEN** the user clicks the Close button in the About dialog
- **THEN** the dialog SHALL close and focus SHALL return to the main application window
