# PDF Escroto — Visual PDF Editor Design

**Date:** 2026-04-14  
**Stack:** Java 21, JavaFX, Apache PDFBox, Maven  
**Status:** Approved

---

## Overview

PDF Escroto is an open source desktop application for visually annotating PDF files. Users can open a PDF, place text boxes, checkboxes, and images on top of pages, and save the result back to a PDF file. Annotations are stored as native PDF objects (free text annotations, AcroForm fields, stamp annotations) so they remain re-editable when the file is reopened in this tool.

---

## UI Layout

A single-window application with a dark theme:

- **Menu bar** — File (Open, Save, Save As), Edit (Undo, Redo), View (Zoom In, Zoom Out, Fit Page)
- **Toolbar** — tool buttons (Select, Text, Checkbox, Image), page navigation (prev/next, page n/total), zoom level display, Save button
- **Canvas area** — dark-background scroll area containing the PDF page rendered in white; annotations are drawn on top; supports scroll to pan and zoom via View menu / keyboard shortcuts
- **Properties panel** (right, 160px) — shows editable fields for the currently selected annotation: text content, font size, x/y position, width/height
- **Status bar** — displays selection state and cursor coordinates in PDF points

---

## Architecture

### Layers

**Model**
```
PdfDocument
└─ PdfPage[]
   ├─ WritableImage  (rendered snapshot)
   └─ Annotation[]
      ├─ TextAnnotation      (x, y, w, h, text, fontSize, fontColor)
      ├─ CheckboxAnnotation  (x, y, w, h, label, checked)
      └─ ImageAnnotation     (x, y, w, h, imagePath, imageData)
```

**View**
- `MainWindow` — root `BorderPane` scene
- `MenuBar` — standard JavaFX menu bar
- `ToolBar` — tool toggle buttons
- `PdfCanvas` — extends `Canvas`; owns the render loop, mouse event handling (create, select, drag, resize), and zoom/pan state
- `PropertiesPanel` — `VBox` with form fields; binds to the selected annotation via JavaFX properties

**Services**
- `PdfLoader` — opens a `PDDocument`, reads existing annotations/AcroForm fields back into the model
- `PdfRenderer` — uses PDFBox `PDFRenderer` to render each page to a `BufferedImage`, converted to `WritableImage`
- `PdfSaver` — maps model annotations to PDFBox types and writes to disk (temp-file-then-rename strategy)
- `CoordinateMapper` — converts between canvas pixels (top-left origin) and PDF points (bottom-left origin, 1 pt = 1/72 in) at the current zoom level

### Annotation → PDF Type Mapping

| Tool       | PDFBox Type                          | Re-editable |
|------------|--------------------------------------|-------------|
| Text box   | `PDAnnotationFreeText`               | Yes         |
| Checkbox   | `PDCheckBox` (AcroForm widget)       | Yes         |
| Image      | `PDAnnotationStamp` + `PDImageXObject` | Yes       |

All annotations written by this tool include a custom `Contents` or `Subject` field tagged with `pdf-escroto` so `PdfLoader` can identify and reconstruct them on reopen.

### Undo / Redo

Command pattern with an `UndoManager` holding a bounded stack:

- `AddAnnotationCommand`
- `MoveAnnotationCommand`
- `ResizeAnnotationCommand`
- `DeleteAnnotationCommand`
- `EditAnnotationCommand` (text/property changes)

### Threading

- PDF load and save run on a JavaFX background `Task<Void>` to keep the UI responsive
- All canvas rendering and model mutations happen on the JavaFX Application Thread
- Progress indicator shown in status bar during load/save

---

## Data Flow

```
Open PDF
  → PdfLoader reads PDDocument
  → PdfRenderer renders pages to WritableImage[]
  → PdfCanvas displays current page + annotations

User selects tool, clicks/drags on canvas
  → PdfCanvas creates Annotation, adds to PdfPage.annotations
  → canvas redraws

User drags/resizes annotation
  → Annotation x/y/w/h updated via MoveAnnotationCommand / ResizeAnnotationCommand
  → canvas redraws, PropertiesPanel updates

Save
  → PdfSaver iterates pages and annotations
  → CoordinateMapper converts to PDF coordinates
  → PDFBox writes AcroForm fields / annotations to PDDocument
  → Writes to temp file, renames over original on success
```

---

## Error Handling

| Scenario | Behavior |
|---|---|
| Corrupted / unreadable PDF | Alert dialog; app stays on current document |
| Encrypted PDF | Clear error message; file not opened |
| Page render failure | Placeholder shown for that page; other pages load normally |
| Save write failure | Alert dialog; original file not touched (temp-file strategy) |
| Unsupported image format on import | Alert dialog; import cancelled |
| Oversized image import | Auto-scaled down to fit page dimensions |

---

## Testing Strategy

**Unit tests** (JUnit 5):
- `CoordinateMapper` — round-trip conversions at various zoom levels
- `PdfSaver` + `PdfLoader` — write annotations, read back, assert fields match
- `UndoManager` — undo/redo sequences, stack bounds
- Each `Annotation` subclass — property accessors

**Integration tests**:
- Open a real PDF, add one of each annotation type, save, reopen, assert annotations survive the round-trip
- Test with multi-page PDFs
- Test with PDFs that already contain AcroForm fields

**UI testing**: Manual smoke test checklist (canvas interaction not automated).

---

## Build & Distribution

- **Build tool:** Maven
- **Java version:** 21
- **Key dependencies:** `org.apache.pdfbox:pdfbox`, `org.openjfx:javafx-controls`, `org.openjfx:javafx-fxml`
- **Distribution:** Executable fat JAR via `maven-shade-plugin`; later optionally packaged as a native installer with `jpackage`

---

## Out of Scope (v1)

- PDF text selection or editing of existing PDF content
- Multi-page drag (annotations are per-page)
- Collaboration / cloud sync
- PDF form filling beyond checkboxes
- Annotation styling beyond font size and color for text
