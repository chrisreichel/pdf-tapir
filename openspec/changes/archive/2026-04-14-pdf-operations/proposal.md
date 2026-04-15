## Why

pdf-tapir currently only supports annotation workflows on existing PDFs. Users need document-level operations — encryption/decryption and structural editing (merging, page removal) — to manage their PDFs as first-class editing tasks rather than relying on external tools.

## What Changes

- Add **Encrypt PDF** action: prompt for a password and save a password-protected PDF
- Add **Decrypt PDF** action: unlock a password-protected PDF and allow saving it decrypted or re-encrypted with a new password
- Add **Merge PDFs** action: combine two or more PDFs by appending or prepending pages from other files
- Add **Remove Pages** action: select one or more pages from the current document and delete them, then save

## Capabilities

### New Capabilities

- `pdf-encryption`: Encrypt an open PDF with a password; decrypt a password-protected PDF on open or via menu action; save decrypted or re-encrypted
- `pdf-merge`: Append or prepend pages from one or more external PDFs into the currently open document
- `pdf-page-removal`: Select and delete one or more pages from the current document

### Modified Capabilities

<!-- None — no existing spec-level requirements are changing -->

## Impact

- **UI**: New menu items under a `Document` top-level menu (Encrypt, Decrypt, Merge, Remove Pages); page-selection UI for removal
- **Service layer**: New `PdfEncryptionService`, `PdfMergeService`, and `PdfPageService` classes (or methods added to existing services)
- **PdfLoader**: Must handle encrypted PDFs — prompt for password on load if document is encrypted
- **Dependencies**: PDFBox 3.x already supports standard security handlers; no new Maven dependencies expected
- **PdfDocument model**: May need to track encryption state (is currently encrypted, owner/user passwords) for save decisions
