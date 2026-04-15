## ADDED Requirements

### Requirement: User can append pages from another PDF
The application SHALL allow the user to select one or more external PDF files and append their pages after the last page of the current document.

#### Scenario: Append pages via menu
- **WHEN** the user selects Document → Merge… , chooses "Append", selects one or more PDF files, and confirms
- **THEN** the pages from the selected files are added after the current document's last page and the canvas reloads showing the updated page count

#### Scenario: Append cancelled
- **WHEN** the user dismisses the Merge dialog or file chooser
- **THEN** no change is made to the document

### Requirement: User can prepend pages from another PDF
The application SHALL allow the user to select one or more external PDF files and insert their pages before the first page of the current document.

#### Scenario: Prepend pages via menu
- **WHEN** the user selects Document → Merge… , chooses "Prepend", selects one or more PDF files, and confirms
- **THEN** the pages from the selected files are inserted before page 1 of the current document and the canvas reloads showing the updated page count

### Requirement: Merge resets undo history
After a merge operation the undo/redo history SHALL be cleared and the user SHALL be warned that the merge cannot be undone.

#### Scenario: Confirm dialog before merge
- **WHEN** the user initiates a merge
- **THEN** a confirmation dialog warns "This action cannot be undone" before the merge proceeds

#### Scenario: Undo history cleared after merge
- **WHEN** a merge completes successfully
- **THEN** the undo and redo stacks are empty
