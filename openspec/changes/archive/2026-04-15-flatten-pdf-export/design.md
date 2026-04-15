## Context

PDF Tapir saves documents by (1) flattening annotations into page content via `PdfAnnotationFlattener` and (2) embedding a private metadata package via `PdfTapirPackageService` that allows the app to reload the editable model on next open. This dual-write strategy means every saved file carries extra embedded data.

`PdfAnnotationFlattener` already knows how to render all annotation types into PDF page content streams. `PdfSaver` orchestrates both the flattening step and the metadata write. What's missing is an export path that skips the metadata write entirely.

## Goals / Non-Goals

**Goals:**
- Add "Export Flattened PDF..." to the File menu as a distinct action from Save/Save As
- Produce a standard PDF with all annotations burned into page content and zero PDF Tapir metadata
- Warn the user before export that the resulting file cannot be re-edited in PDF Tapir
- Never overwrite the user's current editable source file
- Reuse `PdfAnnotationFlattener` — no new rendering logic

**Non-Goals:**
- Modifying or replacing the existing Save/Save As workflow
- Removing PDF encryption or third-party metadata from the export (only PDF Tapir metadata is stripped)
- Batch/bulk export of multiple documents at once

## Decisions

**New `PdfFlattenExporter` service** rather than adding a parameter to `PdfSaver`.
- `PdfSaver` has a clear, single responsibility: save with re-editability. Adding a boolean flag couples two unrelated concerns and makes `PdfSaver` harder to reason about.
- `PdfFlattenExporter` loads the clean base PDF bytes, calls `PdfAnnotationFlattener.flatten()`, and saves — without calling `PdfTapirPackageService.write()`. It is a thin ~30-line class.

**Confirmation dialog before file chooser** (not after).
- The user must acknowledge the destructive nature of the export before choosing where to save. This mirrors the mental model: "I understand this is permanent → now pick a location."
- A single `Alert` with "Export Flattened PDF" (confirm) and "Cancel" buttons is sufficient. No checkbox or "don't show again" needed — the action is infrequent enough to warrant the reminder every time.

**"Export Flattened PDF..." in File menu** between "Save As..." and the separator before "Exit".
- Keeps all save-related actions grouped together.
- The ellipsis signals that a dialog follows (file chooser), consistent with other menu items.

**Source file is never touched**.
- The export always writes to a user-chosen destination via a file chooser. The `openDocument` source file reference is not updated after export. The document remains "open" and editable in its original form.

## Risks / Trade-offs

- **User confusion about file identity**: After export, users might open the exported file and be surprised they can't re-edit it. Mitigated by the confirmation dialog copy ("This file cannot be re-opened for editing in PDF Tapir").
- **Large PDFs with images**: The export re-encodes embedded images via PDFBox (`PDImageXObject.createFromByteArray`). This is the same path used during normal Save, so no regression risk, but image-heavy files may still be large after export. This is expected behaviour — the feature targets metadata bloat, not image compression.

## Migration Plan

No migration needed. The export is a new, additive menu action. Existing files and the save workflow are unchanged.

## Open Questions

None.
