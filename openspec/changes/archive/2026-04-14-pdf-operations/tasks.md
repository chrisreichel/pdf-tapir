## 1. PDF Encryption Service

- [x] 1.1 Create `PdfEncryptionService` with `encrypt(PDDocument, String password)` method using PDFBox `StandardProtectionPolicy` (AES-256, owner=user password)
- [x] 1.2 Add `decrypt(PDDocument)` method to `PdfEncryptionService` that removes the security handler
- [x] 1.3 Add `isEncrypted(PDDocument)` helper method to `PdfEncryptionService`

## 2. Encrypted PDF on Open

- [x] 2.1 Wrap `Loader.loadPDF(file)` in `PdfLoader.load()` to catch `InvalidPasswordException`
- [x] 2.2 On `InvalidPasswordException`, show a password dialog in `MainWindow` and retry with `Loader.loadPDF(file, password)`
- [x] 2.3 On wrong password show an error alert; on cancel abort loading without changing app state

## 3. Encryption UI (Document Menu)

- [x] 3.1 Add `Document` menu to the menu bar in `MainWindow` (between View and any existing right-side menus)
- [x] 3.2 Add `Document > Encrypt…` menu item: show password input dialog, call `PdfEncryptionService.encrypt()`, then save via existing save flow
- [x] 3.3 Add `Document > Decrypt…` menu item: call `PdfEncryptionService.decrypt()`, then save; disable item when document is not encrypted
- [x] 3.4 Track encryption state on `PdfDocument` (boolean `isEncrypted`) and update the Decrypt menu item binding when a document is opened or saved

## 4. PDF Merge Service

- [x] 4.1 Create `PdfMergeService` with `append(PdfDocument target, List<File> sources)` and `prepend(PdfDocument target, List<File> sources)` methods using PDFBox `PDFMergerUtility`
- [x] 4.2 After merge, rebuild `PdfDocument.pages` list from the mutated `PDDocument` and reset the `UndoManager`

## 5. Merge UI

- [x] 5.1 Add `Document > Merge…` menu item that opens a dialog with Append/Prepend radio buttons and a multi-file chooser
- [x] 5.2 Show confirm dialog "This action cannot be undone" before merging; on confirmation call `PdfMergeService`, then reload the canvas to page 0

## 6. PDF Page Removal Service

- [x] 6.1 Create `PdfPageService` with `removePages(PdfDocument doc, Set<Integer> pageIndices)` method that removes pages from the `PDDocument` and rebuilds the `PdfDocument.pages` list
- [x] 6.2 Validate that at least one page remains; throw `IllegalArgumentException` if all pages would be removed

## 7. Remove Pages UI

- [x] 7.1 Add `Document > Remove Pages…` menu item that opens a checklist dialog showing all page numbers (1-based)
- [x] 7.2 Disable the confirm button when all pages are selected (to prevent a zero-page document)
- [x] 7.3 Show confirm dialog "This action cannot be undone" before removal; on confirmation call `PdfPageService.removePages()`, then reload the canvas to the nearest valid page and reset `UndoManager`

## 8. Tests

- [x] 8.1 Unit test `PdfEncryptionService`: encrypt a blank PDF, verify it is encrypted; decrypt and verify it is no longer encrypted
- [x] 8.2 Unit test `PdfMergeService`: append a second single-page PDF to a document and verify page count increases
- [x] 8.3 Unit test `PdfPageService`: remove one page from a two-page document and verify one page remains; verify all-page removal is rejected
