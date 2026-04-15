## 1. Close File — Menu Item

- [x] 1.1 Add "Close" menu item to File menu between "Open…" and "Save"
- [x] 1.2 Wire `fileMenu.setOnShowing` to disable "Close" when `openDocument == null`

## 2. Close File — Implementation

- [x] 2.1 Add `closeFile()` method to `MainWindow`: call `openDocument.getPdDocument().close()`, nullify `openDocument`, `canvas`, `scrollPane`
- [x] 2.2 In `closeFile()`, call `undoManager.clear()` to discard stale undo history
- [x] 2.3 In `closeFile()`, call `root.setCenter(null)` and `root.setTop(buildMenuBar())` to return to empty state

## 3. Print — Menu Item

- [x] 3.1 Add "Print…" menu item to File menu after "Export Flattened PDF…"
- [x] 3.2 Wire `fileMenu.setOnShowing` to disable "Print…" when `openDocument == null`

## 4. Print — Implementation

- [x] 4.1 Add `printDocument()` method to `MainWindow` using Java AWT `PrinterJob` + PDFBox `PDFPageable`
- [x] 4.2 Show native print dialog via `PrinterJob.getPrinterJob()` and `printerJob.printDialog()`
- [x] 4.3 On confirm, call `printerJob.print(pageable)` to send all pages to the selected printer
- [x] 4.4 Wrap the print call with `SwingUtilities.invokeLater` (crossed from JavaFX thread to AWT EDT)
- [x] 4.5 On print failure, show an error alert to the user via `Platform.runLater`

## 5. Verification

- [x] 5.1 Verify "Close" is disabled in File menu when no document is open
- [x] 5.2 Verify "Close" removes canvas and toolbar; app remains running with menu bar only
- [x] 5.3 Verify a new document can be opened after close
- [x] 5.4 Verify "Print…" is disabled when no document is open
- [x] 5.5 Verify "Print…" opens the OS native print dialog when a document is loaded
