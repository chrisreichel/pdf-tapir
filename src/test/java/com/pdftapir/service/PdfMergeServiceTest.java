package com.pdftapir.service;

import com.pdftapir.model.PdfDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfMergeServiceTest {

    private final PdfMergeService mergeService = new PdfMergeService();
    private final PdfLoader loader = new PdfLoader();

    private File createPdf(Path dir, String name, int pages) throws Exception {
        var file = dir.resolve(name).toFile();
        try (var doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(file);
        }
        return file;
    }

    @Test
    void appendIncreasesPageCount(@TempDir Path tmp) throws Exception {
        var base = createPdf(tmp, "base.pdf", 2);
        var extra = createPdf(tmp, "extra.pdf", 3);

        var doc = loader.load(base);
        assertEquals(2, doc.getPages().size());

        mergeService.append(doc, List.of(extra));

        assertEquals(5, doc.getPdDocument().getNumberOfPages());
        assertEquals(5, doc.getPages().size());
        doc.close();
    }

    @Test
    void prependInsertsAtFront(@TempDir Path tmp) throws Exception {
        var base = createPdf(tmp, "base.pdf", 1);
        var extra = createPdf(tmp, "extra.pdf", 2);

        var doc = loader.load(base);
        mergeService.prepend(doc, List.of(extra));

        assertEquals(3, doc.getPdDocument().getNumberOfPages());
        // After prepend the 2 source pages should be at indices 0 and 1
        // (original page moved to index 2)
        assertEquals(3, doc.getPages().size());
        doc.close();
    }
}
