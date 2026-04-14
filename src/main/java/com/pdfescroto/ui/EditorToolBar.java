package com.pdfescroto.ui;

import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.PdfDocument;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Toolbar that provides tool-selection toggle buttons, page navigation controls,
 * and a Save button for the PDF editor.
 *
 * <p>Tool toggle buttons are grouped so that only one tool is active at a time;
 * {@link Tool#SELECT} is selected by default. Page navigation delegates to a
 * {@link PdfCanvas} bound via {@link #bindCanvas(PdfCanvas)}. The Save button
 * simulates a Ctrl+S key event on the scene, which {@code MainWindow}'s keyboard
 * shortcut handler intercepts to persist the document.</p>
 */
public class EditorToolBar {

    private final HBox        node        = new HBox(4);
    private final PdfDocument doc;
    private       PdfCanvas   canvas;
    private final Label       pageLabel   = new Label("Page — / —");
    private final Label       zoomLabel   = new Label("100%");
    private int currentPage = 0;

    /**
     * Creates the toolbar for the given document and undo manager.
     *
     * @param doc the currently open PDF document
     * @param um  the shared undo/redo manager
     */
    public EditorToolBar(PdfDocument doc, UndoManager um) {
        this.doc = doc;
        node.getStyleClass().add("tool-bar");
        node.setPadding(new Insets(4, 8, 4, 8));
        node.setSpacing(6);

        // Tool toggle group
        var tg        = new ToggleGroup();
        var selectBtn = toolButton("↖ Select",   Tool.SELECT,   tg);
        var textBtn   = toolButton("T Text",     Tool.TEXT,     tg);
        var cbBtn     = toolButton("☑ Checkbox", Tool.CHECKBOX, tg);
        var imgBtn    = toolButton("🖼 Image",    Tool.IMAGE,    tg);
        selectBtn.setSelected(true);

        // Vertical separator
        var sep = new Separator(Orientation.VERTICAL);

        // Page navigation
        var prevBtn = new Button("◀");
        var nextBtn = new Button("▶");
        prevBtn.setOnAction(e -> navigatePage(-1));
        nextBtn.setOnAction(e -> navigatePage(+1));
        updatePageLabel();

        // Flexible spacer
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Save button — fires a simulated Ctrl+S key event on the scene so that
        // MainWindow's keyboard shortcut handler can intercept it.
        var saveBtn = new Button("💾 Save");
        saveBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            if (node.getScene() != null) {
                javafx.event.Event.fireEvent(node.getScene(),
                    new javafx.scene.input.KeyEvent(
                        javafx.scene.input.KeyEvent.KEY_PRESSED, "s", "s",
                        javafx.scene.input.KeyCode.S, false, true, false, false));
            }
        });

        node.getChildren().addAll(selectBtn, textBtn, cbBtn, imgBtn,
                sep, prevBtn, pageLabel, nextBtn, spacer, zoomLabel, saveBtn);
    }

    /**
     * Creates a {@link ToggleButton} that activates the given tool on the bound canvas.
     *
     * @param label the button label
     * @param tool  the tool to activate when the button is selected
     * @param tg    the toggle group to add the button to
     * @return the configured toggle button
     */
    private ToggleButton toolButton(String label, Tool tool, ToggleGroup tg) {
        var btn = new ToggleButton(label);
        btn.setToggleGroup(tg);
        btn.setOnAction(e -> { if (canvas != null) canvas.setActiveTool(tool); });
        return btn;
    }

    /**
     * Navigates the canvas by the given page delta, clamping to the valid page range.
     *
     * @param delta number of pages to move (negative for backward, positive for forward)
     */
    private void navigatePage(int delta) {
        if (canvas == null || doc == null) return;
        int next = currentPage + delta;
        if (next < 0 || next >= doc.getPages().size()) return;
        currentPage = next;
        canvas.goToPage(currentPage);
        updatePageLabel();
    }

    /**
     * Refreshes the page label to reflect the current page index and total page count.
     */
    private void updatePageLabel() {
        int total = doc != null ? doc.getPages().size() : 0;
        pageLabel.setText("Page " + (currentPage + 1) + " / " + total);
    }

    /**
     * Binds this toolbar to the given canvas so that tool-selection and
     * page-navigation buttons can delegate to it.
     *
     * @param canvas the canvas to bind
     */
    public void bindCanvas(PdfCanvas canvas) {
        this.canvas = canvas;
        zoomLabel.setText(Math.round(canvas.getScale() * 100) + "%");
        canvas.scaleProperty().addListener((obs, oldVal, newVal) ->
                zoomLabel.setText(Math.round(newVal.doubleValue() * 100) + "%"));
    }

    /**
     * Returns the JavaFX node that represents this toolbar in the scene graph.
     *
     * @return the toolbar node
     */
    public Node getNode() { return node; }
}
