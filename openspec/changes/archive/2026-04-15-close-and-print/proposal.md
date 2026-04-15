## Why

There is currently no way to close the open document without quitting the entire application, forcing users to exit and relaunch when they want to start fresh or switch files. Printing is also absent — users must open the PDF in another viewer just to send it to a printer.

## What Changes

- **Close File**: A "Close" menu item in the File menu closes the current document and returns the app to an empty state, without quitting
- **Print**: A "Print…" menu item in the File menu opens the OS native print dialog for the current PDF, using Java's `PrinterJob` API

## Capabilities

### New Capabilities
- `close-file`: Close the open document and return the application to an empty (no document open) state without exiting
- `print-pdf`: Send the current PDF to the OS print dialog using the system print infrastructure

### Modified Capabilities

## Impact

- **`MainWindow`**: new `closeFile()` and `printDocument()` methods; new menu items in the File menu; app state management when no document is open (canvas and toolbar removed, menu items disabled)
- **`pom.xml`**: no new dependencies — `javafx-swing` (already present) provides `PrinterJob` and JavaFX print support; `javafx-print` module may need to be added
