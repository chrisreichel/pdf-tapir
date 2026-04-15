## ADDED Requirements

### Requirement: Dragging an image annotation does not pan the PDF
When the user drags a selected image annotation, the underlying PDF canvas SHALL remain stationary; only the annotation SHALL move.

#### Scenario: Drag image annotation with SELECT tool
- **WHEN** the user presses and drags on a selected image annotation using the SELECT tool
- **THEN** the annotation moves with the mouse and the PDF page does not pan

#### Scenario: Mouse event does not propagate to ScrollPane during annotation drag
- **WHEN** the user is actively dragging any annotation
- **THEN** the mouse press, drag, and release events SHALL be consumed by the canvas and SHALL NOT reach the parent ScrollPane

#### Scenario: ScrollPane panning still works when not dragging an annotation
- **WHEN** the user presses and drags on an area of the canvas that contains no annotation
- **THEN** the ScrollPane pans normally
