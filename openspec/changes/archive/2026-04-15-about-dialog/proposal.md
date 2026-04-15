## Why

PDF Tapir has no way for users to discover the project's identity, authorship, license, or source URL from within the app itself. An About dialog is the standard mechanism for surfacing this information and is expected in any desktop application.

## What Changes

- Add **Help → About PDF Tapir** menu item to the menu bar
- Implement a modal About dialog containing:
  - Application icon (ICON.png)
  - Project name: "PDF Tapir"
  - Source URL: https://github.com/chrisreichel/pdf-tapir
  - Developer: Christian Reichel
  - License: AGPL-3.0
  - Warranty disclaimer: the software is provided as-is with no warranty of any kind

## Capabilities

### New Capabilities

- `about-dialog`: Modal dialog accessible from the Help menu that displays project identity, authorship, license, and warranty disclaimer

### Modified Capabilities

<!-- None -->

## Impact

- `src/main/java/com/pdftapir/ui/` — new `AboutDialog.java` class; `MainWindow.java` gains a Help menu
- No new dependencies
- No data model or PDF persistence changes
