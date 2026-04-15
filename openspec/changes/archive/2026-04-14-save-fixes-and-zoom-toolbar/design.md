## Context

Three independent issues share this change:

1. **Save intent** — `MainWindow.saveFile(false)` writes directly to `openDocument.getSourceFile()` without ever asking the user whether they want to overwrite or save to a new location. The user can only get a chooser by using "Save As…" explicitly.

2. **Image annotation not restored on reload** — `PdfSaver.writeImage` embeds image bytes into a `PDAnnotationRubberStamp` appearance stream. `PdfLoader.parseAnnotation` reconstructs the `ImageAnnotation` with only its bounding box; it never reads the image bytes back from the appearance stream. The result is that every reloaded image annotation shows as a placeholder.

3. **Checkbox annotation may lose state** — The checked state is persisted via AcroForm field structures (`PDCheckBox.check()`/`unCheck()` → `/V` key). However, `PdfLoader.reconstructCheckboxField` relies on `PDFieldFactory.createField` to reconstruct the typed field from the widget's COS dictionary. This path is fragile: if the merged/non-merged form structure is not exactly what PDFieldFactory expects, `isChecked()` silently returns `false`. The label is similarly only stored in `/TU` (alternate field name), which requires field reconstruction to recover.

4. **Zoom toolbar** — The zoom level label exists in `EditorToolBar` but the only way to change zoom is via the View menu or Shift+scroll. Two flanking buttons would eliminate the need to open the menu for the most common zoom actions.

---

## Goals / Non-Goals

**Goals:**
- Show a one-time save-intent dialog per opened file per session, offering overwrite vs. save-as-new.
- Fix image annotation reload by extracting image bytes from the PDF appearance stream.
- Fix checkbox save/load by encoding all checkbox metadata (`checked`, `label`, `checkmarkColor`) in the widget's `/Contents` string, removing the dependency on AcroForm field reconstruction.
- Add `−` and `+` buttons flanking the zoom label in `EditorToolBar`.

**Non-Goals:**
- Persisting save-intent choice across app restarts.
- Full AcroForm compliance for PDF interoperability (checkbox appearance in external viewers is out of scope).
- Zoom input field allowing the user to type a specific percentage.

---

## Decisions

### 1. Save intent — per-session, per-source-file Set

**Decision:** `MainWindow` adds a `Set<File> saveIntentDecided` field. On the first call to `saveFile(false)` for a file that was opened from disk (`getSourceFile() != null` and not yet in the set), show a two-button dialog (overwrite / save as new). On "overwrite", add the file to the set and proceed. On "save as new", show the file chooser, update `openDocument.setSourceFile()`, add the new target to the set, and save. Subsequent calls with the same source file skip the dialog.

**Alternative considered:** A checkbox "don't ask again" in the dialog. Rejected — the simpler per-session approach satisfies the requirement without persistent settings.

---

### 2. Image reload — extract PDImageXObject from appearance stream

**Decision:** In `PdfLoader.parseAnnotation` for the `TAG_IMAGE` rubber stamp, after constructing the `ImageAnnotation`, walk the stamp's normal appearance stream resources to find the first `PDImageXObject`. Convert it to PNG bytes via `javax.imageio.ImageIO.write(pdImage.getImage(), "PNG", bos)`, store in `ia.setImageData(bytes)`, and construct the JavaFX `Image` from a `ByteArrayInputStream`. If extraction fails (null appearance, no image resources), the annotation is still returned but shows as a placeholder.

**Alternative considered:** Re-embedding the raw file bytes in a custom PDF annotation key (e.g., base64 in `/Contents`). Rejected — images can be large; encoding them in a text field is wasteful when the bytes are already in the appearance stream XObject.

---

### 3. Checkbox metadata — encode in widget /Contents

**Decision:** Change `PdfSaver.writeCheckbox` to encode all metadata in the widget `/Contents` key as a semicolon-delimited string: `"cc=<checkmarkColor>;chk=<true|false>;lbl=<label>"`. On load, `PdfLoader.reconstructCheckboxField` parses these three fields directly from `widget.getContents()`, removing the dependency on AcroForm field reconstruction entirely. The AcroForm field is still created for PDF standards compliance, but our app no longer depends on it for reload.

**Alternative considered:** Continue relying on AcroForm field reconstruction but add more defensive fallbacks. Rejected — the fragile path is structurally hard to make reliable across all PDFBox versions and PDF viewer round-trips; the contents-encoding approach is deterministic.

---

### 4. Zoom toolbar buttons — flanking the existing label

**Decision:** In `EditorToolBar`, add `Button("−")` before and `Button("+")` after the `zoomLabel`. Wire them to `canvas.zoomOut()` and `canvas.zoomIn()` respectively (via null-guard, same pattern as existing zoom wiring). Style them minimally to blend with the toolbar.

**Alternative considered:** Replace the label with a `Spinner<Integer>` for direct numeric input. Rejected — out of scope for this change; +/- buttons satisfy the requirement with minimal surface area.

---

## Risks / Trade-offs

- **ImageIO dependency**: `javax.imageio.ImageIO` is available in the standard JDK but requires `java.desktop` in module-info if the project uses modules. If the project is classpath-based this is not an issue. → Mitigation: verify no module-path restriction at compile time; fall back to placeholder if `getImage()` throws.
- **Checkbox label with semicolons**: If a label contains a semicolon, the split-based parser would misread it. → Mitigation: URL-encode the label value when writing, URL-decode when reading (or escape semicolons as `\;`). Simple percent-encoding is sufficient.
- **Save-intent dialog on "Save As…"**: "Save As…" is a separate menu item that always shows the chooser. The intent dialog applies only to the plain "Save" action. → No conflict; `saveAs == true` bypasses the intent check entirely.

---

## Migration Plan

No data migration needed. The changes to checkbox encoding are backward-compatible with PDFs that were saved before this change: old checkboxes have no `/Contents` key on the widget, so `widget.getContents()` returns `null` and the fallback `reconstructCheckboxField` (AcroForm path) is used. New saves encode metadata in contents, so old-path fallback is only needed for pre-existing PDFs.

---

## Open Questions

- Should the save-intent dialog also appear for files created via "Save As…" and then subsequently plain-saved? (Current answer: no — once a save-as destination is chosen, that target is added to `saveIntentDecided`.)
