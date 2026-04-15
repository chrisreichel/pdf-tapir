### Requirement: User can toggle bold and italic on text annotations
The PropertiesPanel SHALL display bold and italic checkboxes when a text annotation is selected, allowing the user to independently enable or disable each style.

#### Scenario: Bold and italic controls shown for text annotation
- **WHEN** the user selects a text annotation
- **THEN** a "Bold" checkbox and an "Italic" checkbox are visible in the PropertiesPanel

#### Scenario: Enabling bold redraws the annotation
- **WHEN** the user checks the "Bold" checkbox
- **THEN** the text annotation is redrawn with bold styling immediately

#### Scenario: Enabling italic redraws the annotation
- **WHEN** the user checks the "Italic" checkbox
- **THEN** the text annotation is redrawn with italic styling immediately

#### Scenario: Bold and italic can be combined
- **WHEN** the user enables both "Bold" and "Italic"
- **THEN** the text annotation is rendered in bold-italic style

#### Scenario: Bold change is undoable
- **WHEN** the user toggles bold and then invokes Undo
- **THEN** the bold state reverts to its previous value

#### Scenario: Italic change is undoable
- **WHEN** the user toggles italic and then invokes Undo
- **THEN** the italic state reverts to its previous value

#### Scenario: Default is non-bold, non-italic
- **WHEN** a new text annotation is created
- **THEN** bold is false and italic is false

### Requirement: User can set text alignment on text annotations
The PropertiesPanel SHALL display left, center, and right alignment toggle buttons when a text annotation is selected.

#### Scenario: Alignment controls shown for text annotation
- **WHEN** the user selects a text annotation
- **THEN** three alignment toggle buttons (Left, Center, Right) are visible in the PropertiesPanel with the current alignment highlighted

#### Scenario: Changing alignment redraws the annotation
- **WHEN** the user selects a different alignment button
- **THEN** the text annotation is redrawn with the new alignment immediately

#### Scenario: Alignment change is undoable
- **WHEN** the user changes alignment and then invokes Undo
- **THEN** the alignment reverts to its previous value

#### Scenario: Default alignment is left
- **WHEN** a new text annotation is created
- **THEN** its text alignment is LEFT

### Requirement: Bold, italic, and alignment are preserved in saved PDFs
Bold, italic, and alignment SHALL be serialised into the pdf-tapir metadata package so they survive a save/reload cycle.

#### Scenario: Bold state survives save and reload
- **WHEN** a text annotation with bold enabled is saved and the file is reopened
- **THEN** the annotation is still bold

#### Scenario: Italic state survives save and reload
- **WHEN** a text annotation with italic enabled is saved and the file is reopened
- **THEN** the annotation is still italic

#### Scenario: Alignment survives save and reload
- **WHEN** a text annotation with center alignment is saved and the file is reopened
- **THEN** the annotation still uses center alignment

#### Scenario: Missing fields default gracefully on older files
- **WHEN** a PDF saved without bold/italic/alignment fields is opened
- **THEN** the text annotation loads with bold=false, italic=false, alignment=LEFT

### Requirement: Bold, italic, and alignment apply in flattened PDF output
When a PDF is saved or exported as flattened, text annotations SHALL reflect bold, italic, and alignment in the burned-in page content.

#### Scenario: Bold text is rendered bold in flattened output
- **WHEN** a text annotation with bold=true is flattened
- **THEN** the text appears in a bold font variant in the output PDF

#### Scenario: Italic text is rendered italic in flattened output
- **WHEN** a text annotation with italic=true is flattened
- **THEN** the text appears in an italic (oblique) font variant in the output PDF

#### Scenario: Center-aligned text is centered in flattened output
- **WHEN** a text annotation with center alignment is flattened
- **THEN** each line of text is horizontally centered within the annotation bounding box

#### Scenario: Right-aligned text is right-aligned in flattened output
- **WHEN** a text annotation with right alignment is flattened
- **THEN** each line of text is aligned to the right edge of the annotation bounding box
