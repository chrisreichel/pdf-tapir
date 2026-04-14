## 1. PdfCanvas — expose scale as property

- [x] 1.1 Replace `private double scale` with `private final DoubleProperty scale = new SimpleDoubleProperty(1.0)` in `PdfCanvas`
- [x] 1.2 Replace all reads of `scale` with `scale.get()` throughout `PdfCanvas`
- [x] 1.3 Add `public DoubleProperty scaleProperty()` and `public double getScale()` accessors
- [x] 1.4 Add `public void setScale(double s)` that clamps to [0.25, 4.0], calls `scale.set(clamped)`, and calls `redraw()`
- [x] 1.5 Add `public void zoomIn()` — calls `setScale(getScale() * 1.25)`
- [x] 1.6 Add `public void zoomOut()` — calls `setScale(getScale() / 1.25)`
- [x] 1.7 Add `public void fitPage(ScrollPane sp)` — computes `min(viewport.width / pageWidthPt, viewport.height / pageHeightPt)`, calls `setScale(computed)`

## 2. EditorToolBar — live zoom label

- [x] 2.1 Replace the hardcoded `"100%"` label node with a `Label zoomLabel = new Label("100%")` field
- [x] 2.2 Add `zoomLabel` to the toolbar node (between page nav and save button)
- [x] 2.3 In `bindCanvas(PdfCanvas canvas)`, add a listener on `canvas.scaleProperty()` that updates `zoomLabel.setText(Math.round(newVal * 100) + "%")`
- [x] 2.4 Set initial label text in `bindCanvas` to match canvas's current scale

## 3. MainWindow — View menu actions

- [x] 3.1 Store the `ScrollPane` wrapping the canvas as a field (already created in `attachDocument`) so `fitPage` can reference it
- [x] 3.2 In `buildMenuBar()` (or `attachDocument`), add View menu items: **Zoom In** (`Ctrl+=`), **Zoom Out** (`Ctrl+-`), **Fit Page** (`Ctrl+0`)
- [x] 3.3 Wire Zoom In action to `canvas.zoomIn()`
- [x] 3.4 Wire Zoom Out action to `canvas.zoomOut()`
- [x] 3.5 Wire Fit Page action to `canvas.fitPage(scrollPane)`
- [x] 3.6 Add `Ctrl+=` / `Ctrl+-` / `Ctrl+0` keyboard accelerators to the menu items

## 4. Verify

- [x] 4.1 Run `mvn test` — all 16 tests pass
- [ ] 4.2 Launch app (`mvn javafx:run`), open a PDF, verify Zoom In/Out changes canvas size and toolbar label updates
- [ ] 4.3 Verify Fit Page scales the page to fill the visible area
- [ ] 4.4 Verify zoom cannot go below 25% or above 400%
- [x] 4.5 Commit: `feat: add zoom in/out/fit-page to View menu`
