package com.pdftapir.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.net.URI;
import java.util.Properties;

/**
 * Modal About dialog showing project identity, authorship, license, and
 * warranty disclaimer.
 */
public class AboutDialog {

    public void show(Window owner) {
        var stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("About PDF Tapir");
        stage.setResizable(false);

        // ── Icon ──────────────────────────────────────────────────────────────
        var iconView = new ImageView();
        iconView.setFitWidth(64);
        iconView.setFitHeight(64);
        iconView.setPreserveRatio(true);
        var iconUrl = getClass().getResource("/com/pdftapir/ICON.png");
        if (iconUrl != null) {
            var image = new Image(iconUrl.toExternalForm());
            iconView.setImage(image);
            stage.getIcons().add(image);
        }

        // ── App name ──────────────────────────────────────────────────────────
        var nameLabel = new Label("PDF Tapir");
        nameLabel.setFont(Font.font(null, FontWeight.BOLD, 22));

        // ── Version ───────────────────────────────────────────────────────────
        var versionLabel = new Label(readVersion());
        versionLabel.setStyle("-fx-text-fill: -fx-text-base-color; -fx-font-size: 12;");

        // ── URL hyperlink ─────────────────────────────────────────────────────
        var urlLink = new Hyperlink("https://github.com/chrisreichel/pdf-tapir");
        urlLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://github.com/chrisreichel/pdf-tapir"));
                }
            } catch (Exception ex) {
                // silently ignore — URL remains readable on screen
            }
        });

        // ── Developer & license ───────────────────────────────────────────────
        var developerLabel = new Label("Developer: Christian Reichel");
        var licenseLabel   = new Label("License: AGPL-3.0");

        // ── Warranty disclaimer ───────────────────────────────────────────────
        var disclaimer = new TextArea(
                "PDF Tapir is provided \u201cas is\u201d, without warranty of any kind, " +
                "express or implied, including but not limited to the warranties of " +
                "merchantability, fitness for a particular purpose, and non-infringement. " +
                "In no event shall the author or copyright holders be liable for any claim, " +
                "damages, or other liability, whether in an action of contract, tort, or " +
                "otherwise, arising from, out of, or in connection with the software or the " +
                "use or other dealings in the software. Use at your own risk."
        );
        disclaimer.setEditable(false);
        disclaimer.setWrapText(true);
        disclaimer.setFocusTraversable(false);
        disclaimer.setPrefRowCount(5);
        disclaimer.setPrefWidth(380);
        disclaimer.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // ── Close button ──────────────────────────────────────────────────────
        var closeButton = new Button("Close");
        closeButton.setDefaultButton(true);
        closeButton.setOnAction(e -> stage.close());
        closeButton.setPrefWidth(80);

        // ── Layout ────────────────────────────────────────────────────────────
        var content = new VBox(12,
                iconView,
                nameLabel,
                versionLabel,
                urlLink,
                developerLabel,
                licenseLabel,
                disclaimer,
                closeButton
        );
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 28, 20, 28));

        stage.setScene(new Scene(content));
        stage.showAndWait();
    }

    private String readVersion() {
        try (var in = getClass().getResourceAsStream("/com/pdftapir/build.properties")) {
            if (in == null) return "";
            var props = new Properties();
            props.load(in);
            return props.getProperty("version", "");
        } catch (Exception e) {
            return "";
        }
    }
}
