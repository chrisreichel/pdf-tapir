## Context

`PdfCanvas` has a `scale` field (default `1.0`) used in all rendering and coordinate-mapping calls, but nothing ever mutates it. The View menu exists in `MainWindow.buildMenuBar()` but has no items. The toolbar shows a hardcoded `"100%"` string that is never updated.

The three files involved have clear responsibilities:
- `PdfCanvas` — owns scale state and canvas size
- `MainWindow` — owns the menu bar
- `EditorToolBar` — owns the toolbar label

## Goals / Non-Goals

**Goals:**
- Zoom In / Zoom Out / Fit Page actions that mutate `PdfCanvas.scale` and trigger a redraw
- Keyboard shortcuts: `Ctrl+=` (zoom in), `Ctrl+-` (zoom out), `Ctrl+0` (fit page)
- Zoom clamped to 25%–400%
- Toolbar zoom label updates live as scale changes

**Non-Goals:**
- Pinch-to-zoom or scroll-wheel zoom
- Per-page zoom levels
- Saving zoom level to disk

## Decisions

**Expose scale as a JavaFX `DoubleProperty` on `PdfCanvas`**
Rationale: allows `EditorToolBar` to bind its label directly (`canvas.scaleProperty().addListener(...)`) without polling or callbacks. Alternatives: callback/listener interface — more boilerplate; direct method call from `MainWindow` to both canvas and toolbar — tight coupling.

**Fit Page calculates scale from page dimensions vs. scroll pane viewport**
`scale = min(viewportWidth / pageWidthPt, viewportHeight / pageHeightPt)`. `PdfCanvas` needs a reference to the `ScrollPane`'s viewport so it can read its size. `MainWindow` passes the `ScrollPane` into `PdfCanvas.fitPage()` at call time — no stored reference needed.

**Zoom step: ×1.25 / ÷1.25**
Standard desktop zoom step. Produces clean levels: 100% → 125% → 156% → 195%... Clamped after each step.

## Risks / Trade-offs

- [Canvas size changes on zoom] `PdfCanvas.setWidth/Height` is called in `redraw()` — zooming simply triggers a redraw and the canvas resizes correctly. The `ScrollPane` handles overflow via scrollbars automatically.
- [Fit Page on first load] On document open, viewport size may be 0 before layout pass. Fit Page should only be triggered by explicit user action, not automatically on load.
