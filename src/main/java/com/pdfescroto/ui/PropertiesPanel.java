package com.pdfescroto.ui;

import com.pdfescroto.command.EditAnnotationCommand;
import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class PropertiesPanel {

    private final VBox        node = new VBox(6);
    private final UndoManager undoManager;
    private final Runnable    onRedraw;
    private Annotation        current;

    // Shared geometry fields
    private final TextField xField = new TextField();
    private final TextField yField = new TextField();
    private final TextField wField = new TextField();
    private final TextField hField = new TextField();

    // Text-only section
    private final TextField textField   = new TextField();
    private final TextField fontField   = new TextField();
    private final VBox      textSection = new VBox(4);

    // Checkbox-only section
    private final TextField labelField = new TextField();
    private final CheckBox  checkedBox = new CheckBox("Checked");
    private final VBox      cbSection  = new VBox(4);

    public PropertiesPanel(UndoManager undoManager, Runnable onRedraw) {
        this.undoManager = undoManager;
        this.onRedraw    = onRedraw;
        node.getStyleClass().add("properties-panel");
        node.setPrefWidth(170);
        node.setPadding(new Insets(10));
        buildLayout();
        showAnnotation(null);
    }

    private void buildLayout() {
        textSection.getChildren().addAll(label("Text"), textField, label("Font size"), fontField);
        cbSection.getChildren().addAll(label("Label"), labelField, checkedBox);

        node.getChildren().addAll(
                bold("Properties"),
                label("X"), xField, label("Y"), yField,
                label("W"), wField, label("H"), hField,
                textSection, cbSection
        );

        // Commit geometry on Enter or focus-lost
        commitOnChange(xField, this::commitMove);
        commitOnChange(yField, this::commitMove);
        commitOnChange(wField, this::commitResize);
        commitOnChange(hField, this::commitResize);

        // Text content
        commitOnChange(textField, () -> {
            if (!(current instanceof TextAnnotation ta)) return;
            var ann = ta; // ta is already a captured local from the pattern binding
            String old = ann.getText();
            String nw  = textField.getText();
            if (old.equals(nw)) return;
            undoManager.execute(new EditAnnotationCommand(
                    () -> { ann.setText(nw);  onRedraw.run(); },
                    () -> { ann.setText(old); onRedraw.run(); }));
        });

        // Font size
        commitOnChange(fontField, () -> {
            if (!(current instanceof TextAnnotation ta)) return;
            float old = ta.getFontSize();
            try {
                float nw = Float.parseFloat(fontField.getText());
                if (old == nw) return;
                undoManager.execute(new EditAnnotationCommand(
                        () -> { ta.setFontSize(nw);  onRedraw.run(); },
                        () -> { ta.setFontSize(old); onRedraw.run(); }));
            } catch (NumberFormatException ignored) {}
        });

        // Checkbox label
        commitOnChange(labelField, () -> {
            if (!(current instanceof CheckboxAnnotation ca)) return;
            String old = ca.getLabel();
            String nw  = labelField.getText();
            if (old.equals(nw)) return;
            undoManager.execute(new EditAnnotationCommand(
                    () -> { ca.setLabel(nw);  onRedraw.run(); },
                    () -> { ca.setLabel(old); onRedraw.run(); }));
        });

        // Checkbox checked state
        checkedBox.setOnAction(e -> {
            if (!(current instanceof CheckboxAnnotation ca)) return;
            boolean nw  = checkedBox.isSelected();
            boolean old = !nw;
            undoManager.execute(new EditAnnotationCommand(
                    () -> { ca.setChecked(nw);  onRedraw.run(); },
                    () -> { ca.setChecked(old); onRedraw.run(); }));
        });
    }

    public void showAnnotation(Annotation a) {
        current = a;
        boolean has = a != null;
        xField.setDisable(!has);
        yField.setDisable(!has);
        wField.setDisable(!has);
        hField.setDisable(!has);
        textSection.setVisible(a instanceof TextAnnotation);
        textSection.setManaged(a instanceof TextAnnotation);
        cbSection.setVisible(a instanceof CheckboxAnnotation);
        cbSection.setManaged(a instanceof CheckboxAnnotation);

        if (!has) {
            xField.clear(); yField.clear(); wField.clear(); hField.clear();
            return;
        }

        xField.setText(fmt(a.getX()));
        yField.setText(fmt(a.getY()));
        wField.setText(fmt(a.getWidth()));
        hField.setText(fmt(a.getHeight()));

        if (a instanceof TextAnnotation ta) {
            textField.setText(ta.getText());
            fontField.setText(String.valueOf((int) ta.getFontSize()));
        } else if (a instanceof CheckboxAnnotation ca) {
            labelField.setText(ca.getLabel());
            checkedBox.setSelected(ca.isChecked());
        }
    }

    private void commitMove() {
        if (current == null) return;
        var ann = current; // capture before any async operation
        try {
            double oldX = ann.getX(), oldY = ann.getY();
            double newX = Double.parseDouble(xField.getText());
            double newY = Double.parseDouble(yField.getText());
            if (oldX == newX && oldY == newY) return;
            undoManager.execute(new EditAnnotationCommand(
                    () -> { ann.setX(newX); ann.setY(newY); onRedraw.run(); },
                    () -> { ann.setX(oldX); ann.setY(oldY); onRedraw.run(); }));
        } catch (NumberFormatException ignored) {}
    }

    private void commitResize() {
        if (current == null) return;
        var ann = current; // capture before any async operation
        try {
            double oldW = ann.getWidth(), oldH = ann.getHeight();
            double newW = Double.parseDouble(wField.getText());
            double newH = Double.parseDouble(hField.getText());
            if (oldW == newW && oldH == newH) return;
            undoManager.execute(new EditAnnotationCommand(
                    () -> { ann.setWidth(newW); ann.setHeight(newH); onRedraw.run(); },
                    () -> { ann.setWidth(oldW); ann.setHeight(oldH); onRedraw.run(); }));
        } catch (NumberFormatException ignored) {}
    }

    private void commitOnChange(TextField tf, Runnable action) {
        tf.setOnAction(e -> action.run());
        tf.focusedProperty().addListener((obs, was, focused) -> { if (!focused) action.run(); });
    }

    private Label label(String text) {
        var l = new Label(text);
        l.getStyleClass().add("label");
        return l;
    }

    private Label bold(String text) {
        var l = new Label(text);
        l.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        return l;
    }

    private String fmt(double v) { return String.format("%.1f", v); }

    public Node getNode() { return node; }
}
