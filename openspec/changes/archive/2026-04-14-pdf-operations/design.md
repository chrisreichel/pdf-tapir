## Context

pdf-tapir is a JavaFX desktop PDF editor built on PDFBox 3.x. Its current architecture has three layers:
- **Model**: `PdfDocument`, `PdfPage`, and annotation types
- **Services**: `PdfLoader`, `PdfSaver`, `PdfRenderer`, `CoordinateMapper`
- **UI**: `MainWindow`, `PdfCanvas`, `EditorToolBar`, `PropertiesPanel`

`MainWindow` owns the top-level menu bar and save orchestration. Three new document-level operations are being added: encryption/decryption, PDF merging, and page removal.

PDFBox 3.x ships with built-in support for standard PDF security (RC4/AES) and can merge documents natively — no new Maven dependencies are needed.

## Goals / Non-Goals

**Goals:**
- Encrypt an open PDF with a user-supplied password (owner + user password, AES-256)
- Decrypt an encrypted PDF: prompt for password on open; allow saving unencrypted or re-encrypted
- Merge: append or prepend pages from one or more external PDF files into the current document
- Remove pages: select one or more pages from the current document by page number and delete them

**Non-Goals:**
- Certificate-based or public-key encryption
- Selective page encryption
- Merging annotations/forms from merged documents (pages are imported as rendered content only if merging requires flattening; otherwise structural merge is fine)
- Split-to-multiple-files (only removal/trimming in scope)
- Drag-and-drop reordering of pages (future)

## Decisions

### 1. New service classes per operation

Three new service classes alongside existing `PdfLoader`/`PdfSaver`:

| Class | Responsibility |
|---|---|
| `PdfEncryptionService` | Encrypt / decrypt PDDocument using PDFBox's `StandardProtectionPolicy` |
| `PdfMergeService` | Append or prepend PDDocument pages from external files into the open document |
| `PdfPageService` | Remove a set of page indices from the open PdfDocument |

**Alternative considered**: Adding methods to `PdfSaver` / `PdfLoader`. Rejected because encryption, merging, and page removal are document-structural operations unrelated to annotation serialization — mixing concerns would make the existing classes harder to test and reason about.

### 2. Document menu in MainWindow

New `Document` menu added to the menu bar in `MainWindow` (between `View` and any future menus):
- `Document > Encrypt…` — shows password dialog, encrypts and saves
- `Document > Decrypt…` — shows password dialog if encrypted, decrypts (optionally re-encrypts), saves
- `Document > Merge…` — shows file chooser + append/prepend choice, merges pages, reloads canvas
- `Document > Remove Pages…` — shows page-selection dialog, removes chosen pages, reloads canvas

**Alternative considered**: Toolbar buttons. Rejected — these are infrequent document-level actions that fit naturally in a menu rather than occupying toolbar space.

### 3. Encryption: owner password = user password (simplified)

PDFBox `StandardProtectionPolicy` requires both an owner password and a user password. For simplicity the user supplies one password; the app sets both owner and user password to the same value.

**Rationale**: Differentiated owner/user permissions are an advanced use case outside the scope of this feature. A single password dialog keeps the UX simple.

### 4. Merge imports pages structurally (not flattened)

`PDFMergerUtility` is used to copy pages at the PDFBox level. This preserves vector content, fonts, and images. Annotations from merged pages are NOT reconstructed as pdf-tapir annotation model objects — they remain as native PDF annotations that are visible but not interactively editable in the canvas.

**Alternative considered**: Flatten merged pages to images before inserting. Rejected because it degrades quality and loses text searchability.

### 5. Page removal reloads the entire PdfDocument

After page removal the `PdfDocument` model (in-memory) is rebuilt from the mutated `PDDocument`. `MainWindow` reloads the canvas to page 0 (or the nearest valid page) and resets the UndoManager.

**Rationale**: Page removal invalidates all page indices cached in annotations and undo history. A full reload is safer than patching indices throughout the model.

### 6. Encrypted PDF handling on open

`PdfLoader.load(File)` currently uses `Loader.loadPDF(file)`. If the file is password-protected PDFBox throws `InvalidPasswordException`. `MainWindow` will catch this, prompt for a password, and retry with `Loader.loadPDF(file, password)`.

## Risks / Trade-offs

- **Large PDFs and merge**: PDFBox loads the entire merged document into memory. Very large source files may cause OOM. → Mitigation: document the memory trade-off; no streaming workaround in scope.
- **Merge breaks undo history**: Merging pages resets the UndoManager (same as page removal). Users cannot undo a merge. → Mitigation: confirm dialog warns "This action cannot be undone."
- **Page removal with annotations**: Removing a page that contains pdf-tapir annotations removes those annotations permanently. The model is consistent (page + annotations both removed) but the user cannot undo. → Same mitigation: confirm dialog.
- **Re-encrypt changes password silently**: If a file was opened with password "A" and the user saves encrypted with password "B", there is no warning that the original password is being replaced. → Acceptable — the user explicitly chose to encrypt with a new password.
