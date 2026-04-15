## ADDED Requirements

### Requirement: User can set font color for text annotations
The PropertiesPanel SHALL display a color picker when a text annotation is selected, allowing the user to change the text color.

#### Scenario: Color picker shown for text annotation
- **WHEN** the user selects a text annotation
- **THEN** a color picker is visible in the PropertiesPanel

#### Scenario: Changing text color updates the canvas
- **WHEN** the user picks a new color from the color picker
- **THEN** the text annotation is redrawn with the selected color immediately

#### Scenario: Color change is undoable
- **WHEN** the user changes the font color and then invokes Undo
- **THEN** the font color reverts to its previous value

#### Scenario: Default text color is black
- **WHEN** a new text annotation is created
- **THEN** its font color is #000000 (black)

### Requirement: User can set checkmark color for checkbox annotations
The PropertiesPanel SHALL display a color picker when a checkbox annotation is selected, allowing the user to change the checkmark color.

#### Scenario: Color picker shown for checkbox annotation
- **WHEN** the user selects a checkbox annotation
- **THEN** a color picker for the checkmark is visible in the PropertiesPanel

#### Scenario: Changing checkmark color updates the canvas
- **WHEN** the user picks a new color from the checkmark color picker
- **THEN** the checkbox annotation is redrawn with the selected checkmark color immediately

#### Scenario: Default checkmark color is black
- **WHEN** a new checkbox annotation is created
- **THEN** its checkmark color is #000000 (black)
