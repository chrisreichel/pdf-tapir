## ADDED Requirements

### Requirement: Export Flattened PDF menu action
The application SHALL provide an "Export Flattened PDF..." menu item in the File menu. The action SHALL be available whenever a document is open.

#### Scenario: Menu item appears in File menu
- **WHEN** a PDF document is open
- **THEN** "Export Flattened PDF..." is visible in the File menu between "Save As..." and the separator before "Exit"

#### Scenario: Menu item is disabled when no document is open
- **WHEN** no document is currently loaded
- **THEN** "Export Flattened PDF..." is disabled and cannot be triggered

### Requirement: Confirmation dialog before export
Before writing any file, the application SHALL display a confirmation dialog that makes clear the exported file will be read-only and cannot be re-edited in PDF Tapir.

#### Scenario: Confirmation dialog shown on action trigger
- **WHEN** the user triggers "Export Flattened PDF..."
- **THEN** a dialog appears with the message that the exported PDF cannot be re-edited in PDF Tapir and will have a smaller file size

#### Scenario: User cancels export at confirmation
- **WHEN** the user dismisses the confirmation dialog by clicking "Cancel"
- **THEN** no file chooser opens and no file is written

#### Scenario: User confirms export
- **WHEN** the user clicks the confirm button in the dialog
- **THEN** a file chooser opens for the user to select the export destination

### Requirement: Flattened PDF export output
The exported PDF SHALL contain all annotations rendered as permanent page content. The exported file SHALL NOT contain any PDF Tapir editing metadata. The source file open in the editor SHALL NOT be modified.

#### Scenario: Annotations are burned into page content
- **WHEN** the user exports a document with text, checkbox, and image annotations
- **THEN** the exported PDF displays all annotations as static page content visible in any PDF viewer

#### Scenario: No PDF Tapir metadata in exported file
- **WHEN** the exported PDF is opened in PDF Tapir
- **THEN** the application loads it as a plain PDF with no editable annotations

#### Scenario: Source file is unchanged after export
- **WHEN** the user exports a flattened PDF to a new location
- **THEN** the original document remains open and editable in PDF Tapir with all annotations intact

#### Scenario: Export to chosen file path
- **WHEN** the user selects a destination in the file chooser and confirms
- **THEN** the flattened PDF is written to exactly that path with a .pdf extension
