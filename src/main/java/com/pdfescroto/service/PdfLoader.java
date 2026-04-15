package com.pdfescroto.service;

import com.pdfescroto.model.*;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.Loader;
import java.util.Base64;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Loads a PDF file into a {@link PdfDocument}, reconstructing any
 * pdf-escroto annotations that were previously written by {@link PdfSaver}.
 * <p>
 * The loader identifies annotations by the tags embedded during save:
 * <ul>
 *   <li>Text annotations: {@link PDAnnotationFreeText} with subject {@code "pdf-escroto-text"}</li>
 *   <li>Checkbox annotations: {@link PDAnnotationWidget} with annotation name {@code "pdf-escroto-checkbox"}</li>
 *   <li>Image annotations: {@link PDAnnotationRubberStamp} with subject {@code "pdf-escroto-image"}</li>
 * </ul>
 */
public class PdfLoader {

    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(PdfLoader.class.getName());

    private final PdfRenderer renderer = new PdfRenderer();

    /**
     * Loads the given PDF file, rendering each page to a {@link javafx.scene.image.WritableImage}
     * and reconstructing any pdf-escroto annotations.
     * <p>
     * If an exception occurs after the {@link PDDocument} is opened, the document is closed
     * before the exception is rethrown to avoid resource leaks.
     *
     * @param file the PDF file to load
     * @return a {@link PdfDocument} containing the loaded pages and annotations
     * @throws IOException if the file cannot be read or parsed
     */
    public PdfDocument load(File file) throws IOException {
        var pdDoc = Loader.loadPDF(file);
        try {
            var pages = new ArrayList<PdfPage>();
            for (int i = 0; i < pdDoc.getNumberOfPages(); i++) {
                var pdPage   = pdDoc.getPage(i);
                var mediaBox = pdPage.getMediaBox();
                var pdfPage  = new PdfPage(i, mediaBox.getWidth(), mediaBox.getHeight());
                try {
                    pdfPage.setRenderedImage(renderer.renderPage(pdDoc, i));
                } catch (Exception e) {
                    // May fail in headless environments (e.g., tests without JavaFX initialized)
                    LOG.warning("Could not render page " + i + ": " + e.getMessage());
                }

                for (var annot : pdPage.getAnnotations()) {
                    parseAnnotation(pdDoc, annot).ifPresent(pdfPage::addAnnotation);
                }
                pages.add(pdfPage);
            }
            return new PdfDocument(pdDoc, pages, file);
        } catch (Exception e) {
            pdDoc.close();
            throw (e instanceof IOException ioe) ? ioe : new IOException("Failed to load PDF", e);
        }
    }

    private Optional<Annotation> parseAnnotation(PDDocument pdDoc, PDAnnotation pdAnnotation) {
        var rect = pdAnnotation.getRectangle();

        // Text annotation: PDAnnotationFreeText with subject TAG_TEXT
        if (pdAnnotation instanceof PDAnnotationFreeText freeText) {
            if (PdfSaver.TAG_TEXT.equals(freeText.getSubject())) {
                var ta = new TextAnnotation(
                        rect.getLowerLeftX(), rect.getLowerLeftY(),
                        rect.getWidth(), rect.getHeight());
                ta.setText(freeText.getContents() != null ? freeText.getContents() : "");
                // Recover font metadata from /T field: "fs=<size>;fc=<color>;ff=<family>"
                String titleBar = freeText.getTitlePopup();
                if (titleBar != null) {
                    for (String part : titleBar.split(";")) {
                        if (part.startsWith("fs=")) {
                            try { ta.setFontSize(Float.parseFloat(part.substring(3))); }
                            catch (NumberFormatException ignored) {}
                        } else if (part.startsWith("fc=")) {
                            ta.setFontColor(part.substring(3));
                        } else if (part.startsWith("ff=")) {
                            ta.setFontFamily(part.substring(3));
                        }
                    }
                }
                return Optional.of(ta);
            }
        }

        // Checkbox annotation: PDAnnotationWidget with annotationName TAG_CHECKBOX
        if (pdAnnotation instanceof PDAnnotationWidget widget) {
            if (PdfSaver.TAG_CHECKBOX.equals(widget.getAnnotationName())) {
                var ca = new CheckboxAnnotation(
                        rect.getLowerLeftX(), rect.getLowerLeftY(),
                        rect.getWidth(), rect.getHeight());
                reconstructCheckboxField(pdDoc, widget, ca);
                return Optional.of(ca);
            }
        }

        // Image annotation: PDAnnotationRubberStamp with subject TAG_IMAGE
        if (pdAnnotation instanceof PDAnnotationRubberStamp stamp) {
            if (PdfSaver.TAG_IMAGE.equals(stamp.getSubject())) {
                var ia = new ImageAnnotation(
                        rect.getLowerLeftX(), rect.getLowerLeftY(),
                        rect.getWidth(), rect.getHeight());
                restoreImageData(stamp, ia);
                return Optional.of(ia);
            }
        }

        return Optional.empty();
    }

    /**
     * Looks up the AcroForm field that owns the given widget and populates
     * the checkbox annotation's label and checked state.
     * <p>
     * When a PDF has exactly one widget per field the field and widget share
     * the same COS dictionary (the "merged form" case described in the PDF spec),
     * so the "Parent" entry on the widget is absent. Instead this method
     * reconstructs the {@link PDCheckBox} directly from the widget's own
     * COS dictionary via {@link PDFieldFactory}, which handles both merged
     * and non-merged layouts.
     */
    private void reconstructCheckboxField(PDDocument pdDoc,
                                          PDAnnotationWidget widget,
                                          CheckboxAnnotation ca) {
        // Primary path: parse the structured metadata string from widget /Contents.
        // Format (new): "cc=<color>;chk=<true|false>;lbl=<label>"
        String contents = widget.getContents();
        boolean hasChkField = false;
        if (contents != null) {
            for (String part : contents.split(";")) {
                if (part.startsWith("cc=")) {
                    ca.setCheckmarkColor(part.substring(3));
                } else if (part.startsWith("chk=")) {
                    ca.setChecked(Boolean.parseBoolean(part.substring(4)));
                    hasChkField = true;
                } else if (part.startsWith("lbl=")) {
                    ca.setLabel(decodeLbl(part.substring(4)));
                }
            }
        }

        if (hasChkField) return; // all metadata recovered — skip AcroForm reconstruction

        // Fallback: AcroForm reconstruction for PDFs saved before the new encoding.
        // Also handles the old single-field "cc=..." format (no chk/lbl keys).
        var acroForm = pdDoc.getDocumentCatalog().getAcroForm();
        if (acroForm == null) return;

        var widgetDict = widget.getCOSObject();
        var parentBase = widgetDict.getDictionaryObject(COSName.PARENT);
        COSDictionary fieldDict = (parentBase instanceof COSDictionary pd) ? pd : widgetDict;

        var field = PDFieldFactory.createField(acroForm, fieldDict, null);
        if (field instanceof PDCheckBox checkbox) {
            String alt = checkbox.getAlternateFieldName();
            ca.setLabel(alt != null ? alt : "");
            try {
                ca.setChecked(checkbox.isChecked());
            } catch (Exception e) {
                // isChecked() should not throw, but guard defensively
            }
        }
    }

    /**
     * Attempts to extract the image bytes from the rubber stamp's normal appearance
     * stream and populate the {@link ImageAnnotation} for in-app rendering.
     * If extraction fails, the annotation is left as a placeholder (no image data).
     */
    private void restoreImageData(PDAnnotationRubberStamp stamp, ImageAnnotation ia) {
        // Primary path: recover from the custom Base64 key written by PdfSaver.
        // This is reliable regardless of the original image format (CMYK JPEG, GIF, etc.)
        // and does not depend on PDFBox's image-decoding pipeline.
        String b64 = stamp.getCOSObject().getString(COSName.getPDFName("PEScrotoImgData"));
        if (b64 != null && !b64.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(b64);
                ia.setImageData(bytes);
                try {
                    ia.setFxImage(new javafx.scene.image.Image(new ByteArrayInputStream(bytes)));
                } catch (Exception fxEx) {
                    // Canvas will lazy-init on first draw
                }
                return; // successfully restored from custom key
            } catch (Exception ex) {
                LOG.warning("Could not decode custom image key; falling back to appearance stream: " + ex.getMessage());
            }
        }

        // Fallback: recover from the appearance stream (for PDFs saved before the custom key was added).
        try {
            var appearance = stamp.getAppearance();
            if (appearance == null) return;
            var normalEntry = appearance.getNormalAppearance();
            if (normalEntry == null) return;
            var form = normalEntry.getAppearanceStream();
            if (form == null || form.getResources() == null) return;

            for (var name : form.getResources().getXObjectNames()) {
                var xobj = form.getResources().getXObject(name);
                if (xobj instanceof PDImageXObject pdImg) {
                    var bos = new ByteArrayOutputStream();
                    boolean written = ImageIO.write(pdImg.getImage(), "PNG", bos);
                    if (!written) {
                        LOG.warning("ImageIO could not write image from appearance stream.");
                        break;
                    }
                    byte[] bytes = bos.toByteArray();
                    if (bytes.length == 0) break;
                    ia.setImageData(bytes);
                    try {
                        ia.setFxImage(new javafx.scene.image.Image(new ByteArrayInputStream(bytes)));
                    } catch (Exception fxEx) {
                        // Canvas will lazy-init on first draw
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            LOG.warning("Could not restore image data from appearance stream: " + ex.getMessage());
        }
    }

    private static String decodeLbl(String encoded) {
        return encoded == null ? "" : encoded.replace("%3B", ";");
    }
}
