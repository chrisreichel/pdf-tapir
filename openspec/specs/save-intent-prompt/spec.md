## ADDED Requirements

### Requirement: User is asked once per session how to save an opened file
When the user invokes Save on a file that was opened from disk and has not yet been saved in the current session, the application SHALL display a dialog asking whether to overwrite the original file or save to a new location.

#### Scenario: First save of an opened file
- **WHEN** the user invokes Save (menu, toolbar, or Ctrl+S) for a file that was opened from disk and not yet saved in this session
- **THEN** a dialog appears offering two options: "Overwrite original" and "Save as new file"

#### Scenario: Overwrite original chosen
- **WHEN** the user selects "Overwrite original" in the save-intent dialog
- **THEN** the file is saved to the original path and no dialog is shown for subsequent saves of the same file in this session

#### Scenario: Save as new file chosen
- **WHEN** the user selects "Save as new file" in the save-intent dialog
- **THEN** a file chooser is shown for the user to pick a new destination; after selection, the document is saved to the new path and subsequent saves use the new path without prompting

#### Scenario: Dialog cancelled
- **WHEN** the user dismisses the save-intent dialog without choosing
- **THEN** the save is aborted and no file is written

#### Scenario: Subsequent saves skip the dialog
- **WHEN** the user invokes Save again in the same session for the same file after already making a save-intent choice
- **THEN** the file is saved silently without showing the dialog

#### Scenario: Save As always shows file chooser
- **WHEN** the user invokes Save As (explicit action)
- **THEN** the file chooser is shown directly without the save-intent dialog, regardless of session state

#### Scenario: Newly created file (never saved) skips dialog
- **WHEN** the user invokes Save on a document that has no original file path
- **THEN** the file chooser is shown directly without the save-intent dialog
