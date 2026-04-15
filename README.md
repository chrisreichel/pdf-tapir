# PDF Tapir

An open source visual PDF editor. Open a PDF, place text boxes, checkboxes, and images on top of pages, and save the result back to the file. Annotations are stored as native PDF objects so they remain re-editable when the file is reopened.

## Requirements

- Java 21+
- Maven 3.8+

## Run

```bash
mvn javafx:run
```

## Build a fat JAR

```bash
mvn package
java -jar target/pdf-tapir-1.0.0-SNAPSHOT.jar
```

## Test

```bash
mvn test
```

## Features

### File operations

| Action | How |
|--------|-----|
| Open a PDF | **File → Open** |
| Close the current document | **File → Close** |
| Save (overwrite or save-as) | **File → Save** (`Ctrl+S`) / **File → Save As** |
| Export a read-only flattened copy | **File → Export Flattened PDF…** |
| Print via OS print dialog | **File → Print…** |

**Export Flattened PDF** burns all annotations permanently into the page content and strips the re-editable metadata, producing a clean PDF suitable for sharing. The original document stays open and editable.

**Print** sends all pages to the system's native print dialog using the OS print infrastructure. Page range and printer selection are handled by the OS dialog.

### Annotations

Three annotation types can be placed on any page:

| Type | Placement | Properties |
|------|-----------|------------|
| **Text box** | Click and drag to draw | Content, font size, bold, italic, alignment (left / centre / right) |
| **Checkbox** | Click and drag to place | Label, checked state, borderless mode |
| **Image** | Click and drag, then pick a file | Repositionable and resizable |

Annotations are stored as re-editable objects inside the PDF. Reopening the file in PDF Tapir restores full edit capability. External viewers see the annotations as fixed page content.

### Annotation formatting

Select a text annotation with the Select tool to access formatting in the right panel:

- **Bold / Italic** — toggle independently
- **Alignment** — left, centre, or right
- **Font size** — numeric input

Select a checkbox annotation to toggle **borderless mode** (renders the checkmark without a surrounding border).

### Page navigation and zoom

| Action | How |
|--------|-----|
| Previous / next page | **◀ ▶** toolbar buttons |
| Zoom in | **View → Zoom In** (`Ctrl+=`) |
| Zoom out | **View → Zoom Out** (`Ctrl+-`) |
| Fit page to window | **View → Fit Page** (`Ctrl+0`) |

### Document operations

| Action | How |
|--------|-----|
| Encrypt with a password | **Document → Encrypt…** |
| Decrypt (remove password) | **Document → Decrypt** |
| Merge another PDF | **Document → Merge…** (append or prepend) |
| Remove pages | **Document → Remove Pages…** |

### Undo / Redo

| Action | Shortcut |
|--------|----------|
| Undo | `Ctrl+Z` |
| Redo | `Ctrl+Y` |
| Delete selected annotation | `Delete` or `Backspace` |

## Usage

### Opening a file

**File → Open** — opens a PDF file. The first page is displayed on the canvas.

### Tools

Select a tool from the toolbar before interacting with the canvas:

| Tool | Description |
|------|-------------|
| **Select** | Click to select an annotation; drag to move it; drag a corner handle to resize it |
| **Text** | Click and drag to draw a text box |
| **Checkbox** | Click and drag to place a checkbox |
| **Image** | Click and drag to place an image; a file picker opens after the drag |

### Editing annotations

Select an annotation with the Select tool to show its properties in the right panel:

- **Text box** — edit the text content, font size, bold/italic, and alignment
- **Checkbox** — edit the label, toggle the checked state, and toggle borderless mode
- **Image** — reposition and resize via the properties fields or by dragging on the canvas

### Page navigation

Use the **◀ ▶** buttons in the toolbar to move between pages.

### Saving

**File → Save** (`Ctrl+S`) or **File → Save As** — writes annotations back to the PDF. Annotations are tagged and will be reconstructed when the file is reopened in PDF Tapir.

## Tech stack

- **Java 21** + **JavaFX 21.0.4** — UI and canvas rendering
- **Apache PDFBox 3.0.2** — PDF reading and writing
- **Maven** — build

## License

MIT
