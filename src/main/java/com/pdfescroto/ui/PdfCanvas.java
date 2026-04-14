package com.pdfescroto.ui;

import com.pdfescroto.command.DeleteAnnotationCommand;
import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.*;
import com.pdfescroto.service.CoordinateMapper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Canvas that renders the current PDF page image and overlays interactive
 * annotation handles for the select, text, checkbox, and image tools.
 *
 * <p>Mouse interaction is wired in Task 12 by overriding the protected
 * {@code onMousePressed}, {@code onMouseDragged}, and {@code onMouseReleased}
 * hooks. This class handles rendering only.</p>
 */
public class PdfCanvas extends Canvas {

    private final PdfDocument     document;
    private final UndoManager     undoManager;
    private final PropertiesPanel propertiesPanel;

    private PdfPage    currentPage;
    private int        currentPageIndex    = 0;
    private double     scale               = 1.0;
    private Tool       activeTool          = Tool.SELECT;
    private Annotation selectedAnnotation;

    // Drag/create state (used in Task 12)
    private boolean    isDragging;
    private double     dragStartX, dragStartY;
    private double     annotStartX, annotStartY;
    private boolean    isCreating;
    private double     createStartX, createStartY;
    private Annotation creatingAnnotation;
    private String     resizeHandle;          // "NW","NE","SW","SE" or null
    private double     oldAnnotW, oldAnnotH;  // old size at resize start

    /** Side length of each corner selection handle square, in canvas pixels. */
    protected static final double HANDLE_SIZE = 8.0;

    /**
     * Creates the canvas for the given document, wired to the shared undo manager
     * and the properties panel that should be updated on selection changes.
     *
     * @param document the currently open PDF document
     * @param undoManager the shared undo/redo manager
     * @param propertiesPanel the properties panel to notify when an annotation is selected
     */
    public PdfCanvas(PdfDocument document, UndoManager undoManager, PropertiesPanel propertiesPanel) {
        super(800, 1000);
        this.document        = document;
        this.undoManager     = undoManager;
        this.propertiesPanel = propertiesPanel;
        goToPage(0);
        setupMouseHandlers();
    }

    // ---- Page navigation ----

    /**
     * Navigates to the specified page, resets the selection, and redraws.
     *
     * @param index the zero-based page index; out-of-range values are ignored
     */
    public void goToPage(int index) {
        if (index < 0 || index >= document.getPages().size()) return;
        currentPageIndex   = index;
        currentPage        = document.getPages().get(index);
        selectedAnnotation = null;
        propertiesPanel.showAnnotation(null);
        resizeCanvasToPage();
        redraw();
    }

    private void resizeCanvasToPage() {
        if (currentPage == null) return;
        setWidth(currentPage.getPageWidthPt()  * scale);
        setHeight(currentPage.getPageHeightPt() * scale);
    }

    // ---- Rendering ----

    /**
     * Clears the canvas and repaints the current page image together with all
     * annotation overlays and (if applicable) the in-progress creation ghost.
     */
    public void redraw() {
        if (currentPage == null) return;
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // Draw PDF page image
        var img = currentPage.getRenderedImage();
        if (img != null) gc.drawImage(img, 0, 0, getWidth(), getHeight());

        // Draw annotations
        var mapper = mapper();
        for (var annotation : currentPage.getAnnotations()) {
            drawAnnotation(gc, annotation, mapper, annotation == selectedAnnotation);
        }

        // Draw in-progress creation overlay
        if (isCreating && creatingAnnotation != null) {
            drawAnnotation(gc, creatingAnnotation, mapper, false);
        }
    }

    private void drawAnnotation(GraphicsContext gc, Annotation a,
                                CoordinateMapper mapper, boolean selected) {
        double cx = mapper.pdfXToCanvas(a.getX());
        double cy = mapper.pdfYToCanvasTop(a.getY(), a.getHeight());
        double cw = mapper.pdfDimToCanvas(a.getWidth());
        double ch = mapper.pdfDimToCanvas(a.getHeight());

        if (a instanceof TextAnnotation ta) {
            gc.setFill(Color.rgb(255, 255, 255, 0.05));
            gc.fillRect(cx, cy, cw, ch);
            gc.setStroke(selected ? Color.DODGERBLUE : Color.rgb(74, 123, 189, 0.8));
            gc.setLineWidth(selected ? 2.0 : 1.0);
            gc.strokeRect(cx, cy, cw, ch);
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(ta.getFontSize() * scale));
            gc.fillText(ta.getText(), cx + 3, cy + ta.getFontSize() * scale, cw - 6);

        } else if (a instanceof CheckboxAnnotation ca) {
            gc.setStroke(selected ? Color.DODGERBLUE : Color.rgb(46, 125, 50, 0.9));
            gc.setLineWidth(selected ? 2.0 : 1.5);
            gc.strokeRect(cx, cy, cw, ch);
            if (ca.isChecked()) {
                gc.setStroke(Color.rgb(46, 125, 50));
                gc.setLineWidth(2.0);
                // Draw a checkmark inside
                gc.strokeLine(cx + 3, cy + ch * 0.55, cx + cw * 0.4, cy + ch - 3);
                gc.strokeLine(cx + cw * 0.4, cy + ch - 3, cx + cw - 3, cy + 3);
            }

        } else if (a instanceof ImageAnnotation ia) {
            if (ia.getFxImage() != null) {
                gc.drawImage(ia.getFxImage(), cx, cy, cw, ch);
            } else {
                gc.setFill(Color.rgb(230, 81, 0, 0.1));
                gc.fillRect(cx, cy, cw, ch);
                gc.setStroke(Color.rgb(230, 81, 0, 0.8));
                gc.setLineWidth(1.0);
                gc.strokeRect(cx, cy, cw, ch);
            }
        }

        if (selected) drawHandles(gc, cx, cy, cw, ch);
    }

    private void drawHandles(GraphicsContext gc, double cx, double cy, double cw, double ch) {
        gc.setFill(Color.DODGERBLUE);
        double h = HANDLE_SIZE;
        gc.fillRect(cx - h / 2,      cy - h / 2,      h, h);
        gc.fillRect(cx + cw - h / 2, cy - h / 2,      h, h);
        gc.fillRect(cx - h / 2,      cy + ch - h / 2, h, h);
        gc.fillRect(cx + cw - h / 2, cy + ch - h / 2, h, h);
    }

    // ---- Mouse handler setup (overridden in Task 12) ----

    private void setupMouseHandlers() {
        setOnMousePressed(e  -> onMousePressed(e.getX(),  e.getY(),  e.isPrimaryButtonDown()));
        setOnMouseDragged(e  -> onMouseDragged(e.getX(),  e.getY()));
        setOnMouseReleased(e -> onMouseReleased(e.getX(), e.getY()));
    }

    /**
     * Invoked when a mouse button is pressed on the canvas.
     * In SELECT mode, performs a hit test against annotations and begins a drag move.
     * In creation modes, records the PDF-space start point and creates a zero-size ghost.
     *
     * @param cx      canvas X coordinate of the press
     * @param cy      canvas Y coordinate of the press
     * @param primary {@code true} if the primary (left) mouse button is down
     */
    protected void onMousePressed(double cx, double cy, boolean primary) {
        if (!primary || currentPage == null) return;
        var mapper = mapper();

        if (getActiveTool() == Tool.SELECT) {
            // Check if clicking a resize handle of the selected annotation
            if (getSelectedAnnotation() != null) {
                String handle = hitTestHandle(cx, cy, getSelectedAnnotation(), mapper);
                if (handle != null) {
                    resizeHandle = handle;
                    setIsDragging(false);
                    setDragStart(cx, cy);
                    setAnnotStart(getSelectedAnnotation().getX(), getSelectedAnnotation().getY());
                    oldAnnotW = getSelectedAnnotation().getWidth();
                    oldAnnotH = getSelectedAnnotation().getHeight();
                    return;
                }
            }
            resizeHandle = null;

            // Hit test annotations in reverse order (top-most first)
            var annotations = currentPage.getAnnotations();
            Annotation hit = null;
            for (int i = annotations.size() - 1; i >= 0; i--) {
                if (hitTest(annotations.get(i), cx, cy, mapper)) {
                    hit = annotations.get(i);
                    break;
                }
            }
            setSelectedAnnotation(hit);
            if (hit != null) {
                setIsDragging(true);
                setDragStart(cx, cy);
                setAnnotStart(hit.getX(), hit.getY());
            }
            redraw();

        } else {
            // Creation tools: record start in PDF coords
            double pdfX = mapper.canvasXToPdf(cx);
            double pdfY = mapper.canvasYToPdf(cy);
            setCreateStart(pdfX, pdfY);
            setCreating(true);
            setCreatingAnnotation(createAnnotation(pdfX, pdfY, 0, 0));
            setSelectedAnnotation(null);
            redraw();
        }
    }

    /**
     * Invoked while the mouse is dragged across the canvas.
     * In SELECT mode with an active drag, moves the selected annotation.
     * In creation mode, resizes the ghost annotation to match the current mouse position.
     *
     * @param cx canvas X coordinate
     * @param cy canvas Y coordinate
     */
    protected void onMouseDragged(double cx, double cy) {
        if (currentPage == null) return;
        var mapper = mapper();

        // Resize via handle drag
        if (getActiveTool() == Tool.SELECT && resizeHandle != null && getSelectedAnnotation() != null) {
            var ann    = getSelectedAnnotation();
            double pdfX = mapper.canvasXToPdf(cx);
            double pdfY = mapper.canvasYToPdf(cy);
            double x = ann.getX(), y = ann.getY(), w = ann.getWidth(), h = ann.getHeight();
            // Compute fixed corner (the corner opposite the dragged handle) in PDF space
            double fixedPdfX, fixedPdfY;
            switch (resizeHandle) {
                case "SE" -> { fixedPdfX = x;     fixedPdfY = y + h; } // drag SE → fixed NW (PDF top-left)
                case "NW" -> { fixedPdfX = x + w; fixedPdfY = y;     } // drag NW → fixed SE (PDF bottom-right)
                case "NE" -> { fixedPdfX = x;     fixedPdfY = y;     } // drag NE → fixed SW (PDF bottom-left)
                case "SW" -> { fixedPdfX = x + w; fixedPdfY = y + h; } // drag SW → fixed NE (PDF top-right)
                default   -> { return; }
            }
            double newX = Math.min(fixedPdfX, pdfX);
            double newY = Math.min(fixedPdfY, pdfY);
            double newW = Math.abs(pdfX - fixedPdfX);
            double newH = Math.abs(pdfY - fixedPdfY);
            if (newW > 1 && newH > 1) {
                ann.setX(newX); ann.setY(newY);
                ann.setWidth(newW); ann.setHeight(newH);
                getPropertiesPanel().showAnnotation(ann);
                redraw();
            }
            return;
        }

        if (getActiveTool() == Tool.SELECT && getIsDragging() && getSelectedAnnotation() != null) {
            // Move: translate PDF coords by delta
            double dx =  mapper.canvasDimToPdf(cx - getDragStartX());
            double dy = -mapper.canvasDimToPdf(cy - getDragStartY()); // Y axis flip
            var ann = getSelectedAnnotation();
            ann.setX(getAnnotStartX() + dx);
            ann.setY(getAnnotStartY() + dy);
            getPropertiesPanel().showAnnotation(ann);
            redraw();

        } else if (isCreating() && getCreatingAnnotation() != null) {
            // Resize creation ghost from start corner to current mouse position
            double pdfX = mapper.canvasXToPdf(cx);
            double pdfY = mapper.canvasYToPdf(cy);
            double x = Math.min(getCreateStartX(), pdfX);
            double y = Math.min(getCreateStartY(), pdfY);
            double w = Math.abs(pdfX - getCreateStartX());
            double h = Math.abs(pdfY - getCreateStartY());
            var ann = getCreatingAnnotation();
            // Bypass the constructor guard — allow zero during drag
            ann.setX(x); ann.setY(y); ann.setWidth(w); ann.setHeight(h);
            redraw();
        }
    }

    /**
     * Invoked when a mouse button is released on the canvas.
     * In SELECT mode, commits a completed drag as an undoable command.
     * In creation mode, commits the new annotation if its size exceeds a minimum threshold.
     *
     * @param cx canvas X coordinate of the release
     * @param cy canvas Y coordinate of the release
     */
    protected void onMouseReleased(double cx, double cy) {
        if (currentPage == null) return;

        // Commit resize
        if (getActiveTool() == Tool.SELECT && resizeHandle != null && getSelectedAnnotation() != null) {
            var ann    = getSelectedAnnotation();
            double finalX = ann.getX(), finalY = ann.getY();
            double finalW = ann.getWidth(), finalH = ann.getHeight();
            double startX = getAnnotStartX(), startY = getAnnotStartY();
            // Reset to original so ResizeAnnotationCommand.execute() applies the resize correctly
            ann.setX(startX); ann.setY(startY);
            ann.setWidth(oldAnnotW); ann.setHeight(oldAnnotH);
            getUndoManager().execute(new com.pdfescroto.command.ResizeAnnotationCommand(
                    ann, startX, startY, oldAnnotW, oldAnnotH,
                        finalX, finalY, finalW, finalH));
            resizeHandle = null;
            redraw();
            return;
        }

        if (getActiveTool() == Tool.SELECT && getIsDragging() && getSelectedAnnotation() != null) {
            // Commit move as undoable command (only if the annotation actually moved)
            var ann   = getSelectedAnnotation();
            double finalX = ann.getX();
            double finalY = ann.getY();
            double oldX   = getAnnotStartX();
            double oldY   = getAnnotStartY();
            if (Math.abs(finalX - oldX) > 0.5 || Math.abs(finalY - oldY) > 0.5) {
                // Reset to old position so MoveAnnotationCommand.execute() applies the move correctly
                ann.setX(oldX);
                ann.setY(oldY);
                getUndoManager().execute(new com.pdfescroto.command.MoveAnnotationCommand(
                        ann, oldX, oldY, finalX, finalY));
            }
            setIsDragging(false);
            redraw();

        } else if (isCreating() && getCreatingAnnotation() != null) {
            var ann = getCreatingAnnotation();
            // Only commit if the annotation has meaningful size (not a stray click)
            if (ann.getWidth() > 2 && ann.getHeight() > 2) {
                getUndoManager().execute(
                        new com.pdfescroto.command.AddAnnotationCommand(getCurrentPage(), ann));
                setSelectedAnnotation(ann);

                // For image annotations: prompt file chooser after creation
                if (getActiveTool() == Tool.IMAGE) {
                    promptImageFile((ImageAnnotation) ann);
                }
            }
            setCreating(false);
            setCreatingAnnotation(null);
            redraw();
        }
    }

    // ---- Actions ----

    /**
     * Deletes the currently selected annotation by executing a
     * {@link DeleteAnnotationCommand} through the undo manager.
     */
    public void deleteSelected() {
        if (selectedAnnotation == null || currentPage == null) return;
        undoManager.execute(new DeleteAnnotationCommand(currentPage, selectedAnnotation));
        selectedAnnotation = null;
        propertiesPanel.showAnnotation(null);
        redraw();
    }

    /**
     * Switches the active editing tool.
     *
     * @param tool the tool to activate
     */
    public void setActiveTool(Tool tool) { this.activeTool = tool; }

    // ---- Protected accessors for Task 12 mouse logic ----

    /**
     * Returns a {@link CoordinateMapper} calibrated to the current page and scale.
     *
     * @return coordinate mapper for the current view state
     */
    protected CoordinateMapper mapper() {
        return new CoordinateMapper(currentPage.getPageHeightPt(), scale);
    }

    /** @return the currently displayed page */
    protected PdfPage       getCurrentPage()            { return currentPage; }

    /** @return the currently active tool */
    protected Tool          getActiveTool()             { return activeTool; }

    /** @return the currently selected annotation, or {@code null} */
    protected Annotation    getSelectedAnnotation()     { return selectedAnnotation; }

    /**
     * Selects the given annotation and notifies the properties panel.
     *
     * @param a the annotation to select, or {@code null} to clear selection
     */
    protected void          setSelectedAnnotation(Annotation a) {
        selectedAnnotation = a;
        propertiesPanel.showAnnotation(a);
    }

    /** @return the shared undo manager */
    protected UndoManager   getUndoManager()            { return undoManager; }

    /** @return the properties panel wired to this canvas */
    protected PropertiesPanel getPropertiesPanel()      { return propertiesPanel; }

    /** @return {@code true} if an annotation is currently being drawn */
    protected boolean       isCreating()                { return isCreating; }

    /**
     * Sets the in-progress creation flag.
     *
     * @param b {@code true} while drawing a new annotation
     */
    protected void          setCreating(boolean b)      { isCreating = b; }

    /** @return the canvas X coordinate where the current drag began */
    protected double        getDragStartX()             { return dragStartX; }

    /** @return the canvas Y coordinate where the current drag began */
    protected double        getDragStartY()             { return dragStartY; }

    /** @return the PDF X coordinate of the annotation at drag start */
    protected double        getAnnotStartX()            { return annotStartX; }

    /** @return the PDF Y coordinate of the annotation at drag start */
    protected double        getAnnotStartY()            { return annotStartY; }

    /** @return the canvas X coordinate where the current creation drag began */
    protected double        getCreateStartX()           { return createStartX; }

    /** @return the canvas Y coordinate where the current creation drag began */
    protected double        getCreateStartY()           { return createStartY; }

    /** @return the annotation being drawn, or {@code null} */
    protected Annotation    getCreatingAnnotation()     { return creatingAnnotation; }

    /**
     * Records the canvas coordinates where the current drag started.
     *
     * @param x canvas X
     * @param y canvas Y
     */
    protected void          setDragStart(double x, double y)    { dragStartX = x; dragStartY = y; }

    /**
     * Records the PDF-space position of the annotation at the start of a drag.
     *
     * @param x PDF X
     * @param y PDF Y
     */
    protected void          setAnnotStart(double x, double y)   { annotStartX = x; annotStartY = y; }

    /**
     * Records the canvas coordinates where the current creation drag started.
     *
     * @param x canvas X
     * @param y canvas Y
     */
    protected void          setCreateStart(double x, double y)  { createStartX = x; createStartY = y; }

    /**
     * Sets the annotation instance being drawn during a creation drag.
     *
     * @param a the partially constructed annotation
     */
    protected void          setCreatingAnnotation(Annotation a) { creatingAnnotation = a; }

    /**
     * Sets the drag-in-progress flag.
     *
     * @param b {@code true} while a drag move is active
     */
    protected void          setIsDragging(boolean b)            { isDragging = b; }

    /** @return {@code true} while a drag move is active */
    protected boolean       getIsDragging()                     { return isDragging; }

    // ---- Private helpers ----

    /**
     * Returns {@code true} if the canvas point (cx, cy) falls within the bounding box
     * of the given annotation.
     *
     * @param a      the annotation to test
     * @param cx     canvas X coordinate of the point to test
     * @param cy     canvas Y coordinate of the point to test
     * @param mapper the coordinate mapper for the current view state
     * @return {@code true} if the point is inside the annotation bounds
     */
    private boolean hitTest(Annotation a, double cx, double cy, CoordinateMapper mapper) {
        double ax = mapper.pdfXToCanvas(a.getX());
        double ay = mapper.pdfYToCanvasTop(a.getY(), a.getHeight());
        double aw = mapper.pdfDimToCanvas(a.getWidth());
        double ah = mapper.pdfDimToCanvas(a.getHeight());
        return cx >= ax && cx <= ax + aw && cy >= ay && cy <= ay + ah;
    }

    /**
     * Creates a new annotation of the type corresponding to the current active tool,
     * positioned at the given PDF coordinates with the given dimensions.
     *
     * @param pdfX PDF X coordinate (points)
     * @param pdfY PDF Y coordinate (points)
     * @param w    width in PDF points
     * @param h    height in PDF points
     * @return a new annotation instance for the active creation tool
     * @throws IllegalStateException if the active tool is not a creation tool
     */
    private Annotation createAnnotation(double pdfX, double pdfY, double w, double h) {
        return switch (getActiveTool()) {
            case TEXT     -> new TextAnnotation(pdfX, pdfY, w, h);
            case CHECKBOX -> new CheckboxAnnotation(pdfX, pdfY, w, h);
            case IMAGE    -> new ImageAnnotation(pdfX, pdfY, w, h);
            default       -> throw new IllegalStateException("Not a creation tool: " + getActiveTool());
        };
    }

    /**
     * Returns the handle name ("NW", "NE", "SW", "SE") if (cx,cy) is within HANDLE_SIZE
     * pixels of a corner handle of annotation {@code a}, or {@code null} if no handle hit.
     *
     * @param cx     canvas X coordinate of the test point
     * @param cy     canvas Y coordinate of the test point
     * @param a      the annotation whose handles to test
     * @param mapper the coordinate mapper for the current view state
     * @return handle name, or {@code null} if no handle was hit
     */
    private String hitTestHandle(double cx, double cy, Annotation a, CoordinateMapper mapper) {
        double ax = mapper.pdfXToCanvas(a.getX());
        double ay = mapper.pdfYToCanvasTop(a.getY(), a.getHeight());
        double aw = mapper.pdfDimToCanvas(a.getWidth());
        double ah = mapper.pdfDimToCanvas(a.getHeight());
        if (near(cx, ax,      cy, ay)      ) return "NW";
        if (near(cx, ax + aw, cy, ay)      ) return "NE";
        if (near(cx, ax,      cy, ay + ah) ) return "SW";
        if (near(cx, ax + aw, cy, ay + ah) ) return "SE";
        return null;
    }

    /**
     * Returns {@code true} if canvas point (cx, cy) is within HANDLE_SIZE pixels
     * of the handle anchor (hx, hy).
     *
     * @param cx canvas X of the test point
     * @param hx canvas X of the handle centre
     * @param cy canvas Y of the test point
     * @param hy canvas Y of the handle centre
     * @return {@code true} if within tolerance
     */
    private boolean near(double cx, double hx, double cy, double hy) {
        return Math.abs(cx - hx) < HANDLE_SIZE && Math.abs(cy - hy) < HANDLE_SIZE;
    }

    /**
     * Opens a file chooser to let the user select an image file and loads its data
     * into the given {@link ImageAnnotation}. Displays an error alert on failure.
     *
     * @param ia the image annotation to populate with the chosen file
     */
    private void promptImageFile(ImageAnnotation ia) {
        var chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choose Image");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        var file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) return;
        try {
            ia.setImageData(java.nio.file.Files.readAllBytes(file.toPath()));
            ia.setFxImage(new javafx.scene.image.Image(file.toURI().toString()));
            redraw();
        } catch (java.io.IOException e) {
            var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setContentText("Could not load image: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
