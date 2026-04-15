## Why

PDF Tapir stores annotation metadata within saved PDFs, enabling re-editing — but this increases file size. Users with large annotated PDFs have no way to produce a compact, final version stripped of editing metadata. A "flatten and finalize" export gives users control over when a document is truly finished.

## What Changes

- Add a **"Export Flattened PDF..."** option in the File menu (distinct from regular Save/Save As)
- When triggered, show a confirmation dialog warning that the exported file cannot be re-edited in PDF Tapir
- The export burns all annotations into the PDF page content and strips all PDF Tapir metadata
- The resulting file is a standard, compact PDF with no editable layers

## Capabilities

### New Capabilities
- `flatten-pdf-export`: Export a flattened, read-only PDF with annotations merged into page content and all PDF Tapir editing metadata removed, reducing file size

### Modified Capabilities

## Impact

- **File menu**: New menu item "Export Flattened PDF..." added alongside existing save actions
- **PDF save/export service**: New export path that flattens annotations using PDFBox and omits metadata
- **No impact** on existing save/load workflows — this is a separate export-only action
