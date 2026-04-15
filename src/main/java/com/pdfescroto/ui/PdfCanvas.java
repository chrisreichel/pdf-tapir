package com.pdfescroto.ui;

import com.pdfescroto.command.DeleteAnnotationCommand;
import com.pdfescroto.command.EditAnnotationCommand;
import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.*;
import com.pdfescroto.service.CoordinateMapper;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Canvas that renders the current PDF page image and overlays interactive
 * annotation handles for the select, text, checkbox, and image tools.
 */
public class PdfCanvas extends Canvas {

    private final PdfDocument     document;
    private final UndoManager     undoManager;
    private final PropertiesPanel propertiesPanel;

    private PdfPage    currentPage;
    private int        currentPageIndex    = 0;
    private final DoubleProperty scale = new SimpleDoubleProperty(1.0);
    private final ObjectProperty<Tool> activeTool = new SimpleObjectProperty<>(Tool.SELECT);
    private Annotation selectedAnnotation;

    // Drag/create state
    private boolean    isDragging;
    private double     dragStartX, dragStartY;
    private double     annotStartX, annotStartY;
    private boolean    isCreating;
    private double     createStartX, createStartY;
    private Annotation creatingAnnotation;
    private String     resizeHandle;
    private double     oldAnnotW, oldAnnotH;

    // Inline text editor support
    private Pane             overlayPane;
    private TextAnnotation   activeInlineAnnotation;
    private Runnable         commitInlineEditor;

    /** Side length of each corner selection handle square, in canvas pixels. */
    protected static final double HANDLE_SIZE = 8.0;

    public PdfCanvas(PdfDocument document, UndoManager undoManager, PropertiesPanel propertiesPanel) {
        super(800, 1000);
        this.document        = document;
        this.undoManager     = undoManager;
        this.propertiesPanel = propertiesPanel;
        goToPage(0);
        setupMouseHandlers();
    }

    // ---- Page navigation ----

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
        setWidth(currentPage.getPageWidthPt()  * scale.get());
        setHeight(currentPage.getPageHeightPt() * scale.get());
    }

    // ---- Rendering ----

    public void redraw() {
        if (currentPage == null) return;
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        var img = currentPage.getRenderedImage();
        if (img != null) gc.drawImage(img, 0, 0, getWidth(), getHeight());

        var mapper = mapper();
        for (var annotation : currentPage.getAnnotations()) {
            drawAnnotation(gc, annotation, mapper, annotation == selectedAnnotation);
        }

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
            // Skip text rendering when inline editor is active for this annotation
            if (ta != activeInlineAnnotation) {
                gc.setFill(Color.web(ta.getFontColor()));
                gc.setFont(Font.font(ta.getFontFamily(), ta.getFontSize() * scale.get()));
                gc.fillText(ta.getText(), cx + 3, cy + ta.getFontSize() * scale.get(), cw - 6);
            }

        } else if (a instanceof CheckboxAnnotation ca) {
            gc.setStroke(selected ? Color.DODGERBLUE : Color.rgb(46, 125, 50, 0.9));
            gc.setLineWidth(selected ? 2.0 : 1.5);
            gc.strokeRect(cx, cy, cw, ch);
            if (ca.isChecked()) {
                gc.setStroke(Color.web(ca.getCheckmarkColor()));
                gc.setLineWidth(2.0);
                gc.strokeLine(cx + 3, cy + ch * 0.55, cx + cw * 0.4, cy + ch - 3);
                gc.strokeLine(cx + cw * 0.4, cy + ch - 3, cx + cw - 3, cy + 3);
            }

        } else if (a instanceof ImageAnnotation ia) {
            // Lazy-init fxImage from imageData if not yet set (e.g. after PDF reload where
            // the background-thread Image constructor failed or was skipped).
            if (ia.getFxImage() == null && ia.getImageData() != null && ia.getImageData().length > 0) {
                try {
                    ia.setFxImage(new javafx.scene.image.Image(
                            new java.io.ByteArrayInputStream(ia.getImageData())));
                } catch (Exception ignored) {}
            }
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

    // ---- Mouse handler setup ----

    private void setupMouseHandlers() {
        setOnMousePressed(e -> {
            onMousePressed(e.getX(), e.getY(), e.isPrimaryButtonDown());
            // Consume if annotation drag started — prevents ScrollPane from also panning
            if (isDragging || resizeHandle != null) e.consume();
        });
        setOnMouseDragged(e -> {
            onMouseDragged(e.getX(), e.getY());
            if (isDragging || resizeHandle != null) e.consume();
        });
        setOnMouseReleased(e -> {
            boolean wasActive = isDragging || resizeHandle != null;
            onMouseReleased(e.getX(), e.getY());
            if (wasActive) e.consume();
        });
        // Double-click opens inline text editor for existing text annotations
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                onDoubleClick(e.getX(), e.getY());
            }
        });
        // Shift + scroll wheel zooms; plain scroll falls through to ScrollPane
        setOnScroll(e -> {
            if (e.isShiftDown()) {
                if (e.getDeltaY() > 0) zoomIn();
                else if (e.getDeltaY() < 0) zoomOut();
                e.consume();
            }
        });
    }

    protected void onMousePressed(double cx, double cy, boolean primary) {
        if (!primary || currentPage == null) return;
        var mapper = mapper();

        if (getActiveTool() == Tool.SELECT) {
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
            double pdfX = mapper.canvasXToPdf(cx);
            double pdfY = mapper.canvasYToPdf(cy);
            setCreateStart(pdfX, pdfY);
            setCreating(true);
            setCreatingAnnotation(createAnnotation(pdfX, pdfY, 0, 0));
            setSelectedAnnotation(null);
            redraw();
        }
    }

    protected void onMouseDragged(double cx, double cy) {
        if (currentPage == null) return;
        var mapper = mapper();

        if (getActiveTool() == Tool.SELECT && resizeHandle != null && getSelectedAnnotation() != null) {
            var ann    = getSelectedAnnotation();
            double pdfX = mapper.canvasXToPdf(cx);
            double pdfY = mapper.canvasYToPdf(cy);
            double x = ann.getX(), y = ann.getY(), w = ann.getWidth(), h = ann.getHeight();
            double fixedPdfX, fixedPdfY;
            switch (resizeHandle) {
                case "SE" -> { fixedPdfX = x;     fixedPdfY = y + h; }
                case "NW" -> { fixedPdfX = x + w; fixedPdfY = y;     }
                case "NE" -> { fixedPdfX = x;     fixedPdfY = y;     }
                case "SW" -> { fixedPdfX = x + w; fixedPdfY = y + h; }
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
            double dx =  mapper.canvasDimToPdf(cx - getDragStartX());
            double dy = -mapper.canvasDimToPdf(cy - getDragStartY());
            var ann = getSelectedAnnotation();
            ann.setX(getAnnotStartX() + dx);
            ann.setY(getAnnotStartY() + dy);
            getPropertiesPanel().showAnnotation(ann);
            redraw();

        } else if (isCreating() && getCreatingAnnotation() != null) {
            double pdfX = mapper.canvasXToPdf(cx);
            double pdfY = mapper.canvasYToPdf(cy);
            double x = Math.min(getCreateStartX(), pdfX);
            double y = Math.min(getCreateStartY(), pdfY);
            double w = Math.abs(pdfX - getCreateStartX());
            double h = Math.abs(pdfY - getCreateStartY());
            var ann = getCreatingAnnotation();
            ann.setX(x); ann.setY(y); ann.setWidth(w); ann.setHeight(h);
            redraw();
        }
    }

    protected void onMouseReleased(double cx, double cy) {
        if (currentPage == null) return;

        if (getActiveTool() == Tool.SELECT && resizeHandle != null && getSelectedAnnotation() != null) {
            var ann    = getSelectedAnnotation();
            double finalX = ann.getX(), finalY = ann.getY();
            double finalW = ann.getWidth(), finalH = ann.getHeight();
            double startX = getAnnotStartX(), startY = getAnnotStartY();
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
            var ann   = getSelectedAnnotation();
            double finalX = ann.getX();
            double finalY = ann.getY();
            double oldX   = getAnnotStartX();
            double oldY   = getAnnotStartY();
            if (Math.abs(finalX - oldX) > 0.5 || Math.abs(finalY - oldY) > 0.5) {
                ann.setX(oldX);
                ann.setY(oldY);
                getUndoManager().execute(new com.pdfescroto.command.MoveAnnotationCommand(
                        ann, oldX, oldY, finalX, finalY));
            }
            setIsDragging(false);
            redraw();

        } else if (isCreating() && getCreatingAnnotation() != null) {
            var ann = getCreatingAnnotation();
            if (ann.getWidth() > 2 && ann.getHeight() > 2) {
                getUndoManager().execute(
                        new com.pdfescroto.command.AddAnnotationCommand(getCurrentPage(), ann));
                setSelectedAnnotation(ann);

                if (getActiveTool() == Tool.IMAGE) {
                    promptImageFile((ImageAnnotation) ann);
                } else {
                    // TEXT or CHECKBOX: open inline editor (for text) or switch to select
                    if (ann instanceof TextAnnotation ta) {
                        setActiveTool(Tool.SELECT);
                        showInlineTextEditor(ta);
                    } else {
                        setActiveTool(Tool.SELECT);
                    }
                }
            }
            setCreating(false);
            setCreatingAnnotation(null);
            redraw();
        }
    }

    private void onDoubleClick(double cx, double cy) {
        if (getActiveTool() != Tool.SELECT || currentPage == null) return;
        var mapper = mapper();
        for (int i = currentPage.getAnnotations().size() - 1; i >= 0; i--) {
            var ann = currentPage.getAnnotations().get(i);
            if (ann instanceof TextAnnotation ta && hitTest(ann, cx, cy, mapper)) {
                showInlineTextEditor(ta);
                return;
            }
        }
    }

    // ---- Inline text editor ----

    public void setOverlayPane(Pane pane) { this.overlayPane = pane; }

    private void showInlineTextEditor(TextAnnotation ta) {
        if (overlayPane == null) return;
        // Commit any previously open editor first
        if (commitInlineEditor != null) commitInlineEditor.run();

        activeInlineAnnotation = ta;
        redraw();

        var mapper = mapper();
        double edX = mapper.pdfXToCanvas(ta.getX());
        double edY = mapper.pdfYToCanvasTop(ta.getY(), ta.getHeight());
        double edW = mapper.pdfDimToCanvas(ta.getWidth());
        double edH = mapper.pdfDimToCanvas(ta.getHeight());

        var textArea = new TextArea(ta.getText());
        textArea.setLayoutX(edX);
        textArea.setLayoutY(edY);
        textArea.setPrefWidth(edW);
        textArea.setPrefHeight(edH);
        textArea.setWrapText(true);

        final String oldText = ta.getText();
        final boolean[] committed = {false};

        Runnable commit = () -> {
            if (committed[0]) return;
            committed[0] = true;
            String newText = textArea.getText();
            overlayPane.getChildren().remove(textArea);
            activeInlineAnnotation = null;
            commitInlineEditor = null;
            if (!oldText.equals(newText)) {
                undoManager.execute(new EditAnnotationCommand(
                    () -> { ta.setText(newText); propertiesPanel.showAnnotation(ta); redraw(); },
                    () -> { ta.setText(oldText); propertiesPanel.showAnnotation(ta); redraw(); }
                ));
            } else {
                redraw();
            }
        };

        commitInlineEditor = commit;

        textArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                commit.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                commit.run();
                e.consume();
            }
        });

        textArea.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) commit.run();
        });

        overlayPane.getChildren().add(textArea);
        Platform.runLater(textArea::requestFocus);
    }

    // ---- Actions ----

    public void deleteSelected() {
        if (selectedAnnotation == null || currentPage == null) return;
        undoManager.execute(new DeleteAnnotationCommand(currentPage, selectedAnnotation));
        selectedAnnotation = null;
        propertiesPanel.showAnnotation(null);
        redraw();
    }

    public void setActiveTool(Tool tool) { activeTool.set(tool); }

    /** Returns the active tool as an observable property for toolbar synchronization. */
    public ObjectProperty<Tool> activeToolProperty() { return activeTool; }

    // ---- Zoom ----

    public DoubleProperty scaleProperty() { return scale; }

    public double getScale() { return scale.get(); }

    public void setScale(double s) {
        if (commitInlineEditor != null) commitInlineEditor.run();
        scale.set(Math.max(0.25, Math.min(4.0, s)));
        resizeCanvasToPage();
        redraw();
    }

    public void zoomIn()  { setScale(getScale() * 1.25); }

    public void zoomOut() { setScale(getScale() / 1.25); }

    public void fitPage(ScrollPane sp) {
        if (currentPage == null) return;
        double vw = sp.getViewportBounds().getWidth();
        double vh = sp.getViewportBounds().getHeight();
        if (vw <= 0 || vh <= 0) return;
        setScale(Math.min(vw / currentPage.getPageWidthPt(),
                          vh / currentPage.getPageHeightPt()));
    }

    // ---- Protected accessors ----

    protected CoordinateMapper mapper() {
        return new CoordinateMapper(currentPage.getPageHeightPt(), scale.get());
    }

    protected PdfPage       getCurrentPage()            { return currentPage; }
    protected Tool          getActiveTool()             { return activeTool.get(); }
    protected Annotation    getSelectedAnnotation()     { return selectedAnnotation; }

    protected void setSelectedAnnotation(Annotation a) {
        selectedAnnotation = a;
        propertiesPanel.showAnnotation(a);
    }

    protected UndoManager    getUndoManager()           { return undoManager; }
    protected PropertiesPanel getPropertiesPanel()      { return propertiesPanel; }
    protected boolean        isCreating()               { return isCreating; }
    protected void           setCreating(boolean b)     { isCreating = b; }
    protected double         getDragStartX()            { return dragStartX; }
    protected double         getDragStartY()            { return dragStartY; }
    protected double         getAnnotStartX()           { return annotStartX; }
    protected double         getAnnotStartY()           { return annotStartY; }
    protected double         getCreateStartX()          { return createStartX; }
    protected double         getCreateStartY()          { return createStartY; }
    protected Annotation     getCreatingAnnotation()    { return creatingAnnotation; }
    protected void           setDragStart(double x, double y)    { dragStartX = x; dragStartY = y; }
    protected void           setAnnotStart(double x, double y)   { annotStartX = x; annotStartY = y; }
    protected void           setCreateStart(double x, double y)  { createStartX = x; createStartY = y; }
    protected void           setCreatingAnnotation(Annotation a) { creatingAnnotation = a; }
    protected void           setIsDragging(boolean b)            { isDragging = b; }
    protected boolean        getIsDragging()                     { return isDragging; }

    // ---- Private helpers ----

    private boolean hitTest(Annotation a, double cx, double cy, CoordinateMapper mapper) {
        double ax = mapper.pdfXToCanvas(a.getX());
        double ay = mapper.pdfYToCanvasTop(a.getY(), a.getHeight());
        double aw = mapper.pdfDimToCanvas(a.getWidth());
        double ah = mapper.pdfDimToCanvas(a.getHeight());
        return cx >= ax && cx <= ax + aw && cy >= ay && cy <= ay + ah;
    }

    private Annotation createAnnotation(double pdfX, double pdfY, double w, double h) {
        return switch (getActiveTool()) {
            case TEXT     -> new TextAnnotation(pdfX, pdfY, w, h);
            case CHECKBOX -> new CheckboxAnnotation(pdfX, pdfY, w, h);
            case IMAGE    -> new ImageAnnotation(pdfX, pdfY, w, h);
            default       -> throw new IllegalStateException("Not a creation tool: " + getActiveTool());
        };
    }

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

    private boolean near(double cx, double hx, double cy, double hy) {
        return Math.abs(cx - hx) < HANDLE_SIZE && Math.abs(cy - hy) < HANDLE_SIZE;
    }

    private void promptImageFile(ImageAnnotation ia) {
        var chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choose Image");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        var file = chooser.showOpenDialog(getScene().getWindow());
        // Always switch to SELECT after the dialog closes (whether file chosen or cancelled)
        setActiveTool(Tool.SELECT);
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
