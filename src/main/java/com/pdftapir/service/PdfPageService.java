package com.pdftapir.service;

import com.pdftapir.model.PdfDocument;
import com.pdftapir.model.PdfPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

/**
 * Removes pages from an open {@link PdfDocument}.
 * <p>
 * At least one page must remain after removal. Callers are responsible for
 * clearing the undo history and saving the document afterwards.
 */
public class PdfPageService {

    private final PdfRenderer renderer = new PdfRenderer();

    /**
     * Removes the pages at the given zero-based indices from {@code doc}.
     * <p>
     * The underlying {@link org.apache.pdfbox.pdmodel.PDDocument} is mutated in-place
     * and the {@code PdfDocument}'s page list is rebuilt with updated indices.
     *
     * @param doc         the document to modify
     * @param pageIndices zero-based indices of the pages to remove
     * @throws IllegalArgumentException if {@code pageIndices} would remove all pages
     * @throws IOException              if re-rendering fails (non-fatal; pages will have no preview)
     */
    public void removePages(PdfDocument doc, Set<Integer> pageIndices) throws IOException {
        int total = doc.getPdDocument().getNumberOfPages();
        if (pageIndices.size() >= total) {
            throw new IllegalArgumentException("Cannot remove all pages — at least one page must remain.");
        }

        // Remove in reverse order so that earlier indices are not shifted
        var sortedDesc = pageIndices.stream().sorted((a, b) -> b - a).toList();
        for (int idx : sortedDesc) {
            doc.getPdDocument().removePage(idx);
        }

        rebuildPages(doc);
    }

    private void rebuildPages(PdfDocument doc) throws IOException {
        var pdDoc    = doc.getPdDocument();
        var newPages = new ArrayList<PdfPage>();
        for (int i = 0; i < pdDoc.getNumberOfPages(); i++) {
            var pdPage   = pdDoc.getPage(i);
            var mediaBox = pdPage.getMediaBox();
            var page     = new PdfPage(i, mediaBox.getWidth(), mediaBox.getHeight());
            try {
                page.setRenderedImage(renderer.renderPage(pdDoc, i));
            } catch (Exception e) {
                // Headless / no JavaFX — canvas re-renders on display
            }
            newPages.add(page);
        }
        doc.replacePages(newPages);
    }
}
