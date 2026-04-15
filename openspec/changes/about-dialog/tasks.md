## 1. Create AboutDialog class

- [x] 1.1 Create `src/main/java/com/pdftapir/ui/AboutDialog.java` with a `show(Window owner)` method that constructs and opens the modal stage
- [x] 1.2 Add the application icon (`/com/pdftapir/ICON.png`) scaled to 64×64 px via `ImageView`
- [x] 1.3 Add "PDF Tapir" as the title label (large/bold font)
- [x] 1.4 Add the source URL as a JavaFX `Hyperlink` that calls `Desktop.getDesktop().browse(URI)` on action, with silent fallback if `Desktop` is unsupported
- [x] 1.5 Add developer name ("Christian Reichel") and license ("AGPL-3.0") as labels
- [x] 1.6 Add the warranty disclaimer as a read-only, word-wrapped text area or label
- [x] 1.7 Add a Close button that calls `stage.close()`
- [x] 1.8 Set `initModality(Modality.WINDOW_MODAL)` and `initOwner(owner)` on the stage
- [x] 1.9 Set the stage icon to the application icon (null-safe)

## 2. Wire into MainWindow

- [x] 2.1 Add `aboutItem = new MenuItem("About PDF Tapir\u2026")` in `MainWindow.buildMenuBar()`
- [x] 2.2 Set `aboutItem` action to call `new AboutDialog().show(stage)`
- [x] 2.3 Create `helpMenu = new Menu("Help", null, aboutItem)` and append it to the `MenuBar` after the Document menu

## 3. Verify

- [ ] 3.1 Run `mvn javafx:run`, open Help → About PDF Tapir… and confirm all fields display correctly
- [ ] 3.2 Confirm the URL hyperlink opens the browser
- [ ] 3.3 Confirm the Close button dismisses the dialog and focus returns to the main window
- [x] 3.4 Run `mvn test` and confirm all existing tests still pass
