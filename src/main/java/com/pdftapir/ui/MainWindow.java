package com.pdftapir.ui;

import com.pdftapir.command.UndoManager;
import com.pdftapir.model.PdfDocument;
import com.pdftapir.service.PdfEncryptionService;
import com.pdftapir.service.PdfLoader;
import com.pdftapir.service.PdfMergeService;
import com.pdftapir.service.PdfPageService;
import com.pdftapir.service.PdfSaver;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Root UI controller that owns the primary {@link Stage} and assembles the
 * full application layout: menu bar, editor toolbar, scrollable canvas, and
 * properties panel.
 */
public class MainWindow {

    private final Stage                 primaryStage;
    private final BorderPane            root               = new BorderPane();
    private final UndoManager           undoManager        = new UndoManager(50);
    private final PdfLoader             loader             = new PdfLoader();
    private final PdfSaver              saver              = new PdfSaver();
    private final PdfEncryptionService  encryptionService  = new PdfEncryptionService();
    private final PdfMergeService       mergeService       = new PdfMergeService();
    private final PdfPageService        pageService        = new PdfPageService();

    private MenuItem decryptItem;

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

        var encryptItem = new MenuItem("Encrypt\u2026");
        decryptItem     = new MenuItem("Decrypt");
        var mergeItem       = new MenuItem("Merge\u2026");
        var removePagesItem = new MenuItem("Remove Pages\u2026");

        encryptItem.setOnAction(e -> encryptDocument());
        decryptItem.setOnAction(e -> decryptDocument());
        mergeItem.setOnAction(e -> mergeDocument());
        removePagesItem.setOnAction(e -> removePages());

        decryptItem.setDisable(true); // enabled when an encrypted doc is open
        encryptItem.setDisable(openDocument == null);
        mergeItem.setDisable(openDocument == null);
        removePagesItem.setDisable(openDocument == null);

        var documentMenu = new Menu("Document", null,
                encryptItem, decryptItem,
                new SeparatorMenuItem(),
                mergeItem,
                new SeparatorMenuItem(),
                removePagesItem);

        documentMenu.setOnShowing(e -> {
            boolean hasDoc = openDocument != null;
            encryptItem.setDisable(!hasDoc);
            decryptItem.setDisable(!hasDoc || !openDocument.isEncrypted());
            mergeItem.setDisable(!hasDoc);
            removePagesItem.setDisable(!hasDoc);
        });

        var aboutItem = new MenuItem("About PDF Tapir\u2026");
        aboutItem.setOnAction(e -> new AboutDialog().show(primaryStage));
        var helpMenu = new Menu("Help", null, aboutItem);

        return new MenuBar(fileMenu, editMenu, viewMenu, documentMenu, helpMenu);
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
        loadFile(file, null);
    }

    private void loadFile(File file, String password) {
        var task = new Task<PdfDocument>() {
            @Override protected PdfDocument call() throws Exception {
                return loader.load(file, password);
            }
        };
        task.setOnSucceeded(e -> {
            if (openDocument != null) { try { openDocument.close(); } catch (Exception ex) { /* ignore */ } }
            openDocument = task.getValue();
            saveIntentDecided.clear();
            attachDocument();
        });
        task.setOnFailed(e -> {
            Throwable cause = task.getException();
            if (cause instanceof org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException) {
                Platform.runLater(() -> promptPasswordAndLoad(file));
            } else {
                showError("Failed to open PDF", cause);
            }
        });
        new Thread(task, "pdf-loader").start();
    }

    private void promptPasswordAndLoad(File file) {
        var dialog = new TextInputDialog();
        dialog.setTitle("Password Required");
        dialog.setHeaderText("\"" + file.getName() + "\" is password-protected.");
        dialog.setContentText("Password:");
        // Mask the input field
        var passwordField = new PasswordField();
        dialog.getEditor().setVisible(false);
        dialog.getEditor().setManaged(false);
        dialog.getDialogPane().setContent(buildPasswordContent(passwordField));

        Optional<String> result = dialog.showAndWait();
        String entered = passwordField.getText();
        if (result.isEmpty() || entered.isEmpty()) return; // user cancelled
        loadFile(file, entered);
    }

    private javafx.scene.Node buildPasswordContent(PasswordField passwordField) {
        var label = new Label("Password:");
        var box = new HBox(8, label, passwordField);
        box.setPadding(new Insets(10, 0, 0, 0));
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        return box;
    }

    private void attachDocument() {
        reloadCanvas(0);
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

    // -------------------------------------------------------------------------
    // Document operations
    // -------------------------------------------------------------------------

    private void encryptDocument() {
        if (openDocument == null) return;
        var dialog = new TextInputDialog();
        dialog.setTitle("Encrypt PDF");
        dialog.setHeaderText("Set a password to encrypt this PDF.");
        dialog.setContentText("Password:");
        var passwordField = new PasswordField();
        dialog.getEditor().setVisible(false);
        dialog.getEditor().setManaged(false);
        dialog.getDialogPane().setContent(buildPasswordContent(passwordField));

        Optional<String> result = dialog.showAndWait();
        String password = passwordField.getText();
        if (result.isEmpty() || password.isEmpty()) return;

        try {
            encryptionService.encrypt(openDocument.getPdDocument(), password);
            openDocument.setEncrypted(true);
            saveFile(false);
        } catch (Exception e) {
            showError("Encryption failed", e);
        }
    }

    private void decryptDocument() {
        if (openDocument == null || !openDocument.isEncrypted()) return;
        encryptionService.decrypt(openDocument.getPdDocument());
        openDocument.setEncrypted(false);
        saveFile(false);
    }

    private void mergeDocument() {
        if (openDocument == null) return;

        // Step 1: choose append or prepend
        var appendBtn  = new ButtonType("Append");
        var prependBtn = new ButtonType("Prepend");
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Merge PDFs");
        alert.setHeaderText("How should the pages be inserted?");
        alert.getButtonTypes().setAll(appendBtn, prependBtn, ButtonType.CANCEL);
        Optional<ButtonType> modeResult = alert.showAndWait();
        if (modeResult.isEmpty() || modeResult.get() == ButtonType.CANCEL) return;
        boolean append = modeResult.get() == appendBtn;

        // Step 2: select files
        var chooser = new FileChooser();
        chooser.setTitle("Select PDFs to merge");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        List<File> files = chooser.showOpenMultipleDialog(primaryStage);
        if (files == null || files.isEmpty()) return;

        // Step 3: confirm (cannot be undone)
        var confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This action cannot be undone. Continue?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Merge PDFs");
        confirm.setHeaderText("Merge " + files.size() + " file(s) into this document");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        var task = new Task<Void>() {
            @Override protected Void call() throws Exception {
                if (append) mergeService.append(openDocument, files);
                else        mergeService.prepend(openDocument, files);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            undoManager.clear();
            reloadCanvas(0);
        });
        task.setOnFailed(e -> showError("Merge failed", task.getException()));
        new Thread(task, "pdf-merge").start();
    }

    private void removePages() {
        if (openDocument == null) return;
        int total = openDocument.getPages().size();

        // Build a checklist dialog
        var checkBoxes = new ArrayList<CheckBox>();
        for (int i = 0; i < total; i++) {
            checkBoxes.add(new CheckBox("Page " + (i + 1)));
        }

        var listBox = new VBox(4);
        listBox.getChildren().addAll(checkBoxes);
        var scroll = new ScrollPane(listBox);
        scroll.setPrefHeight(Math.min(total * 28 + 16, 300));
        scroll.setFitToWidth(true);

        var dialog = new Dialog<Set<Integer>>();
        dialog.setTitle("Remove Pages");
        dialog.setHeaderText("Select pages to remove:");
        var confirmBtn = new ButtonType("Remove", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(scroll);

        // Disable confirm when all or none are selected
        var confirmNode = dialog.getDialogPane().lookupButton(confirmBtn);
        Runnable updateConfirm = () -> {
            long selected = checkBoxes.stream().filter(CheckBox::isSelected).count();
            confirmNode.setDisable(selected == 0 || selected == total);
        };
        checkBoxes.forEach(cb -> cb.selectedProperty().addListener((o, ov, nv) -> updateConfirm.run()));
        updateConfirm.run();

        dialog.setResultConverter(bt -> {
            if (bt != confirmBtn) return null;
            var indices = new LinkedHashSet<Integer>();
            for (int i = 0; i < checkBoxes.size(); i++) {
                if (checkBoxes.get(i).isSelected()) indices.add(i);
            }
            return indices;
        });

        Optional<Set<Integer>> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isEmpty()) return;
        Set<Integer> toRemove = result.get();

        // Confirm (cannot be undone)
        var confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + toRemove.size() + " page(s)? This cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Remove Pages");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            pageService.removePages(openDocument, toRemove);
            undoManager.clear();
            int targetPage = Math.min(0, openDocument.getPages().size() - 1);
            reloadCanvas(targetPage);
        } catch (Exception e) {
            showError("Page removal failed", e);
        }
    }

    /** Rebuilds the canvas UI for the current openDocument, showing the given page. */
    private void reloadCanvas(int pageIndex) {
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

        if (pageIndex >= 0 && pageIndex < openDocument.getPages().size()) {
            canvas.goToPage(pageIndex);
        }
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
