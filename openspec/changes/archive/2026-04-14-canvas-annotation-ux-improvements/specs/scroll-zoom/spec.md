## ADDED Requirements

### Requirement: User can zoom using Shift + scroll wheel
The canvas SHALL zoom in when the user holds Shift and scrolls up, and zoom out when the user holds Shift and scrolls down.

#### Scenario: Shift + scroll up zooms in
- **WHEN** the user holds the Shift key and scrolls up on the canvas
- **THEN** the canvas scale increases by a factor of 1.25, clamped to the maximum of 400%

#### Scenario: Shift + scroll down zooms out
- **WHEN** the user holds the Shift key and scrolls down on the canvas
- **THEN** the canvas scale decreases by a factor of 1.25, clamped to the minimum of 25%

#### Scenario: Scroll without Shift does not zoom
- **WHEN** the user scrolls on the canvas without holding Shift
- **THEN** the canvas scale is unchanged and the ScrollPane scrolls normally

#### Scenario: Scroll event is consumed when zooming
- **WHEN** the user performs a Shift + scroll zoom action
- **THEN** the scroll event SHALL be consumed so the ScrollPane does not also scroll

#### Scenario: Toolbar zoom label updates on scroll zoom
- **WHEN** the canvas scale changes via Shift + scroll
- **THEN** the toolbar zoom label displays the updated percentage
