## ADDED Requirements

### Requirement: User can remove one or more pages from the current document
The application SHALL allow the user to select one or more page numbers to delete from the currently open document. At least one page MUST remain after removal.

#### Scenario: Remove a single page via menu
- **WHEN** the user selects Document → Remove Pages… , checks one page in the selection dialog, and confirms
- **THEN** that page is removed, the remaining pages renumber, the canvas reloads at the nearest valid page, and the undo history is cleared

#### Scenario: Remove multiple pages
- **WHEN** the user selects Document → Remove Pages… , checks multiple pages, and confirms
- **THEN** all selected pages are removed, remaining pages renumber, and the canvas reloads

#### Scenario: Cannot remove all pages
- **WHEN** the user attempts to select all pages for removal
- **THEN** the confirm button is disabled or an error is shown preventing a zero-page document

#### Scenario: Removal cancelled
- **WHEN** the user dismisses the Remove Pages dialog without confirming
- **THEN** no pages are removed and the document is unchanged

### Requirement: Remove Pages resets undo history
After a page-removal operation the undo/redo history SHALL be cleared and the user SHALL be warned that the removal cannot be undone.

#### Scenario: Confirm dialog before removal
- **WHEN** the user confirms page removal
- **THEN** a confirmation warning "This action cannot be undone" is shown before pages are deleted

#### Scenario: Undo history cleared after removal
- **WHEN** page removal completes successfully
- **THEN** the undo and redo stacks are empty
