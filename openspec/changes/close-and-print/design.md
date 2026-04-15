## Context

`MainWindow` manages all UI state through two paths: `reloadCanvas(pageIndex)` (called when a document is open) which sets `root.setTop(menuBar + toolbar)` and `root.setCenter(scrollPane)`, and `buildUI()` (called on construction) which sets the initial menu bar and properties panel. There is no code path that returns the app to a "no document" state — once a document is loaded, the canvas and toolbar are always present.

`openDocument` is the sentinel: all document-specific menu items guard against `null` via early returns or `setOnShowing` handlers. The JavaFX `PrinterJob` API (in `javafx.print` module) provides a native OS print dialog.

## Goals / Non-Goals

**Goals:**
- Close the open document and return to a clean empty state (menu bar only, no canvas/toolbar, document-dependent menu items disabled)
- Print the current PDF to the OS print dialog via `javafx.print.PrinterJob`

**Non-Goals:**
- "Unsaved changes" detection or save-before-close prompt (the app has no dirty-state tracking)
- Print preview within the app
- Print-range or page-selection UI (the OS dialog handles that)
- Page-by-page rendering control — we delegate entirely to PDFBox's `PDFPrintable` / `PDFPageable` via Java AWT `PrinterJob`, which is more reliable than JavaFX's `PrinterJob` for multi-page PDFs

## Decisions

**Use Java AWT `PrinterJob` + PDFBox `PDFPageable`, not JavaFX `PrinterJob`.**
- JavaFX `PrinterJob` requires rendering each page as a JavaFX `Node`, which means re-rendering the PDF through our own JavaFX canvas — complex, resolution-limited, and not how OS print dialogs expect PDFs.
- PDFBox provides `PDFPageable` (implements `java.awt.print.Pageable`) which feeds pages directly from the PDF to the OS print spooler at native resolution. This is the standard approach for PDFBox applications.
- `javafx.scene.control.Dialog` cannot block while AWT's print dialog runs on a different thread model; we show the AWT print dialog directly via `PrinterJob.getPrinterJob()`.
- No new Maven dependency: `pdfbox` (already present) includes `PDFPageable` and `PDFPrintable`.

**Close returns to a blank center pane, not to a placeholder/welcome screen.**
- The simplest recoverable state: `root.setCenter(null)`, `root.setTop(buildMenuBar())`, nullify `openDocument`, `canvas`, `scrollPane`. All document-dependent menu items are already guarded by null checks or `setOnShowing` handlers — no additional state machine needed.
- `undoManager.clear()` is called to discard stale undo history.
- The open document's `PDDocument` is closed (via `openDocument.close()`) to release file handles.

**"Close" placement in File menu: between "Open…" and "Save", disabled when no document open.**
- Mirrors standard application convention (File → Close).
- Uses `fileMenu.setOnShowing` (already present) to set the disabled state.

**"Print…" placement in File menu: after "Export Flattened PDF…", before the separator.**
- Groups all output actions together.
- Uses `fileMenu.setOnShowing` to disable when no document is open.

## Risks / Trade-offs

- **AWT + JavaFX thread interaction**: The AWT print dialog must be shown on the AWT Event Dispatch Thread, not the JavaFX thread. Wrap the call with `Platform.runLater` → `SwingUtilities.invokeLater` to cross thread boundaries safely. PDFBox's `PDFPageable` is thread-safe for reading.
- **Close without save prompt**: Users who close accidentally lose unsaved work. Acceptable for now given no dirty-state tracking exists in the app. A future change can add this.

## Migration Plan

No migration needed. Both features are purely additive menu actions.
