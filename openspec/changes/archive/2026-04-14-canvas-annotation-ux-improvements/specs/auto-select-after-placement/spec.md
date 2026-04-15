## ADDED Requirements

### Requirement: Active tool reverts to SELECT after content is committed
After the user places and commits an annotation (text, checkbox, or image), the active tool SHALL automatically switch to the SELECT tool.

#### Scenario: Auto-select after placing a text annotation
- **WHEN** the user draws a text annotation bounding box and releases the mouse
- **THEN** the active tool becomes SELECT and the newly created annotation is selected

#### Scenario: Auto-select after placing a checkbox annotation
- **WHEN** the user draws a checkbox annotation bounding box and releases the mouse
- **THEN** the active tool becomes SELECT and the newly created annotation is selected

#### Scenario: Auto-select after placing an image annotation
- **WHEN** the user draws an image annotation bounding box, releases the mouse, and either selects an image file or cancels the file chooser
- **THEN** the active tool becomes SELECT

#### Scenario: Toolbar toggle group reflects the tool switch
- **WHEN** the active tool is automatically switched to SELECT
- **THEN** the SELECT button in the EditorToolBar is visually active and all other tool buttons are inactive
