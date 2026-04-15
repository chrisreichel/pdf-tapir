package com.pdftapir.ui;

import com.pdftapir.command.UndoManager;
import com.pdftapir.model.PdfDocument;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.EnumMap;
import java.util.Map;

/**
 * Toolbar that provides tool-selection toggle buttons, page navigation controls,
 * zoom controls, and a Save button for the PDF editor.
 */
public class EditorToolBar {

    private final HBox        node        = new HBox(4);
    private final PdfDocument doc;
    private       PdfCanvas   canvas;
    private final Label       pageLabel   = new Label("Page — / —");
    private final Label       zoomLabel   = new Label("100%");
    private final Button      zoomOutBtn  = new Button("−");
    private final Button      zoomInBtn   = new Button("+");
    private int currentPage = 0;

    private final ToggleGroup            toggleGroup  = new ToggleGroup();
    private final Map<Tool, ToggleButton> toolButtons  = new EnumMap<>(Tool.class);

    public EditorToolBar(PdfDocument doc, UndoManager um) {
        this.doc = doc;
        node.getStyleClass().add("tool-bar");
        node.setPadding(new Insets(4, 8, 4, 8));
        node.setSpacing(6);

        var selectBtn = toolButton("↖ Select",   Tool.SELECT);
        var textBtn   = toolButton("T Text",     Tool.TEXT);
        var cbBtn     = toolButton("☑ Checkbox", Tool.CHECKBOX);
        var imgBtn    = toolButton("🖼 Image",    Tool.IMAGE);
        selectBtn.setSelected(true);

        var sep = new Separator(Orientation.VERTICAL);

        var prevBtn = new Button("◀");
        var nextBtn = new Button("▶");
        prevBtn.setOnAction(e -> navigatePage(-1));
        nextBtn.setOnAction(e -> navigatePage(+1));
        updatePageLabel();

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Zoom buttons — disabled until a canvas is bound
        zoomOutBtn.setDisable(true);
        zoomInBtn.setDisable(true);
        zoomOutBtn.setOnAction(e -> { if (canvas != null) canvas.zoomOut(); });
        zoomInBtn.setOnAction(e  -> { if (canvas != null) canvas.zoomIn(); });

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
                sep, prevBtn, pageLabel, nextBtn, spacer, zoomOutBtn, zoomLabel, zoomInBtn, saveBtn);
    }

    private ToggleButton toolButton(String label, Tool tool) {
        var btn = new ToggleButton(label);
        btn.setToggleGroup(toggleGroup);
        btn.setOnAction(e -> { if (canvas != null) canvas.setActiveTool(tool); });
        toolButtons.put(tool, btn);
        return btn;
    }

    private void navigatePage(int delta) {
        if (canvas == null || doc == null) return;
        int next = currentPage + delta;
        if (next < 0 || next >= doc.getPages().size()) return;
        currentPage = next;
        canvas.goToPage(currentPage);
        updatePageLabel();
    }

    private void updatePageLabel() {
        int total = doc != null ? doc.getPages().size() : 0;
        pageLabel.setText("Page " + (currentPage + 1) + " / " + total);
    }

    public void bindCanvas(PdfCanvas canvas) {
        this.canvas = canvas;
        zoomLabel.setText(Math.round(canvas.getScale() * 100) + "%");
        canvas.scaleProperty().addListener((obs, oldVal, newVal) ->
                zoomLabel.setText(Math.round(newVal.doubleValue() * 100) + "%"));
        canvas.activeToolProperty().addListener((obs, oldTool, newTool) -> {
            ToggleButton btn = toolButtons.get(newTool);
            if (btn != null) btn.setSelected(true);
        });
        // Enable zoom buttons now that a canvas is available
        zoomOutBtn.setDisable(false);
        zoomInBtn.setDisable(false);
    }

    public Node getNode() { return node; }
}
