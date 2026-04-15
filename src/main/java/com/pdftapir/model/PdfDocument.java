package com.pdftapir.model;

import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an open PDF document, wrapping PDFBox's {@link PDDocument} together
 * with the application-level page models and the original source file reference.
 * <p>
 * Implements {@link AutoCloseable} so it can be used in try-with-resources blocks;
 * closing this object closes the underlying {@link PDDocument}.
 */
public class PdfDocument implements AutoCloseable {
    private final PDDocument    pdDocument;
    private final List<PdfPage> pages;
    private       File          sourceFile;
    private       boolean       encrypted;
    private       byte[]        cleanBasePdfBytes;

    /**
     * Creates a PdfDocument wrapping the given PDFBox document.
     *
     * @param pdDocument the underlying PDFBox document (must not be {@code null})
     * @param pages      the list of {@link PdfPage} objects, one per page
     * @param sourceFile the file from which the document was loaded, or {@code null} for new documents
     */
    public PdfDocument(PDDocument pdDocument, List<PdfPage> pages, File sourceFile) {
        Objects.requireNonNull(pdDocument, "pdDocument must not be null");
        Objects.requireNonNull(pages, "pages must not be null");
        this.pdDocument = pdDocument;
        this.pages      = pages;
        this.sourceFile = sourceFile;
        this.encrypted  = pdDocument.isEncrypted();
    }

    /** Returns the underlying PDFBox {@link PDDocument}. */
    public PDDocument    getPdDocument()      { return pdDocument; }

    /** Returns the list of pages in this document. */
    public List<PdfPage> getPages()           { return Collections.unmodifiableList(pages); }

    /** Returns the source {@link File} from which this document was loaded. */
    public File          getSourceFile()      { return sourceFile; }

    /** Sets the source file (e.g. after a Save As operation). */
    public void          setSourceFile(File f){ this.sourceFile = f; }

    /** Returns {@code true} if the document is currently encrypted. */
    public boolean       isEncrypted()         { return encrypted; }

    /** Updates the tracked encryption state (call after encrypt/decrypt + save). */
    public void          setEncrypted(boolean v){ this.encrypted = v; }

    /**
     * Returns a defensive copy of the clean base PDF used for hybrid flattened saves,
     * or {@code null} if this document has not yet been saved in that format.
     */
    public byte[] getCleanBasePdfBytes() {
        return cleanBasePdfBytes == null ? null : java.util.Arrays.copyOf(cleanBasePdfBytes, cleanBasePdfBytes.length);
    }

    /** Stores the clean base PDF bytes used to rebuild flattened output on future saves. */
    public void setCleanBasePdfBytes(byte[] bytes) {
        this.cleanBasePdfBytes = bytes == null ? null : java.util.Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Replaces the entire page list in-place. Used by merge and page-removal services
     * after structural changes to the underlying {@link PDDocument}.
     *
     * @param newPages the new page list (must not be {@code null})
     */
    public void replacePages(List<PdfPage> newPages) {
        pages.clear();
        pages.addAll(newPages);
    }

    /**
     * Closes the underlying {@link PDDocument}, releasing all associated resources.
     *
     * @throws IOException if closing the document fails
     */
    @Override
    public void close() throws IOException { pdDocument.close(); }
}
