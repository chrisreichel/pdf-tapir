## ADDED Requirements

### Requirement: User can type directly into a text annotation on the canvas
When a text annotation is active (just created or double-clicked), the canvas SHALL display an editable text overlay positioned over the annotation so the user can type without using the PropertiesPanel.

#### Scenario: Text overlay appears immediately after creation
- **WHEN** the user draws a text annotation bounding box and releases the mouse
- **THEN** an editable text area overlay appears over the annotation bounds, ready for input

#### Scenario: Text overlay appears on double-click of existing text annotation
- **WHEN** the user double-clicks a text annotation with the SELECT tool
- **THEN** an editable text area overlay appears over the annotation bounds, ready for input

#### Scenario: Text is committed on overlay focus-lost
- **WHEN** the inline text overlay loses focus (user clicks elsewhere or presses Escape)
- **THEN** the annotation text is updated with the contents of the overlay, the overlay is removed, and the canvas redraws

#### Scenario: Text is committed on Enter key
- **WHEN** the user presses Enter while the inline text overlay is active
- **THEN** the annotation text is committed and the overlay is removed

#### Scenario: Inline edit is undoable
- **WHEN** the user edits text inline and commits, then invokes Undo
- **THEN** the text reverts to its value before the inline edit

#### Scenario: PropertiesPanel stays in sync during inline edit
- **WHEN** the user commits text via the inline overlay
- **THEN** the PropertiesPanel text field is updated to reflect the new text

#### Scenario: Canvas annotation text hidden while overlay is active
- **WHEN** the inline text overlay is visible
- **THEN** the canvas SHALL NOT render the annotation's own text to avoid visual duplication
