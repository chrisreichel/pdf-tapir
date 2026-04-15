## Context

PDF annotations carry a 32-bit integer flags field (`F` key). Two bits are relevant:

| Bit | Value | Name | Effect |
|-----|-------|------|--------|
| 7 | 64 | `ReadOnly` | Viewer must not allow the user to change or delete the annotation |
| 8 | 128 | `Locked` | Viewer must not allow position/size changes; properties dialog may be suppressed |

AcroForm fields carry a separate flags integer (`Ff` key). Bit 1 (value 1) is the field-level `ReadOnly` flag; when set, the field value cannot be changed from an external form editor.

PDFBox 3.x exposes these via:
- `PDAnnotation.setAnnotationFlags(int)` / `getAnnotationFlags()`
- `PDField.setFieldFlags(int)` / `getFieldFlags()` (for checkboxes)

`PdfSaver` is the single write path for all pdf-tapir annotations. The fix is wholly contained there — no model or loader changes needed.

## Goals / Non-Goals

**Goals:**
- All three annotation types (free-text, rubber-stamp, widget) have `ReadOnly | Locked` flags set in the PDF on every save.
- Checkbox AcroForm fields have the field-level `ReadOnly` flag set.
- Opening the saved PDF in pdf-tapir still allows full editing (PDFBox ignores lock flags when reading; lock is re-applied on the next save).

**Non-Goals:**
- PDF encryption / permissions passwords (a separate feature in `pdf-operations`).
- Locking at the document level (not per-annotation).
- Exposing a "lock/unlock" toggle in the UI — the lock is always applied on save, always transparent to the in-app editing experience.

## Decisions

### Set both `ReadOnly` and `Locked`

`ReadOnly` alone prevents value changes but some viewers still let users drag annotations. `Locked` prevents position/size changes. Using both together gives the broadest cross-viewer protection.

**Alternative considered**: `ReadOnly` only. Rejected — observed that Preview and Acrobat Reader still allow dragging with only `ReadOnly` set.

### Apply flags in `PdfSaver`, not in annotation model

Annotation model objects (`TextAnnotation`, `CheckboxAnnotation`, `ImageAnnotation`) are in-memory editing objects and should not carry PDF-level lock state. The lock is a serialization concern, not a business-object concern.

**Alternative considered**: Storing a `locked` boolean on `Annotation`. Rejected — introduces complexity with no benefit since the lock is always-on for all annotations.

### `PdfLoader` makes no changes

PDFBox reads locked annotations into the same `PDAnnotation` subclasses regardless of flags. The model objects reconstructed by `PdfLoader` are fully mutable. The loader does not need to strip flags explicitly — they are simply ignored in the reading path.

## Risks / Trade-offs

- **Viewer compliance**: The `ReadOnly`/`Locked` bits are advisory in PDF. Most viewers respect them (Acrobat, Preview) but a determined user with a PDF editor can still clear the flags. Full tamper-proofing would require encryption, which is out of scope here.
- **Checkbox checked state**: Setting `ReadOnly` on the field means clicking the checkbox in external viewers won't toggle it. This is the desired behaviour.
