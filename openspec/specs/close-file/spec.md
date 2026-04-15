## ADDED Requirements

### Requirement: User can close the open document without exiting
The application SHALL provide a "Close" menu item in the File menu that closes the current document and returns the app to an empty state, keeping the application running.

#### Scenario: Close menu item appears in File menu
- **WHEN** the application is running
- **THEN** a "Close" menu item is visible in the File menu

#### Scenario: Close is disabled when no document is open
- **WHEN** no document is currently loaded
- **THEN** the "Close" menu item is disabled

#### Scenario: Close is enabled when a document is open
- **WHEN** a document is currently loaded
- **THEN** the "Close" menu item is enabled

#### Scenario: Closing a document removes the canvas and toolbar
- **WHEN** the user selects "Close"
- **THEN** the editor canvas, toolbar, and scroll pane are removed from the UI

#### Scenario: App returns to a usable empty state after close
- **WHEN** the user closes a document
- **THEN** the application remains running with only the menu bar visible and no document loaded

#### Scenario: Document-dependent menu items are disabled after close
- **WHEN** the user closes a document
- **THEN** Save, Save As, Export Flattened PDF, Print, and Document menu items are disabled

#### Scenario: Another document can be opened after close
- **WHEN** the user closes a document and then uses "Open…"
- **THEN** the new document loads and the canvas appears normally

#### Scenario: Undo history is cleared on close
- **WHEN** the user closes a document
- **THEN** the undo and redo history is cleared
