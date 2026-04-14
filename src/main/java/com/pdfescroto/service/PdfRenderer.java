package com.pdfescroto.service;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.io.IOException;

public class PdfRenderer {
    private static final float SCREEN_DPI = 96f;

    public WritableImage renderPage(PDDocument document, int pageIndex) throws IOException {
        return renderPage(document, pageIndex, SCREEN_DPI);
    }

    public WritableImage renderPage(PDDocument document, int pageIndex, float dpi) throws IOException {
        var pdfRenderer   = new PDFRenderer(document);
        var bufferedImage = pdfRenderer.renderImageWithDPI(pageIndex, dpi);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }
}
