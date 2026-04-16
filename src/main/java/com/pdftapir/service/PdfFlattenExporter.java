package com.pdftapir.service;

import com.pdftapir.model.PdfDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * Exports a {@link PdfDocument} as a fully flattened PDF with no PDF Tapir
 * editing metadata. All annotations are burned into page content; the resulting
 * file cannot be re-edited in PDF Tapir and will have a smaller file size.
 * <p>
 * The source document is never modified by this class.
 */
public class PdfFlattenExporter {

    private final PdfTapirPackageService packageService = new PdfTapirPackageService();
    private final PdfAnnotationFlattener flattener      = new PdfAnnotationFlattener();

    /**
     * Writes a flattened, metadata-free copy of {@code document} to {@code target}.
     * Uses an atomic temp-file-then-rename pattern to avoid partially written files.
     *
     * @param document the source document (not modified)
     * @param target   the destination file (will be created or overwritten)
     * @throws IOException if writing or renaming the file fails
     */
    public void export(PdfDocument document, File target) throws IOException {
        byte[] basePdfBytes = document.getCleanBasePdfBytes();
        if (basePdfBytes == null) {
            basePdfBytes = buildCleanBase(document.getPdDocument());
        }

        var tmp = java.nio.file.Files.createTempFile("ptapir-flat-", ".pdf").toFile();
        tmp.deleteOnExit(); // safety net in case of crash
        try (var flatDoc = Loader.loadPDF(basePdfBytes)) {
            flattener.flatten(flatDoc, document);
            // intentionally NOT writing the pdf-tapir metadata package
            flatDoc.save(tmp);
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            tmp.delete();
            throw e;
        }
    }

    /**
     * Flattens {@code document} into a temporary file managed by the JVM temp directory.
     * The caller is responsible for deleting the file when done.
     *
     * @return path to the temporary flattened PDF
     * @throws IOException if flattening fails
     */
    public java.nio.file.Path flattenToTemp(PdfDocument document) throws IOException {
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ptapir-print-", ".pdf");
        tmp.toFile().deleteOnExit();
        export(document, tmp.toFile());
        return tmp;
    }

    private byte[] buildCleanBase(PDDocument source) throws IOException {
        var sourceBytes = new ByteArrayOutputStream();
        source.save(sourceBytes);
        try (var clean = Loader.loadPDF(sourceBytes.toByteArray())) {
            packageService.remove(clean);
            removeCheckboxFields(clean);
            for (int i = 0; i < clean.getNumberOfPages(); i++) {
                var page = clean.getPage(i);
                var existing = new ArrayList<>(page.getAnnotations());
                existing.removeIf(this::isOurAnnotation);
                page.setAnnotations(existing);
            }
            var cleanBytes = new ByteArrayOutputStream();
            clean.save(cleanBytes);
            return cleanBytes.toByteArray();
        }
    }

    private boolean isOurAnnotation(PDAnnotation annotation) {
        if (annotation instanceof PDAnnotationMarkup markup) {
            String subj = markup.getSubject();
            if (PdfSaver.TAG_TEXT.equals(subj) || PdfSaver.TAG_IMAGE.equals(subj)) return true;
        }
        return PdfSaver.TAG_CHECKBOX.equals(annotation.getAnnotationName());
    }

    private void removeCheckboxFields(PDDocument pdDoc) {
        var acroForm = pdDoc.getDocumentCatalog().getAcroForm();
        if (acroForm == null) return;
        acroForm.getFields().removeIf(f ->
                f.getPartialName() != null && f.getPartialName().startsWith(PdfSaver.CB_PREFIX));
    }
}
