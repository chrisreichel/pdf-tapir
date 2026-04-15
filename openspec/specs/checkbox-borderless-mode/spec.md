### Requirement: User can enable borderless mode on checkbox annotations
The PropertiesPanel SHALL display a "Borderless" checkbox when a checkbox annotation is selected. When enabled, the annotation renders without the surrounding box border.

#### Scenario: Borderless control shown for checkbox annotation
- **WHEN** the user selects a checkbox annotation
- **THEN** a "Borderless" checkbox is visible in the PropertiesPanel

#### Scenario: Enabling borderless hides the border
- **WHEN** the user checks "Borderless"
- **THEN** the checkbox annotation is redrawn without the rectangular border

#### Scenario: Borderless checked annotation shows only the checkmark
- **WHEN** a borderless checkbox annotation is in the checked state
- **THEN** only the checkmark is visible, with no surrounding border

#### Scenario: Borderless unchecked annotation is invisible
- **WHEN** a borderless checkbox annotation is in the unchecked state
- **THEN** nothing is rendered for that annotation

#### Scenario: Disabling borderless restores the border
- **WHEN** the user unchecks "Borderless"
- **THEN** the checkbox annotation is redrawn with the rectangular border

#### Scenario: Borderless change is undoable
- **WHEN** the user toggles borderless and then invokes Undo
- **THEN** the borderless state reverts to its previous value

#### Scenario: Default is not borderless
- **WHEN** a new checkbox annotation is created
- **THEN** borderless is false and the border is visible

### Requirement: Borderless mode is preserved in saved PDFs
The borderless flag SHALL be serialised into the pdf-tapir metadata package so it survives a save/reload cycle.

#### Scenario: Borderless state survives save and reload
- **WHEN** a checkbox annotation with borderless=true is saved and the file is reopened
- **THEN** the annotation still has borderless=true and renders without a border

#### Scenario: Missing borderless field defaults to false on older files
- **WHEN** a PDF saved without the borderless field is opened
- **THEN** the checkbox annotation loads with borderless=false

### Requirement: Borderless mode applies in flattened PDF output
When a PDF is saved or exported as flattened, borderless checkbox annotations SHALL not render a border in the burned-in page content.

#### Scenario: Borderless checkbox renders no border in flattened output
- **WHEN** a borderless checked checkbox annotation is flattened
- **THEN** the output PDF shows only the checkmark with no surrounding rectangle
