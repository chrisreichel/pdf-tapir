## Why

Three bugs and a UX gap are affecting core workflows: the save action silently overwrites opened files without ever asking the user whether they want to overwrite or create a new copy; checkbox and image annotations are lost on save/reload; and there is no quick way to change zoom from the toolbar without opening a menu. These block reliable daily use of the tool.

## What Changes

- **Save intent prompt (first-time-per-file)**: When the user saves an already-opened file for the first time in a session, a dialog SHALL ask whether to overwrite the original or save as a new file. Subsequent saves in the same session skip the dialog and use the previous choice.
- **Fix checkbox annotation save**: Checkbox annotations (checked state, label, color) SHALL be correctly serialized and reloaded from PDF.
- **Fix image annotation save**: Image annotations SHALL be correctly serialized (image bytes embedded) and reloaded with the image data restored.
- **Zoom toolbar buttons**: A `−` button and a `+` button SHALL appear flanking the zoom percentage label in the toolbar, providing one-click zoom-out and zoom-in without opening the View menu.

## Capabilities

### New Capabilities

- `save-intent-prompt`: On first save of an opened file, ask the user to choose between overwrite and save-as-new; remember the choice for the session.
- `zoom-toolbar-buttons`: `−` and `+` buttons in the toolbar directly trigger zoom-out and zoom-in.

### Modified Capabilities

- `canvas-zoom`: Zoom can now also be triggered via toolbar `−`/`+` buttons (in addition to menu and Shift+scroll).

## Impact

- `MainWindow`: save flow — add first-save dialog logic and per-document session state.
- `PdfSaver`: fix checkbox serialization (checked state + checkmark color recovery) and image serialization (ensure image bytes are embedded in the PDF appearance stream and reloaded).
- `PdfLoader`: fix checkpoint annotation and image annotation reload.
- `EditorToolBar`: add `−` and `+` `Button` nodes flanking the zoom label; wire to `canvas.zoomOut()` / `canvas.zoomIn()`.
- No new external dependencies.
