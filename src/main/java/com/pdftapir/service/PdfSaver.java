package com.pdftapir.service;

import com.pdftapir.model.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * Saves {@link PdfDocument} annotations as flattened page content plus a private
 * pdf-tapir package containing editable model data.
 * <p>
 * External PDF viewers see fixed page content, not movable PDF annotations or
 * AcroForm widgets. {@link PdfLoader} reads the private package to reconstruct
 * editable pdf-tapir annotations when the file is opened in this app.
 */
public class PdfSaver {

    private final PdfTapirPackageService packageService = new PdfTapirPackageService();
    private final PdfAnnotationFlattener   flattener      = new PdfAnnotationFlattener();

    /** Legacy subject tag applied to free-text annotations written by older versions. */
    static final String TAG_TEXT     = "pdf-tapir-text";
    /** Legacy subject tag applied to image rubber-stamp annotations written by older versions. */
    static final String TAG_IMAGE    = "pdf-tapir-image";
    /** Legacy annotation name tag set on checkbox widgets written by older versions. */
    static final String TAG_CHECKBOX = "pdf-tapir-checkbox";
    /** Legacy prefix applied to AcroForm checkbox field partial names. */
    static final String CB_PREFIX    = "ptapir_";

    /**
     * Saves {@code document} to {@code target}, replacing any previously written
     * pdf-tapir annotations with the current state.
     * <p>
     * Uses an atomic temp-file-then-rename pattern to avoid partially written files.
     *
     * @param document the document to save
     * @param target   the destination file (will be overwritten)
     * @throws IOException if writing or renaming the file fails
     */
    public void save(PdfDocument document, File target) throws IOException {
        byte[] basePdfBytes = document.getCleanBasePdfBytes();
        if (basePdfBytes == null) {
            basePdfBytes = createCleanBasePdfBytes(document.getPdDocument());
            document.setCleanBasePdfBytes(basePdfBytes);
        }

        // Write to a system temp file, then move over the target
        var tmp = java.nio.file.Files.createTempFile("ptapir-", ".pdf").toFile();
        tmp.deleteOnExit(); // safety net in case of crash
        try (var flatDoc = Loader.loadPDF(basePdfBytes)) {
            flattener.flatten(flatDoc, document);
            packageService.write(flatDoc, basePdfBytes, document.getPages());
            String pwd = document.getPendingPassword();
            if (pwd != null) {
                var policy = new StandardProtectionPolicy(pwd, pwd, new AccessPermission());
                policy.setEncryptionKeyLength(256);
                flatDoc.protect(policy);
            }
            flatDoc.save(tmp);
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (pwd != null) {
                document.setPendingPassword(null);
            }
        } catch (IOException e) {
            tmp.delete();
            throw e;
        }
    }

    private byte[] createCleanBasePdfBytes(PDDocument source) throws IOException {
        var sourceBytes = new ByteArrayOutputStream();
        source.save(sourceBytes);
        try (var clean = Loader.loadPDF(sourceBytes.toByteArray())) {
            packageService.remove(clean);
            removeOurCheckboxFields(clean);
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
            if (TAG_TEXT.equals(subj) || TAG_IMAGE.equals(subj)) return true;
        }
        return TAG_CHECKBOX.equals(annotation.getAnnotationName());
    }

    private void removeOurCheckboxFields(PDDocument pdDoc) {
        var acroForm = pdDoc.getDocumentCatalog().getAcroForm();
        if (acroForm == null) return;
        acroForm.getFields().removeIf(f ->
                f.getPartialName() != null && f.getPartialName().startsWith(CB_PREFIX));
    }
}
