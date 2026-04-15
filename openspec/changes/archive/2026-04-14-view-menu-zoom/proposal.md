## Why

The View menu exists in the UI but zoom controls are unimplemented — `scale` is a field on `PdfCanvas` but is never mutated, and there is no way for the user to zoom in, zoom out, or fit the page to the window. This makes the editor difficult to use on large or small PDF pages.

## What Changes

- Add **Zoom In**, **Zoom Out**, and **Fit Page** items to the View menu with keyboard shortcuts
- Wire menu actions to mutate `PdfCanvas.scale` and trigger a redraw
- Update the zoom level label in the toolbar to reflect the current scale
- Clamp zoom to a sensible range (e.g. 25%–400%)

## Capabilities

### New Capabilities

- `canvas-zoom`: User can zoom the PDF canvas in/out and fit the page, with the toolbar label reflecting the current zoom level

### Modified Capabilities

<!-- none — no existing specs to delta -->

## Impact

- `PdfCanvas.java` — add `setScale(double)` method and expose current scale as a property
- `MainWindow.java` — wire View menu items to `PdfCanvas`
- `EditorToolBar.java` — bind zoom label to `PdfCanvas` scale property
- No new dependencies required
