package com.pdfescroto.service;

import com.pdfescroto.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.*;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class PdfRoundTripTest {

    private File tempFile;

    @BeforeEach
    void setUp() throws Exception {
        tempFile = File.createTempFile("pescroto-test-", ".pdf");
        tempFile.deleteOnExit();
        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(tempFile);
        }
    }

    @Test
    void textAnnotationSurvivesRoundTrip() throws Exception {
        var loader = new PdfLoader();
        var saver  = new PdfSaver();

        var doc  = loader.load(tempFile);
        var page = doc.getPages().get(0);
        var ta   = new TextAnnotation(50, 100, 200, 30);
        ta.setText("Hello PDF");
        ta.setFontSize(14f);
        page.addAnnotation(ta);
        saver.save(doc, tempFile);
        doc.close();

        var reloaded    = loader.load(tempFile);
        var annotations = reloaded.getPages().get(0).getAnnotations();
        assertEquals(1, annotations.size());
        var loaded = (TextAnnotation) annotations.get(0);
        assertEquals("Hello PDF", loaded.getText());
        assertEquals(50,  loaded.getX(),      0.5);
        assertEquals(100, loaded.getY(),      0.5);
        assertEquals(200, loaded.getWidth(),  0.5);
        assertEquals(30,  loaded.getHeight(), 0.5);
        reloaded.close();
    }

    @Test
    void checkboxAnnotationSurvivesRoundTrip() throws Exception {
        var loader = new PdfLoader();
        var saver  = new PdfSaver();

        var doc  = loader.load(tempFile);
        var page = doc.getPages().get(0);
        var cb   = new CheckboxAnnotation(50, 200, 20, 20);
        cb.setLabel("agree");
        cb.setChecked(true);
        page.addAnnotation(cb);
        saver.save(doc, tempFile);
        doc.close();

        var reloaded    = loader.load(tempFile);
        var annotations = reloaded.getPages().get(0).getAnnotations();
        assertEquals(1, annotations.size());
        var loaded = (CheckboxAnnotation) annotations.get(0);
        assertEquals("agree", loaded.getLabel());
        assertTrue(loaded.isChecked());
        reloaded.close();
    }
}
