## MODIFIED Requirements

### Requirement: User can zoom in
The canvas SHALL increase its scale by a factor of 1.25 when the user invokes Zoom In (via View menu, keyboard shortcut, or Shift + scroll up), up to a maximum of 400%.

#### Scenario: Zoom in from 100% via menu
- **WHEN** the user selects View → Zoom In or presses Ctrl+=
- **THEN** the canvas scale becomes 125% and redraws at the new scale

#### Scenario: Zoom in from 100% via Shift+scroll
- **WHEN** the user holds Shift and scrolls up on the canvas
- **THEN** the canvas scale becomes 125% and redraws at the new scale

#### Scenario: Zoom in at maximum
- **WHEN** the canvas is already at 400% and the user invokes Zoom In by any means
- **THEN** the scale remains at 400%

### Requirement: User can zoom out
The canvas SHALL decrease its scale by a factor of 1.25 when the user invokes Zoom Out (via View menu, keyboard shortcut, or Shift + scroll down), down to a minimum of 25%.

#### Scenario: Zoom out from 100% via menu
- **WHEN** the user selects View → Zoom Out or presses Ctrl+-
- **THEN** the canvas scale becomes 80% and redraws at the new scale

#### Scenario: Zoom out from 100% via Shift+scroll
- **WHEN** the user holds Shift and scrolls down on the canvas
- **THEN** the canvas scale becomes 80% and redraws at the new scale

#### Scenario: Zoom out at minimum
- **WHEN** the canvas is already at 25% and the user invokes Zoom Out by any means
- **THEN** the scale remains at 25%
