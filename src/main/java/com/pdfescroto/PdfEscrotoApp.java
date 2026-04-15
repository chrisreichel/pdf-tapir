package com.pdfescroto;

import com.pdfescroto.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Application entry point. Bootstraps the JavaFX runtime and constructs the
 * {@link MainWindow}, binding it to the primary stage with the dark CSS theme.
 */
public class PdfEscrotoApp extends Application {

    /**
     * Standard Java entry point — delegates to {@link Application#launch(String...)}.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * JavaFX lifecycle method called on the FX application thread after the
     * runtime is initialised.
     *
     * @param primaryStage the primary window provided by the JavaFX runtime
     */
    @Override
    public void start(Stage primaryStage) {
        var window = new MainWindow(primaryStage);
        var scene  = new Scene(window.getRoot(), 1100, 750);
        scene.getStylesheets().add(
                getClass().getResource("/com/pdfescroto/style.css").toExternalForm());
        primaryStage.setTitle("PDF Escroto");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/ICON.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
