package com.pdfescroto.service;

import com.pdfescroto.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationRubberStamp;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.junit.jupiter.api.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;

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
        cb.setCheckmarkColor("#ff0000");
        page.addAnnotation(cb);
        saver.save(doc, tempFile);
        doc.close();

        var reloaded    = loader.load(tempFile);
        var annotations = reloaded.getPages().get(0).getAnnotations();
        assertEquals(1, annotations.size());
        var loaded = (CheckboxAnnotation) annotations.get(0);
        assertEquals("agree",   loaded.getLabel());
        assertTrue(loaded.isChecked());
        assertEquals("#ff0000", loaded.getCheckmarkColor());
        reloaded.close();
    }

    @Test
    void imageAnnotationSurvivesRoundTrip() throws Exception {
        // Build a minimal 4x4 red PNG as test image data
        var bim = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics g = bim.getGraphics();
        g.setColor(java.awt.Color.RED);
        g.fillRect(0, 0, 4, 4);
        g.dispose();
        var bos = new ByteArrayOutputStream();
        ImageIO.write(bim, "PNG", bos);
        byte[] pngBytes = bos.toByteArray();

        var loader = new PdfLoader();
        var saver  = new PdfSaver();

        var doc  = loader.load(tempFile);
        var page = doc.getPages().get(0);
        var ia   = new ImageAnnotation(50, 100, 100, 100);
        ia.setImageData(pngBytes);
        page.addAnnotation(ia);
        saver.save(doc, tempFile);
        doc.close();

        var reloaded    = loader.load(tempFile);
        var annotations = reloaded.getPages().get(0).getAnnotations();
        assertEquals(1, annotations.size(), "image annotation should survive round-trip");
        assertInstanceOf(ImageAnnotation.class, annotations.get(0));
        var loaded = (ImageAnnotation) annotations.get(0);
        assertNotNull(loaded.getImageData(), "image bytes should be restored from PDF");
        assertEquals(50,  loaded.getX(),      0.5);
        assertEquals(100, loaded.getY(),      0.5);
        assertEquals(100, loaded.getWidth(),  0.5);
        assertEquals(100, loaded.getHeight(), 0.5);
        reloaded.close();
    }

    @Test
    void checkedCheckboxWritesViewerVisibleAppearance() throws Exception {
        var saver = new PdfSaver();

        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            var pageModel = new PdfPage(0, PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight());
            var pdf = new PdfDocument(doc, java.util.List.of(pageModel), tempFile);

            var cb = new CheckboxAnnotation(50, 200, 20, 20);
            cb.setLabel("agree");
            cb.setChecked(true);
            pageModel.addAnnotation(cb);

            saver.save(pdf, tempFile);
        }

        try (var saved = org.apache.pdfbox.Loader.loadPDF(tempFile)) {
            var widget = (PDAnnotationWidget) saved.getPage(0).getAnnotations().get(0);
            var field = (PDCheckBox) saved.getDocumentCatalog().getAcroForm().getFieldTree().iterator().next();

            assertNotNull(widget.getAppearance(), "checkbox widget should have an appearance dictionary");
            assertFalse(field.getOnValue().isBlank(), "checkbox should define a concrete on-state value");
            assertEquals(field.getOnValue(), widget.getAppearanceState().getName(),
                    "checked checkbox should use its on-state appearance");
            assertTrue(field.isChecked(), "saved checkbox field should be checked in the raw PDF");
        }
    }

    @Test
    void imageAnnotationWritesLocalAppearanceBoundingBox() throws Exception {
        var saver = new PdfSaver();

        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            var pageModel = new PdfPage(0, PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight());
            var pdf = new PdfDocument(doc, java.util.List.of(pageModel), tempFile);

            var bim = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
            Graphics g = bim.getGraphics();
            g.setColor(java.awt.Color.RED);
            g.fillRect(0, 0, 4, 4);
            g.dispose();
            var bos = new ByteArrayOutputStream();
            ImageIO.write(bim, "PNG", bos);

            var ia = new ImageAnnotation(50, 100, 100, 100);
            ia.setImageData(bos.toByteArray());
            pageModel.addAnnotation(ia);

            saver.save(pdf, tempFile);
        }

        try (var saved = org.apache.pdfbox.Loader.loadPDF(tempFile)) {
            var stamp = (PDAnnotationRubberStamp) saved.getPage(0).getAnnotations().get(0);
            var bbox = stamp.getAppearance().getNormalAppearance().getAppearanceStream().getBBox();

            assertEquals(0, bbox.getLowerLeftX(), 0.01, "appearance BBox should start at local x=0");
            assertEquals(0, bbox.getLowerLeftY(), 0.01, "appearance BBox should start at local y=0");
            assertEquals(100, bbox.getWidth(), 0.01);
            assertEquals(100, bbox.getHeight(), 0.01);
        }
    }
}
