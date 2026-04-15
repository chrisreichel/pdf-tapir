## 1. Service Layer

- [x] 1.1 Create `PdfFlattenExporter` service in `com.pdftapir.service` that loads clean base PDF bytes, calls `PdfAnnotationFlattener.flatten()`, and saves without writing the PDF Tapir metadata package
- [x] 1.2 Make `PdfAnnotationFlattener` package-visible (or adjust access) so `PdfFlattenExporter` can use it alongside `PdfSaver`

## 2. UI — Menu Item

- [x] 2.1 Add "Export Flattened PDF..." `MenuItem` to the File menu in `MainWindow`, placed between "Save As..." and the separator before "Exit"
- [x] 2.2 Bind the menu item's disabled state to whether a document is currently open (disabled when `openDocument` is null)

## 3. UI — Confirmation Dialog & File Chooser

- [x] 3.1 Implement `exportFlattenedPdf()` handler in `MainWindow` that shows an `Alert` explaining the export is permanent and the file cannot be re-edited in PDF Tapir
- [x] 3.2 On confirm, open a `FileChooser` (title "Export Flattened PDF", `.pdf` extension filter) and obtain the destination path
- [x] 3.3 Call `PdfFlattenExporter.export(openDocument, targetFile)` with the chosen path; handle `IOException` by showing an error alert

## 4. Verification

- [x] 4.1 Manually test: export a document with text, checkbox, and image annotations — verify all render correctly in an external PDF viewer
- [x] 4.2 Manually test: open the exported PDF in PDF Tapir — verify no editable annotations are loaded
- [x] 4.3 Manually test: verify the original source file is unchanged and still fully editable after export
- [x] 4.4 Manually test: cancel at confirmation dialog — verify no file chooser appears and no file is written
