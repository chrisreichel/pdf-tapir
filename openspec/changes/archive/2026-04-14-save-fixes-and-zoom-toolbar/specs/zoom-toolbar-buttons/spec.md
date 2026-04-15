## ADDED Requirements

### Requirement: Toolbar provides zoom-in and zoom-out buttons
The EditorToolBar SHALL display a `−` button and a `+` button flanking the zoom percentage label, allowing the user to zoom in or out with a single click.

#### Scenario: Click + zooms in
- **WHEN** the user clicks the `+` button in the toolbar
- **THEN** the canvas scale increases by a factor of 1.25, clamped to 400%, and the zoom label updates

#### Scenario: Click − zooms out
- **WHEN** the user clicks the `−` button in the toolbar
- **THEN** the canvas scale decreases by a factor of 1.25, clamped to 25%, and the zoom label updates

#### Scenario: Buttons flanking zoom label
- **WHEN** the toolbar is displayed
- **THEN** the `−` button appears immediately to the left of the zoom label and the `+` button appears immediately to the right

#### Scenario: Buttons disabled before document loaded
- **WHEN** no PDF document is open
- **THEN** the `−` and `+` buttons SHALL be disabled (no canvas to zoom)
