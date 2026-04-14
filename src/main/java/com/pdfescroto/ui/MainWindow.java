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

/**
 * Root UI controller that owns the primary {@link Stage} and assembles the
 * full application layout: menu bar, editor toolbar, scrollable canvas, and
 * properties panel.
 * <p>
 * The window is intentionally lazy: the toolbar and canvas are not created
 * until a PDF is successfully opened via {@link #attachDocument()}.
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
     * Creates the main window and wires all permanent UI components.
     *
     * @param primaryStage the primary stage provided by the JavaFX runtime
     */
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
            attachDocument();
        });
        task.setOnFailed(e -> showError("Failed to open PDF", task.getException()));
        new Thread(task, "pdf-loader").start();
    }

    /**
     * Called after a PDF is successfully loaded. Constructs the {@link EditorToolBar}
     * and {@link PdfCanvas}, then assembles them into the {@link BorderPane}.
     */
    private void attachDocument() {
        var toolbar = new EditorToolBar(openDocument, undoManager);
        canvas      = new PdfCanvas(openDocument, undoManager, propertiesPanel);
        toolbar.bindCanvas(canvas);

        scrollPane = new ScrollPane(canvas);
        scrollPane.setPannable(true);

        root.setTop(new VBox(buildMenuBar(), toolbar.getNode()));
        root.setCenter(scrollPane);
    }

    private void saveFile(boolean saveAs) {
        if (openDocument == null) return;
        File target = openDocument.getSourceFile();
        if (saveAs || target == null) {
            var chooser = new FileChooser();
            chooser.setTitle("Save PDF As");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            target = chooser.showSaveDialog(primaryStage);
            if (target == null) return;
            openDocument.setSourceFile(target);
        }

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

    /**
     * Returns the root layout node to be placed in the JavaFX {@link javafx.scene.Scene}.
     *
     * @return the root {@link BorderPane}
     */
    public BorderPane getRoot() { return root; }
}
