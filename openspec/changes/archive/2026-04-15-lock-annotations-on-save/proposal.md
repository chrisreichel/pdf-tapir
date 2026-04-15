## Why

When a PDF saved by pdf-tapir is opened in an external viewer (Adobe Acrobat, macOS Preview, browsers), users can freely move, resize, delete, or edit the text fields, checkboxes, and image stamps placed by the app. The annotations should be treated as finalized output — only editable from within pdf-tapir — not as freely manipulable interactive objects.

## What Changes

- At save time, all pdf-tapir annotations (free-text, widget, rubber-stamp) SHALL have their PDF annotation flags set to `ReadOnly` + `Locked`, preventing external viewers from modifying or moving them.
- AcroForm checkbox fields SHALL also have their field-level `ReadOnly` flag set, preventing external form editors from changing the checked state.
- When pdf-tapir loads an already-saved PDF, it SHALL continue to load and render the locked annotations as fully editable within the app (the lock flags are stripped on read and re-applied on save).

## Capabilities

### New Capabilities

- `annotation-locking`: Annotations written by pdf-tapir are locked in the PDF so external viewers treat them as read-only, while the app itself can still edit them.

### Modified Capabilities

<!-- None — no existing spec-level requirements are changing -->

## Impact

- **`PdfSaver`**: Set annotation flags (`ReadOnly | Locked`) on every annotation written; set field-level `ReadOnly` bit on AcroForm checkbox fields.
- **`PdfLoader`**: No change needed — PDFBox reads locked annotations normally; the model does not carry or enforce lock state.
- **No new dependencies** — PDFBox already exposes `PDAnnotation.setAnnotationFlags()` and `PDField.setFieldFlags()`.
