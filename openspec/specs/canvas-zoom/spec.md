# canvas-zoom

### Requirement: User can zoom in
The canvas SHALL increase its scale by a factor of 1.25 when the user invokes Zoom In (via View menu, keyboard shortcut, Shift + scroll up, or toolbar `+` button), up to a maximum of 400%.

#### Scenario: Zoom in from 100% via menu
- **WHEN** the user selects View → Zoom In or presses Ctrl+=
- **THEN** the canvas scale becomes 125% and redraws at the new scale

#### Scenario: Zoom in from 100% via Shift+scroll
- **WHEN** the user holds Shift and scrolls up on the canvas
- **THEN** the canvas scale becomes 125% and redraws at the new scale

#### Scenario: Zoom in from 100% via toolbar button
- **WHEN** the user clicks the `+` button in the toolbar while the canvas is at 100%
- **THEN** the canvas scale becomes 125% and redraws at the new scale

#### Scenario: Zoom in at maximum
- **WHEN** the canvas is already at 400% and the user invokes Zoom In by any means
- **THEN** the scale remains at 400%

### Requirement: User can zoom out
The canvas SHALL decrease its scale by a factor of 1.25 when the user invokes Zoom Out (via View menu, keyboard shortcut, Shift + scroll down, or toolbar `−` button), down to a minimum of 25%.

#### Scenario: Zoom out from 100% via menu
- **WHEN** the user selects View → Zoom Out or presses Ctrl+-
- **THEN** the canvas scale becomes 80% and redraws at the new scale

#### Scenario: Zoom out from 100% via Shift+scroll
- **WHEN** the user holds Shift and scrolls down on the canvas
- **THEN** the canvas scale becomes 80% and redraws at the new scale

#### Scenario: Zoom out from 100% via toolbar button
- **WHEN** the user clicks the `−` button in the toolbar while the canvas is at 100%
- **THEN** the canvas scale becomes 80% and redraws at the new scale

#### Scenario: Zoom out at minimum
- **WHEN** the canvas is already at 25% and the user invokes Zoom Out by any means
- **THEN** the scale remains at 25%

### Requirement: User can fit the page to the viewport
The canvas SHALL compute and apply a scale such that the current page fits within the visible scroll pane viewport when the user invokes Fit Page.

#### Scenario: Fit Page applied
- **WHEN** the user selects View → Fit Page or presses Ctrl+0
- **THEN** the canvas scale is set so the page fills the viewport (the smaller of width-fit and height-fit), clamped to 25%–400%, and the canvas redraws

### Requirement: Toolbar zoom label reflects current scale
The toolbar zoom label SHALL always display the current canvas scale as a percentage, updating whenever the scale changes.

#### Scenario: Label updates after zoom in
- **WHEN** the user zooms in
- **THEN** the toolbar label shows the new percentage (e.g. "125%")

#### Scenario: Label on document open
- **WHEN** a document is opened and no zoom action has been taken
- **THEN** the toolbar label shows "100%"
