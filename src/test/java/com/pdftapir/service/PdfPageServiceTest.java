package com.pdftapir.service;

import com.pdftapir.model.PdfDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PdfPageServiceTest {

    private final PdfPageService pageService = new PdfPageService();
    private final PdfLoader loader = new PdfLoader();

    @Test
    void removeOnePageFromTwoPageDocument(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("two.pdf").toFile();
        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(file);
        }

        var doc = loader.load(file);
        assertEquals(2, doc.getPages().size());

        pageService.removePages(doc, Set.of(0));

        assertEquals(1, doc.getPdDocument().getNumberOfPages());
        assertEquals(1, doc.getPages().size());
        doc.close();
    }

    @Test
    void removeAllPagesThrows(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("one.pdf").toFile();
        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(file);
        }

        var doc = loader.load(file);
        assertThrows(IllegalArgumentException.class,
                () -> pageService.removePages(doc, Set.of(0)));
        doc.close();
    }

    @Test
    void removePagesRebuildsIndices(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("three.pdf").toFile();
        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(file);
        }

        var doc = loader.load(file);
        pageService.removePages(doc, Set.of(1)); // remove middle page

        assertEquals(2, doc.getPages().size());
        assertEquals(0, doc.getPages().get(0).getPageIndex());
        assertEquals(1, doc.getPages().get(1).getPageIndex());
        doc.close();
    }
}
