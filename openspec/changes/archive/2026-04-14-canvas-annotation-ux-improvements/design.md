## Context

The application is a JavaFX PDF annotation tool built around a `PdfCanvas` (a `Canvas` inside a `ScrollPane`) with a `PropertiesPanel` sidebar. Annotations are stored in PDF-space coordinates and rendered via a JavaFX `GraphicsContext`. All mutations flow through a `Command` pattern with `UndoManager`.

Known current issues:
- Mouse drag events on an image annotation bubble up and also pan the parent `ScrollPane`, causing both the annotation and the PDF to move simultaneously.
- After placing any annotation tool, the active tool remains unchanged, forcing the user to manually switch back to SELECT.
- `TextAnnotation.fontColor` exists in the model but is hardcoded to black in the canvas renderer; no color picker is exposed in the UI.
- `TextAnnotation` has no `fontFamily` field; the canvas uses the JavaFX default font unconditionally.
- `CheckboxAnnotation` has no color field for the checkmark.
- Text annotations can only be edited via the `PropertiesPanel` textfield; no in-canvas editing exists.
- There is no scroll-based zoom; zoom is only accessible through the View menu.

---

## Goals / Non-Goals

**Goals:**
- Fix image annotation drag so only the annotation moves; the PDF stays fixed.
- Auto-switch to SELECT tool immediately after any annotation content is committed.
- Expose font color picker for text and checkbox annotations in `PropertiesPanel`.
- Add font family selector for text annotations (a curated list of common Java/system fonts).
- Enable in-canvas text editing by rendering a `TextArea` overlay on double-click (or immediately on first creation).
- Add Shift + scroll wheel zoom to `PdfCanvas`.

**Non-Goals:**
- Rich-text / multi-format text inside a single text annotation.
- Drag-and-drop reordering of annotations.
- Custom font file upload.
- Color pickers for anything other than text/checkmark color.

---

## Decisions

### 1. Fix image drag — stop event propagation to ScrollPane

**Decision:** In the `onMousePressed` handler, when the SELECT tool hits an image annotation, call `event.consume()` to prevent the `ScrollPane` from receiving the press. Likewise, consume `onMouseDragged` and `onMouseReleased` events during an active annotation drag. Since the `Canvas` node is a child of the `ScrollPane`'s content pane, consuming events at the `Canvas` level is sufficient to block panning.

**Alternative considered:** Override the `ScrollPane` pan filter. This is more complex and affects all mouse interaction, not just annotation dragging.

---

### 2. Auto-switch to SELECT after placement

**Decision:** At the exact point where an annotation is committed (i.e., after `AddAnnotationCommand` is executed on mouse release, and after the image `FileChooser` callback completes), call `setActiveTool(Tool.SELECT)` on the canvas and notify `EditorToolBar` via an existing `activeToolProperty` observable (to be added if not present). The toolbar listens and updates its `ToggleGroup` selection.

**Alternative considered:** Fire a custom JavaFX event from the canvas up to the toolbar. Rejected because a shared observable property is simpler and already the architectural pattern (see how `scale` is exposed).

---

### 3. Font color — use existing model field, add color picker UI

**Decision:** `TextAnnotation.fontColor` already exists as a CSS hex string. For `CheckboxAnnotation`, add a `checkmarkColor` field (String, default `#000000`). In `PropertiesPanel`, add a `ColorPicker` node below the font size field, bound to the selected annotation's color. On color change, issue an `EditAnnotationCommand`. In `PdfCanvas.drawAnnotation`, replace the hardcoded `Color.BLACK` text fill with `Color.web(ta.getFontColor())`.

**Alternative considered:** Use JavaFX CSS styling on an overlaid node. Rejected because annotations are drawn on a `Canvas` via `GraphicsContext`, which does not support CSS.

---

### 4. Font family — new model field + dropdown in PropertiesPanel

**Decision:** Add `fontFamily` (String, default `"System"`) to `TextAnnotation`. In `PropertiesPanel`, add a `ComboBox<String>` populated with a fixed list: `["System", "Arial", "Times New Roman", "Courier New", "Georgia", "Verdana"]`. On selection, issue an `EditAnnotationCommand`. In `PdfCanvas.drawAnnotation`, construct the font as `Font.font(ta.getFontFamily(), ta.getFontSize() * scale)`.

**Alternative considered:** Let the user type any font name. Rejected to avoid broken rendering when a font is not installed on the system.

---

### 5. Inline text editing — TextArea overlay

**Decision:** When a text annotation is double-clicked (in SELECT mode) *or* when it is first created and the creation drag is released, overlay a `TextArea` positioned and sized to match the annotation's canvas bounds. The `TextArea` is added to the canvas's parent `StackPane` (or a dedicated overlay pane). On focus-lost or Enter (configurable), the text is committed via `EditAnnotationCommand`, the overlay is removed, and the annotation redraws. The canvas hides the text portion of the annotation while the overlay is active to avoid double-rendering.

**Alternative considered:** Use a JavaFX `TextField` embedded in the canvas coordinate space. `Canvas` does not support child nodes, so an overlay pane approach is required regardless of widget type. `TextArea` is chosen over `TextField` to allow multi-line future use.

---

### 6. Scroll-to-zoom — wheel event on PdfCanvas

**Decision:** Register an `onScroll` handler on the `PdfCanvas` node. When `event.isShiftDown()` is true, determine zoom direction from `event.getDeltaY()` (positive = zoom in, negative = zoom out) and call the existing `zoomIn()` / `zoomOut()` methods. Consume the event to prevent the `ScrollPane` from scrolling. When Shift is not held, let the event propagate normally for scrolling.

**Alternative considered:** Register the handler on the `ScrollPane` instead. The canvas handler approach is more cohesive since zoom logic already lives in `PdfCanvas`.

---

## Risks / Trade-offs

- **Inline TextArea overlay sizing**: The overlay must be re-positioned whenever the canvas is scrolled or zoomed while editing. → Mitigation: Commit the text on any zoom or scroll event if the overlay is active.
- **Font availability**: Selected font families may not be installed on all operating systems. → Mitigation: Use `Font.font(family, size)` which falls back to the default font gracefully.
- **Event consumption for drag fix**: Consuming events during annotation drag will disable `ScrollPane` panning while dragging an annotation. → Acceptable trade-off; users can scroll when not dragging an annotation.
- **Auto-select timing with FileChooser**: The `FileChooser` is modal; tool switch must happen in the callback after the dialog closes, not before. → The existing `promptImageFile` method is the correct insertion point.

---

## Migration Plan

No data migration is needed. The new `fontFamily` and `checkmarkColor` fields on annotation models default to safe values (`"System"` and `"#000000"`), so existing saved PDFs will render identically after the update.

---

## Open Questions

- Should Shift+scroll zoom anchor to the mouse cursor position (true zoom-to-point) or to the current scroll center? (Recommend cursor-anchored for better UX, but requires adjusting `ScrollPane` viewport offsets post-scale.)
- Should the inline text box appear immediately when an annotation is first placed, or only on double-click of an existing annotation? (Proposal says immediately after placement — confirm with user.)
