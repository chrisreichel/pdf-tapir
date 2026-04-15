package com.pdftapir.service;

import com.pdftapir.model.PdfDocument;
import com.pdftapir.model.PdfPage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges pages from external PDF files into an open {@link PdfDocument}.
 * <p>
 * Pages from source files are imported at the PDFBox level, preserving vector
 * content, fonts, and images. After merging, the {@code PdfDocument}'s page
 * list is rebuilt from the mutated {@code PDDocument}.
 * <p>
 * <strong>Note:</strong> Merging resets the caller's undo history (caller's responsibility).
 */
public class PdfMergeService {

    private final PdfLoader loader = new PdfLoader();
    private final PdfRenderer renderer = new PdfRenderer();

    /**
     * Appends pages from {@code sources} after the last page of {@code target}.
     *
     * @param target  the open document to merge into
     * @param sources PDF files whose pages will be appended
     * @throws IOException if any source file cannot be read or merged
     */
    public void append(PdfDocument target, List<File> sources) throws IOException {
        merge(target, sources, false);
    }

    /**
     * Prepends pages from {@code sources} before the first page of {@code target}.
     *
     * @param target  the open document to merge into
     * @param sources PDF files whose pages will be inserted at the front
     * @throws IOException if any source file cannot be read or merged
     */
    public void prepend(PdfDocument target, List<File> sources) throws IOException {
        merge(target, sources, true);
    }

    private void merge(PdfDocument target, List<File> sources, boolean prepend) throws IOException {
        var pdTarget = target.getPdDocument();

        for (File sourceFile : sources) {
            try (var sourceDoc = Loader.loadPDF(sourceFile)) {
                var merger = new PDFMergerUtility();
                if (prepend) {
                    // PDFMergerUtility always appends — for prepend we temporarily clone
                    // source pages into a fresh doc, merge target into it, then copy
                    // all pages back into the original target PDDocument.
                    var tempMerger = new PDFMergerUtility();
                    // Merge source pages first, then original target pages
                    int originalPageCount = pdTarget.getNumberOfPages();
                    tempMerger.appendDocument(pdTarget, sourceDoc);
                    // Now rotate: move the newly appended pages to the front
                    int srcPageCount = sourceDoc.getNumberOfPages();
                    for (int i = 0; i < srcPageCount; i++) {
                        // Each iteration moves the (originalPageCount)-th page to position i
                        var page = pdTarget.getPage(originalPageCount);
                        pdTarget.removePage(originalPageCount);
                        pdTarget.getPages().insertBefore(page, pdTarget.getPage(i));
                    }
                } else {
                    merger.appendDocument(pdTarget, sourceDoc);
                }
            }
        }

        rebuildPages(target);
    }

    /**
     * Rebuilds the {@link PdfDocument}'s page list from its underlying {@code PDDocument}.
     * The rendered images are re-rendered at the current DPI.
     */
    void rebuildPages(PdfDocument doc) throws IOException {
        var pdDoc = doc.getPdDocument();
        var newPages = new ArrayList<PdfPage>();
        for (int i = 0; i < pdDoc.getNumberOfPages(); i++) {
            var pdPage   = pdDoc.getPage(i);
            var mediaBox = pdPage.getMediaBox();
            var page     = new PdfPage(i, mediaBox.getWidth(), mediaBox.getHeight());
            try {
                page.setRenderedImage(renderer.renderPage(pdDoc, i));
            } catch (Exception e) {
                // Headless / JavaFX not initialized — canvas will re-render on display
            }
            newPages.add(page);
        }
        doc.replacePages(newPages);
    }
}
