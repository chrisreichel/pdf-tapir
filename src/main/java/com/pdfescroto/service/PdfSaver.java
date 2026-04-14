package com.pdfescroto.service;

import com.pdfescroto.model.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * Serialises {@link PdfDocument} annotations back into a PDF file using PDFBox.
 * <p>
 * All annotations written by this class carry an identifying tag so that
 * {@link PdfLoader} can recognise and reconstruct them on re-open:
 * <ul>
 *   <li>{@link TextAnnotation} → {@link PDAnnotationFreeText} with subject {@value #TAG_TEXT}</li>
 *   <li>{@link CheckboxAnnotation} → AcroForm {@link PDCheckBox} whose widget annotation name is {@value #TAG_CHECKBOX}</li>
 *   <li>{@link ImageAnnotation} → {@link PDAnnotationRubberStamp} with subject {@value #TAG_IMAGE}</li>
 * </ul>
 * Checkbox AcroForm field names are prefixed with {@value #CB_PREFIX}.
 */
public class PdfSaver {

    /** Subject tag applied to {@link PDAnnotationFreeText} annotations written by this tool. */
    static final String TAG_TEXT     = "pdf-escroto-text";
    /** Subject tag applied to image rubber-stamp annotations written by this tool. */
    static final String TAG_IMAGE    = "pdf-escroto-image";
    /**
     * Annotation name tag set on {@link PDAnnotationWidget} for checkboxes written by this tool.
     * PDAnnotationWidget does not extend PDAnnotationMarkup, so we use {@code annotationName}
     * (the PDF "NM" key) as the tag carrier instead of "Subj".
     */
    static final String TAG_CHECKBOX = "pdf-escroto-checkbox";
    /** Prefix applied to AcroForm checkbox field partial names. */
    static final String CB_PREFIX    = "pescroto_";

    /**
     * Saves {@code document} to {@code target}, replacing any previously written
     * pdf-escroto annotations with the current state.
     * <p>
     * Uses an atomic temp-file-then-rename pattern to avoid partially written files.
     *
     * @param document the document to save
     * @param target   the destination file (will be overwritten)
     * @throws IOException if writing or renaming the file fails
     */
    public void save(PdfDocument document, File target) throws IOException {
        var pdDoc = document.getPdDocument();

        for (var page : document.getPages()) {
            var pdPage = pdDoc.getPage(page.getPageIndex());

            // Remove previously written pdf-escroto annotations from this page
            var existing = new ArrayList<>(pdPage.getAnnotations());
            existing.removeIf(a -> {
                if (a instanceof PDAnnotationMarkup markup) {
                    String subj = markup.getSubject();
                    return TAG_TEXT.equals(subj) || TAG_IMAGE.equals(subj);
                }
                if (a instanceof PDAnnotationWidget widget) {
                    return TAG_CHECKBOX.equals(widget.getAnnotationName());
                }
                return false;
            });
            pdPage.setAnnotations(existing);

            // Remove our AcroForm checkbox fields
            removeOurCheckboxFields(pdDoc);

            // Write current annotations
            for (var annotation : page.getAnnotations()) {
                if (annotation instanceof TextAnnotation ta) {
                    writeText(pdPage, ta);
                } else if (annotation instanceof CheckboxAnnotation ca) {
                    writeCheckbox(pdDoc, pdPage, ca);
                } else if (annotation instanceof ImageAnnotation ia) {
                    writeImage(pdDoc, pdPage, ia);
                }
            }
        }

        // Atomic write: save to temp file then rename over target
        var tmp = File.createTempFile("pescroto-", ".pdf", target.getParentFile());
        try {
            pdDoc.save(tmp);
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            tmp.delete();
            throw e;
        }
    }

    private void writeText(PDPage pdPage, TextAnnotation ta) throws IOException {
        var rect  = toPdRect(ta);
        var annot = new PDAnnotationFreeText();
        annot.setSubject(TAG_TEXT);
        annot.setContents(ta.getText());
        annot.setRectangle(rect);
        annot.setDefaultAppearance("/Helvetica " + (int) ta.getFontSize() + " Tf 0 0 0 rg");
        pdPage.getAnnotations().add(annot);
    }

    private void writeCheckbox(PDDocument pdDoc, PDPage pdPage, CheckboxAnnotation ca) throws IOException {
        var acroForm = getOrCreateAcroForm(pdDoc);
        var checkbox = new PDCheckBox(acroForm);
        checkbox.setPartialName(CB_PREFIX + ca.getId().replace("-", ""));
        checkbox.setAlternateFieldName(ca.getLabel());

        var widget = checkbox.getWidgets().get(0);
        // PDAnnotationWidget does not extend PDAnnotationMarkup, so use annotationName as tag
        widget.setAnnotationName(TAG_CHECKBOX);
        widget.setPage(pdPage);
        widget.setRectangle(new PDRectangle(
                (float) ca.getX(), (float) ca.getY(),
                (float) ca.getWidth(), (float) ca.getHeight()));

        acroForm.getFields().add(checkbox);
        pdPage.getAnnotations().add(widget);

        if (ca.isChecked()) {
            checkbox.check();
        } else {
            checkbox.unCheck();
        }
    }

    private void writeImage(PDDocument pdDoc, PDPage pdPage, ImageAnnotation ia) throws IOException {
        if (ia.getImageData() == null) return;
        var rect  = toPdRect(ia);
        var stamp = new PDAnnotationRubberStamp();
        stamp.setSubject(TAG_IMAGE);
        stamp.setRectangle(rect);

        var pdImage     = PDImageXObject.createFromByteArray(pdDoc, ia.getImageData(), "img");
        var appearStream = new PDAppearanceStream(pdDoc);
        appearStream.setResources(new PDResources());
        appearStream.setBBox(rect);
        try (var cs = new PDPageContentStream(pdDoc, appearStream)) {
            cs.drawImage(pdImage, 0, 0, rect.getWidth(), rect.getHeight());
        }
        var appearDict = new PDAppearanceDictionary();
        appearDict.setNormalAppearance(appearStream);
        stamp.setAppearance(appearDict);

        pdPage.getAnnotations().add(stamp);
    }

    private PDRectangle toPdRect(Annotation a) {
        return new PDRectangle(
                (float) a.getX(), (float) a.getY(),
                (float) a.getWidth(), (float) a.getHeight());
    }

    private PDAcroForm getOrCreateAcroForm(PDDocument pdDoc) {
        var catalog  = pdDoc.getDocumentCatalog();
        var acroForm = catalog.getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(pdDoc);
            catalog.setAcroForm(acroForm);
        }
        return acroForm;
    }

    private void removeOurCheckboxFields(PDDocument pdDoc) {
        var acroForm = pdDoc.getDocumentCatalog().getAcroForm();
        if (acroForm == null) return;
        acroForm.getFields().removeIf(f ->
                f.getPartialName() != null && f.getPartialName().startsWith(CB_PREFIX));
    }
}
