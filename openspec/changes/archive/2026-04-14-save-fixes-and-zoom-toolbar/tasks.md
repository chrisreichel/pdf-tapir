## 1. Save Intent Prompt (First-Time-Per-File)

- [x] 1.1 Add `Set<File> saveIntentDecided` field to `MainWindow` (initialized as `new HashSet<>()`)
- [x] 1.2 In `MainWindow.saveFile(false)`: before writing, check if `target != null && !saveIntentDecided.contains(target)` — if so, show the intent dialog
- [x] 1.3 Build the intent dialog using `Alert(AlertType.CONFIRMATION)` with two `ButtonType` options: "Overwrite original" and "Save as new file"
- [x] 1.4 On "Overwrite original": add `target` to `saveIntentDecided` and proceed with save to `target`
- [x] 1.5 On "Save as new file": show `FileChooser`, update `openDocument.setSourceFile(newTarget)`, add `newTarget` to `saveIntentDecided`, then save to `newTarget`
- [x] 1.6 On dialog cancel/close: return early without saving
- [x] 1.7 Clear `saveIntentDecided` when a new document is opened (in `openFile` success handler)
- [x] 1.8 Verify: open a PDF → Ctrl+S → dialog appears; choose overwrite → saves, no dialog on next Ctrl+S; choose "save as new" → chooser shown, subsequent saves silent

## 2. Fix Image Annotation Reload

- [x] 2.1 In `PdfLoader.parseAnnotation` (TAG_IMAGE branch), after constructing the `ImageAnnotation`, retrieve the stamp's normal appearance stream: `stamp.getAppearance().getNormalAppearance().getAppearanceStream()`
- [x] 2.2 Iterate `form.getResources().getXObjectNames()` and find the first `PDImageXObject`
- [x] 2.3 Convert to PNG bytes: `ImageIO.write(pdImage.getImage(), "PNG", bos)` where `bos` is a `ByteArrayOutputStream`
- [x] 2.4 Call `ia.setImageData(bos.toByteArray())` and `ia.setFxImage(new Image(new ByteArrayInputStream(bytes)))` on the annotation
- [x] 2.5 Wrap the extraction in a try-catch; on failure, log a warning and return the annotation without image data (shows placeholder)
- [x] 2.6 Add necessary imports: `javax.imageio.ImageIO`, `java.io.ByteArrayOutputStream`, `java.io.ByteArrayInputStream`, `javafx.scene.image.Image`, `org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject`
- [x] 2.7 Verify: place an image annotation, save, close and reopen → image is restored instead of placeholder

## 3. Fix Checkbox Annotation Save/Reload

- [x] 3.1 In `PdfSaver.writeCheckbox`, change `widget.setContents(...)` to encode all three fields: `"cc=" + ca.getCheckmarkColor() + ";chk=" + ca.isChecked() + ";lbl=" + encodeLbl(ca.getLabel())` (percent-encode the label to handle semicolons)
- [x] 3.2 Add a private `encodeLbl(String)` helper in `PdfSaver` that percent-encodes at minimum the `;` character (replace `;` with `%3B`)
- [x] 3.3 In `PdfLoader.reconstructCheckboxField`, parse `widget.getContents()` for `cc=`, `chk=`, and `lbl=` keys (same split logic as existing font metadata parsing)
- [x] 3.4 Add a `decodeLbl(String)` helper in `PdfLoader` that reverses the encoding (`%3B` → `;`)
- [x] 3.5 If `chk=` is found, call `ca.setChecked(Boolean.parseBoolean(value))` directly without AcroForm reconstruction
- [x] 3.6 Keep the existing AcroForm reconstruction block as a fallback (runs only if `chk=` is not present — i.e., for PDFs saved before this change)
- [x] 3.7 Verify: create a checked checkbox, save, reopen → checkbox is still checked; unchecked → still unchecked; label preserved; checkmark color preserved

## 4. Zoom Toolbar Buttons

- [x] 4.1 In `EditorToolBar`, add `Button zoomOutBtn = new Button("−")` and `Button zoomInBtn = new Button("+")`
- [x] 4.2 Wire `zoomOutBtn.setOnAction` to call `canvas.zoomOut()` (with null-guard: `if (canvas != null)`)
- [x] 4.3 Wire `zoomInBtn.setOnAction` to call `canvas.zoomIn()` (with null-guard)
- [x] 4.4 Set both buttons disabled by default; enable them in `bindCanvas()` once a canvas is available
- [x] 4.5 Insert the buttons into the `HBox` in order: `... zoomOutBtn, zoomLabel, zoomInBtn, saveBtn`
- [x] 4.6 Verify: open PDF → `−` and `+` are enabled; click `+` → zoom increases, label updates; click `−` → zoom decreases; at 25% clicking `−` does nothing; at 400% clicking `+` does nothing

## 5. Regression Verification

- [x] 5.1 Save As… still shows the file chooser directly (no intent dialog)
- [x] 5.2 Text annotations (font color, family, content, size) still survive save/reload
- [x] 5.3 Zoom via View menu and Shift+scroll still works alongside new toolbar buttons
- [x] 5.4 Undo/Redo stack is unaffected by the save changes
