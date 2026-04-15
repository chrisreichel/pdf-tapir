## 1. Fix Image Annotation Drag (PDF stays fixed)

- [x] 1.1 In `PdfCanvas.onMousePressed`, call `event.consume()` when a hit-test matches an annotation (so the ScrollPane does not receive the press)
- [x] 1.2 In `PdfCanvas.onMouseDragged`, call `event.consume()` when `isDraggingAnnotation` is true
- [x] 1.3 In `PdfCanvas.onMouseReleased`, call `event.consume()` when `isDraggingAnnotation` was true before resetting state
- [x] 1.4 Verify: drag an image annotation — PDF does not pan; drag on empty canvas — PDF does pan

## 2. Auto-Switch to SELECT After Placement

- [x] 2.1 Add/expose `activeToolProperty` as an `ObjectProperty<Tool>` in `PdfCanvas` so the toolbar can observe it
- [x] 2.2 Wire `EditorToolBar` toggle group to `PdfCanvas.activeToolProperty` so changing the property updates the button selection
- [x] 2.3 After `AddAnnotationCommand` is executed on mouse release (text and checkbox), call `setActiveTool(Tool.SELECT)` on the canvas
- [x] 2.4 After `promptImageFile` callback completes (both file chosen and cancelled), call `setActiveTool(Tool.SELECT)`
- [x] 2.5 Verify: place text → tool switches to SELECT; place checkbox → tool switches; place image → tool switches

## 3. Font Color for Text Annotations

- [x] 3.1 Confirm `TextAnnotation.fontColor` field and getter/setter exist (field exists per codebase review)
- [x] 3.2 In `PdfCanvas.drawAnnotation`, replace hardcoded `Color.BLACK` text fill with `Color.web(ta.getFontColor())`
- [x] 3.3 Add `ColorPicker` to `PropertiesPanel` for text annotations, initialized from `ta.getFontColor()`
- [x] 3.4 On `ColorPicker` value change, issue an `EditAnnotationCommand` capturing old and new color hex values
- [x] 3.5 Verify: select text annotation, change color in PropertiesPanel → text color updates; Undo → reverts

## 4. Checkmark Color for Checkbox Annotations

- [x] 4.1 Add `checkmarkColor` (String, default `"#000000"`) field with getter/setter to `CheckboxAnnotation`
- [x] 4.2 In `PdfCanvas.drawAnnotation` for checkboxes, replace hardcoded checkmark draw color with `Color.web(ca.getCheckmarkColor())`
- [x] 4.3 Add `ColorPicker` to `PropertiesPanel` for checkbox annotations, initialized from `ca.getCheckmarkColor()`
- [x] 4.4 On `ColorPicker` value change, issue an `EditAnnotationCommand` for checkmark color
- [x] 4.5 Verify: select checkbox, change checkmark color → checkmark redraws; Undo → reverts

## 5. Font Family for Text Annotations

- [x] 5.1 Add `fontFamily` (String, default `"System"`) field with getter/setter to `TextAnnotation`
- [x] 5.2 In `PdfCanvas.drawAnnotation`, construct font as `Font.font(ta.getFontFamily(), ta.getFontSize() * scale)` instead of `Font.font(size)`
- [x] 5.3 Add `ComboBox<String>` to `PropertiesPanel` for text annotations with values: System, Arial, Times New Roman, Courier New, Georgia, Verdana
- [x] 5.4 On `ComboBox` selection change, issue an `EditAnnotationCommand` capturing old and new font family
- [x] 5.5 Verify: select text annotation, change font → canvas redraws with new font; Undo → reverts; unknown font → falls back to system default

## 6. Inline Text Editing on Canvas

- [x] 6.1 Add an overlay `Pane` (e.g., `StackPane`) as the parent of `PdfCanvas` in `MainWindow` to host the `TextArea` overlay
- [x] 6.2 Create `showInlineTextEditor(TextAnnotation, canvasBounds)` method that adds a `TextArea` positioned over the annotation on the overlay pane
- [x] 6.3 On text annotation creation (after mouse release in TEXT tool), immediately call `showInlineTextEditor`
- [x] 6.4 On double-click of a text annotation in SELECT mode, call `showInlineTextEditor`
- [x] 6.5 Implement commit logic: on `TextArea` focus-lost or Escape, call `EditAnnotationCommand` with new text and remove the overlay
- [x] 6.6 Implement commit logic: on Enter key press in `TextArea`, commit and remove the overlay
- [x] 6.7 In `PdfCanvas.drawAnnotation`, skip rendering the text string while the inline editor overlay is active for that annotation
- [x] 6.8 On zoom or scroll change while overlay is active, reposition the `TextArea` to the updated canvas bounds
- [x] 6.9 After commit, update `PropertiesPanel` text field to reflect the new value
- [x] 6.10 Verify: create text → overlay appears; type → commit → annotation shows text; double-click existing text → edit → commit; Undo → reverts

## 7. Scroll-to-Zoom

- [x] 7.1 Register `setOnScroll` handler on the `PdfCanvas` node
- [x] 7.2 In the handler, if `event.isShiftDown()` is true: call `zoomIn()` for positive `deltaY`, `zoomOut()` for negative `deltaY`, then `event.consume()`
- [x] 7.3 If `event.isShiftDown()` is false, do not consume the event (let ScrollPane scroll normally)
- [x] 7.4 Verify: Shift + scroll up → zoom increases; Shift + scroll down → zoom decreases; scroll without Shift → page scrolls, no zoom change; toolbar label updates

## 8. Regression & Integration Verification

- [x] 8.1 Existing View menu zoom (Zoom In, Zoom Out, Fit Page) still works
- [x] 8.2 Undo/Redo stack is intact for all new commands (color, font family, inline text)
- [x] 8.3 Saving and re-opening a PDF preserves font color, font family, and checkmark color on text and checkbox annotations
- [x] 8.4 Drag of non-image annotations (text, checkbox) also does not pan the PDF (event consumption applies to all annotation drag, not just image)
