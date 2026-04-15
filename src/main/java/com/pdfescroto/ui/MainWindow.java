package com.pdfescroto.ui;

import com.pdfescroto.command.UndoManager;
import com.pdfescroto.model.PdfDocument;
import com.pdfescroto.service.PdfLoader;
import com.pdfescroto.service.PdfSaver;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Root UI controller that owns the primary {@link Stage} and assembles the
 * full application layout: menu bar, editor toolbar, scrollable canvas, and
 * properties panel.
 */
public class MainWindow {

    private final Stage       primaryStage;
    private final BorderPane  root        = new BorderPane();
    private final UndoManager undoManager = new UndoManager(50);
    private final PdfLoader   loader      = new PdfLoader();
    private final PdfSaver    saver       = new PdfSaver();

    private PdfDocument     openDocument;
    private PdfCanvas       canvas;
    private PropertiesPanel propertiesPanel;
    private ScrollPane      scrollPane;

    /**
     * Tracks files for which the user has already decided their save intent
     * (overwrite or save-as-new) in the current session. Once a target is in
     * this set, subsequent saves skip the dialog.
     */
    private final Set<File> saveIntentDecided = new HashSet<>();

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        buildUI();
        setupKeyboardShortcuts();
    }

    private void buildUI() {
        root.setTop(buildMenuBar());
        propertiesPanel = new PropertiesPanel(undoManager, () -> {
            if (canvas != null) canvas.redraw();
        });
        root.setRight(propertiesPanel.getNode());
    }

    private MenuBar buildMenuBar() {
        var openItem   = new MenuItem("Open\u2026");
        var saveItem   = new MenuItem("Save");
        var saveAsItem = new MenuItem("Save As\u2026");
        var exitItem   = new MenuItem("Exit");

        openItem.setOnAction(e -> openFile());
        saveItem.setOnAction(e -> saveFile(false));
        saveAsItem.setOnAction(e -> saveFile(true));
        exitItem.setOnAction(e -> Platform.exit());

        var fileMenu = new Menu("File", null, openItem, saveItem, saveAsItem,
                new SeparatorMenuItem(), exitItem);

        var undoItem = new MenuItem("Undo");
        var redoItem = new MenuItem("Redo");
        undoItem.setOnAction(e -> { undoManager.undo(); if (canvas != null) canvas.redraw(); });
        redoItem.setOnAction(e -> { undoManager.redo(); if (canvas != null) canvas.redraw(); });
        var editMenu = new Menu("Edit", null, undoItem, redoItem);

        var zoomInItem  = new MenuItem("Zoom In");
        var zoomOutItem = new MenuItem("Zoom Out");
        var fitPageItem = new MenuItem("Fit Page");
        zoomInItem.setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN));
        zoomOutItem.setAccelerator(new KeyCodeCombination(KeyCode.MINUS,  KeyCombination.SHORTCUT_DOWN));
        fitPageItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN));
        zoomInItem.setOnAction(e  -> { if (canvas != null) canvas.zoomIn(); });
        zoomOutItem.setOnAction(e -> { if (canvas != null) canvas.zoomOut(); });
        fitPageItem.setOnAction(e -> { if (canvas != null && scrollPane != null) canvas.fitPage(scrollPane); });
        var viewMenu = new Menu("View", null, zoomInItem, zoomOutItem, new SeparatorMenuItem(), fitPageItem);

        return new MenuBar(fileMenu, editMenu, viewMenu);
    }

    private void setupKeyboardShortcuts() {
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) return;
            scene.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case Z -> { if (e.isShortcutDown()) { undoManager.undo(); if (canvas != null) canvas.redraw(); } }
                    case Y -> { if (e.isShortcutDown()) { undoManager.redo(); if (canvas != null) canvas.redraw(); } }
                    case S -> { if (e.isShortcutDown()) saveFile(false); }
                    case DELETE, BACK_SPACE -> { if (canvas != null) canvas.deleteSelected(); }
                    default -> {}
                }
            });
        });
    }

    private void openFile() {
        var chooser = new FileChooser();
        chooser.setTitle("Open PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        var file = chooser.showOpenDialog(primaryStage);
        if (file == null) return;

        var task = new Task<PdfDocument>() {
            @Override protected PdfDocument call() throws Exception { return loader.load(file); }
        };
        task.setOnSucceeded(e -> {
            if (openDocument != null) { try { openDocument.close(); } catch (Exception ex) { /* ignore */ } }
            openDocument = task.getValue();
            saveIntentDecided.clear(); // new document — reset session save-intent state
            attachDocument();
        });
        task.setOnFailed(e -> showError("Failed to open PDF", task.getException()));
        new Thread(task, "pdf-loader").start();
    }

    private void attachDocument() {
        var toolbar = new EditorToolBar(openDocument, undoManager);
        canvas      = new PdfCanvas(openDocument, undoManager, propertiesPanel);
        toolbar.bindCanvas(canvas);

        var overlayPane = new javafx.scene.layout.Pane();
        overlayPane.setPickOnBounds(false);
        canvas.setOverlayPane(overlayPane);

        var contentPane = new javafx.scene.layout.StackPane(canvas, overlayPane);
        contentPane.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        scrollPane = new ScrollPane(contentPane);
        scrollPane.setPannable(false);

        root.setTop(new VBox(buildMenuBar(), toolbar.getNode()));
        root.setCenter(scrollPane);
    }

    private void saveFile(boolean saveAs) {
        if (openDocument == null) return;
        File target = openDocument.getSourceFile();

        if (saveAs || target == null) {
            // "Save As…" or new (unsaved) document: always show file chooser directly
            var chooser = new FileChooser();
            chooser.setTitle("Save PDF As");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            target = chooser.showSaveDialog(primaryStage);
            if (target == null) return;
            openDocument.setSourceFile(target);
            saveIntentDecided.add(target);

        } else if (!saveIntentDecided.contains(target)) {
            // First plain Save for this opened file: ask whether to overwrite or save as new
            var overwriteBtn = new ButtonType("Overwrite original");
            var saveAsNewBtn = new ButtonType("Save as new file");
            var alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Save");
            alert.setHeaderText("How would you like to save?");
            alert.setContentText("\"" + target.getName() + "\" was opened from disk.");
            alert.getButtonTypes().setAll(overwriteBtn, saveAsNewBtn, ButtonType.CANCEL);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == ButtonType.CANCEL) return;

            if (result.get() == saveAsNewBtn) {
                var chooser = new FileChooser();
                chooser.setTitle("Save PDF As");
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                File newTarget = chooser.showSaveDialog(primaryStage);
                if (newTarget == null) return;
                openDocument.setSourceFile(newTarget);
                saveIntentDecided.add(newTarget);
                target = newTarget;
            } else {
                // "Overwrite original"
                saveIntentDecided.add(target);
            }
        }
        // else: intent already decided this session — save silently

        final File finalTarget = target;
        var task = new Task<Void>() {
            @Override protected Void call() throws Exception {
                saver.save(openDocument, finalTarget);
                return null;
            }
        };
        task.setOnFailed(e -> showError("Save failed", task.getException()));
        new Thread(task, "pdf-saver").start();
    }

    private void showError(String header, Throwable t) {
        Platform.runLater(() -> {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(header);
            alert.setContentText(t != null ? t.getMessage() : "Unknown error");
            alert.showAndWait();
        });
    }

    public BorderPane getRoot() { return root; }
}
