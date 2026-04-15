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

## Usage

### Opening a file

**File → Open** (or `Ctrl+O`) — opens a PDF file. The first page is displayed on the canvas.

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

- **Text box** — edit the text content and font size
- **Checkbox** — edit the label and toggle the checked state
- **Image** — reposition and resize via the properties fields or by dragging on the canvas

### Page navigation

Use the **◀ ▶** buttons in the toolbar to move between pages.

### Undo / Redo

| Action | Shortcut |
|--------|----------|
| Undo | `Ctrl+Z` |
| Redo | `Ctrl+Y` |
| Delete selected | `Delete` or `Backspace` |

### Saving

**File → Save** (`Ctrl+S`) or **File → Save As** — writes annotations back to the PDF as native objects. Annotations created by this tool are tagged and will be reconstructed when the file is reopened.

## Tech stack

- **Java 21** + **JavaFX 21.0.4** — UI and canvas rendering
- **Apache PDFBox 3.0.2** — PDF reading and writing
- **Maven** — build

## License

MIT
