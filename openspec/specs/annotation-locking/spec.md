## ADDED Requirements

### Requirement: Annotations are locked against editing in external viewers on save
All annotations written by pdf-tapir (text, checkbox, image) SHALL have the PDF `ReadOnly` (bit 7) and `Locked` (bit 8) annotation flags set when serialised to the PDF file. This prevents external PDF viewers from moving, resizing, or deleting the annotations.

#### Scenario: Text annotation cannot be moved in external viewer
- **WHEN** a PDF containing a text annotation is saved by pdf-tapir and opened in an external viewer
- **THEN** the annotation's `ReadOnly` and `Locked` flags are set in the PDF, so the viewer must not allow the user to move, resize, or delete it

#### Scenario: Image annotation cannot be moved in external viewer
- **WHEN** a PDF containing an image annotation is saved by pdf-tapir and opened in an external viewer
- **THEN** the annotation's `ReadOnly` and `Locked` flags are set in the PDF, so the viewer must not allow the user to move, resize, or delete it

#### Scenario: Checkbox annotation cannot be toggled in external viewer
- **WHEN** a PDF containing a checkbox annotation is saved by pdf-tapir and opened in an external viewer
- **THEN** the annotation widget's `ReadOnly` and `Locked` flags are set, and the AcroForm field's `ReadOnly` flag is set, so the viewer must not allow the user to check/uncheck or move the checkbox

### Requirement: Locked annotations remain fully editable within pdf-tapir
Annotations that were saved with lock flags SHALL be loaded and treated as fully editable within pdf-tapir. The lock is a serialization-time concern only and does not restrict in-app editing.

#### Scenario: Reload locked annotation and edit it
- **WHEN** a PDF with locked annotations is reopened in pdf-tapir
- **THEN** all annotations are loaded and can be moved, resized, edited, or deleted within the app as normal

#### Scenario: Re-saving reapplies locks
- **WHEN** a user edits a locked annotation in pdf-tapir and saves the document again
- **THEN** the updated annotation is written back to the PDF with lock flags re-applied
