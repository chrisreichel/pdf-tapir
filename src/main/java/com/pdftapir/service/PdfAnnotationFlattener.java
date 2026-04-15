package com.pdftapir.service;

import com.pdftapir.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;

/**
 * Draws pdf-tapir model annotations directly into page content streams.
 * The resulting PDF has fixed visible content rather than movable PDF annotations.
 */
class PdfAnnotationFlattener {

    private static final PDType1Font HELVETICA =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    void flatten(PDDocument targetDocument, PdfDocument sourceDocument) throws IOException {
        for (var page : sourceDocument.getPages()) {
            var pdPage = targetDocument.getPage(page.getPageIndex());
            try (var content = new PDPageContentStream(targetDocument, pdPage,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                for (var annotation : page.getAnnotations()) {
                    if (annotation instanceof TextAnnotation ta) {
                        drawText(content, ta);
                    } else if (annotation instanceof CheckboxAnnotation ca) {
                        drawCheckbox(content, ca);
                    } else if (annotation instanceof ImageAnnotation ia) {
                        drawImage(targetDocument, content, ia);
                    }
                }
            }
        }
    }

    private void drawText(PDPageContentStream content, TextAnnotation ta) throws IOException {
        content.saveGraphicsState();
        float[] rgb = parseRgb(ta.getFontColor());
        content.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        content.beginText();
        content.setFont(HELVETICA, ta.getFontSize());
        content.setLeading(ta.getFontSize() * 1.2f);
        content.newLineAtOffset((float) ta.getX() + 3f,
                (float) (ta.getY() + ta.getHeight() - ta.getFontSize()));
        for (String line : safeText(ta.getText()).split("\\R", -1)) {
            content.showText(line);
            content.newLine();
        }
        content.endText();
        content.restoreGraphicsState();
    }

    private void drawCheckbox(PDPageContentStream content, CheckboxAnnotation ca) throws IOException {
        float x = (float) ca.getX();
        float y = (float) ca.getY();
        float w = (float) ca.getWidth();
        float h = (float) ca.getHeight();

        content.saveGraphicsState();
        content.setStrokingColor(0f, 0f, 0f);
        content.setLineWidth(1f);
        content.addRect(x, y, w, h);
        content.stroke();

        if (ca.isChecked()) {
            float[] rgb = parseRgb(ca.getCheckmarkColor());
            content.setStrokingColor(rgb[0], rgb[1], rgb[2]);
            content.setLineWidth(Math.max(1.5f, Math.min(w, h) * 0.12f));
            content.moveTo(x + w * 0.18f, y + h * 0.5f);
            content.lineTo(x + w * 0.42f, y + h * 0.22f);
            content.lineTo(x + w * 0.82f, y + h * 0.78f);
            content.stroke();
        }
        content.restoreGraphicsState();
    }

    private void drawImage(PDDocument document, PDPageContentStream content, ImageAnnotation ia) throws IOException {
        byte[] imageData = ia.getImageData();
        if (imageData == null || imageData.length == 0) return;
        var pdImage = PDImageXObject.createFromByteArray(document, imageData, "pdf-tapir-image");
        content.drawImage(pdImage, (float) ia.getX(), (float) ia.getY(),
                (float) ia.getWidth(), (float) ia.getHeight());
    }

    private float[] parseRgb(String hex) {
        if (hex == null || !hex.matches("^#?[0-9a-fA-F]{6}$")) {
            return new float[]{0f, 0f, 0f};
        }
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        return new float[]{
                Integer.parseInt(normalized.substring(0, 2), 16) / 255f,
                Integer.parseInt(normalized.substring(2, 4), 16) / 255f,
                Integer.parseInt(normalized.substring(4, 6), 16) / 255f
        };
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }
}
