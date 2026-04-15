## ADDED Requirements

### Requirement: User can select a font family for text annotations
The PropertiesPanel SHALL display a font family dropdown when a text annotation is selected, allowing the user to choose from a curated set of fonts.

#### Scenario: Font family dropdown shown for text annotation
- **WHEN** the user selects a text annotation
- **THEN** a font family ComboBox is visible in the PropertiesPanel, showing the current font family

#### Scenario: Available font families
- **WHEN** the font family ComboBox is opened
- **THEN** it SHALL contain at minimum: System, Arial, Times New Roman, Courier New, Georgia, Verdana

#### Scenario: Changing font family updates the canvas
- **WHEN** the user selects a different font family
- **THEN** the text annotation is redrawn with the selected font immediately

#### Scenario: Font family change is undoable
- **WHEN** the user changes the font family and then invokes Undo
- **THEN** the font family reverts to its previous value

#### Scenario: Default font family is System
- **WHEN** a new text annotation is created
- **THEN** its font family is "System" (the JavaFX default)

#### Scenario: Unknown font family falls back gracefully
- **WHEN** a text annotation has a font family that is not installed on the OS
- **THEN** the text is rendered using the system default font without throwing an error
