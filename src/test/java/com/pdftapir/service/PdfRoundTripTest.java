package com.pdftapir.service;

import com.pdftapir.model.*;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

class PdfRoundTripTest {

    private File tempFile;
    private static final COSName PDF_TAPIR_PACKAGE = COSName.getPDFName("PETapirPackage");

    @BeforeEach
    void setUp() throws Exception {
        tempFile = File.createTempFile("ptapir-test-", ".pdf");
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
    void saveFlattensVisibleItemsButKeepsEditablePdfTapirPackage() throws Exception {
        var loader = new PdfLoader();
        var saver  = new PdfSaver();

        var doc  = loader.load(tempFile);
        var page = doc.getPages().get(0);

        var text = new TextAnnotation(50, 500, 120, 24);
        text.setText("Flattened");
        text.setFontSize(16f);
        text.setFontColor("#0033cc");
        page.addAnnotation(text);

        var checkbox = new CheckboxAnnotation(50, 450, 24, 24);
        checkbox.setChecked(true);
        checkbox.setCheckmarkColor("#008800");
        page.addAnnotation(checkbox);

        var image = new ImageAnnotation(50, 100, 30, 30);
        image.setImageData(redPngBytes());
        page.addAnnotation(image);

        saver.save(doc, tempFile);
        doc.close();

        try (var saved = org.apache.pdfbox.Loader.loadPDF(tempFile)) {
            assertTrue(saved.getPage(0).getAnnotations().isEmpty(),
                    "flattened PDF should not expose pdf-tapir items as movable annotations/widgets");
            var acroForm = saved.getDocumentCatalog().getAcroForm();
            assertTrue(acroForm == null || !acroForm.getFieldTree().iterator().hasNext(),
                    "flattened PDF should not expose pdf-tapir checkboxes as form fields");
            assertNotNull(saved.getDocumentCatalog().getCOSObject().getDictionaryObject(PDF_TAPIR_PACKAGE),
                    "editable pdf-tapir package should be stored privately in the PDF");

            var rendered = new PDFRenderer(saved).renderImageWithDPI(0, 72);
            int sampleX = 65;
            int sampleY = Math.round(PDRectangle.A4.getHeight() - 115);
            var sampled = new java.awt.Color(rendered.getRGB(sampleX, sampleY));
            assertTrue(sampled.getRed() > 180 && sampled.getGreen() < 100 && sampled.getBlue() < 100,
                    "flattened image annotation should be drawn into visible page content");
        }

        var reloaded = loader.load(tempFile);
        var annotations = reloaded.getPages().get(0).getAnnotations();
        assertEquals(3, annotations.size(), "private package should restore editable pdf-tapir items");
        assertInstanceOf(TextAnnotation.class, annotations.get(0));
        assertInstanceOf(CheckboxAnnotation.class, annotations.get(1));
        assertInstanceOf(ImageAnnotation.class, annotations.get(2));
        assertEquals("Flattened", ((TextAnnotation) annotations.get(0)).getText());
        assertTrue(((CheckboxAnnotation) annotations.get(1)).isChecked());
        assertNotNull(((ImageAnnotation) annotations.get(2)).getImageData());
        reloaded.close();
    }

    @Test
    void reloadEditAndResaveUpdatesPrivatePackageWithoutReintroducingPdfAnnotations() throws Exception {
        var loader = new PdfLoader();
        var saver  = new PdfSaver();

        var doc  = loader.load(tempFile);
        var page = doc.getPages().get(0);
        var text = new TextAnnotation(50, 500, 120, 24);
        text.setText("Before");
        page.addAnnotation(text);
        saver.save(doc, tempFile);
        doc.close();

        var reloaded = loader.load(tempFile);
        var loadedText = (TextAnnotation) reloaded.getPages().get(0).getAnnotations().get(0);
        loadedText.setText("After");
        loadedText.setX(200);
        saver.save(reloaded, tempFile);
        reloaded.close();

        try (var saved = org.apache.pdfbox.Loader.loadPDF(tempFile)) {
            assertTrue(saved.getPage(0).getAnnotations().isEmpty(),
                    "re-saving a hybrid PDF should keep pdf-tapir items flattened");
        }

        var edited = loader.load(tempFile);
        var annotations = edited.getPages().get(0).getAnnotations();
        assertEquals(1, annotations.size(), "private metadata should contain the current editable item once");
        var editedText = (TextAnnotation) annotations.get(0);
        assertEquals("After", editedText.getText());
        assertEquals(200, editedText.getX(), 0.5);
        edited.close();
    }

    private byte[] redPngBytes() throws Exception {
        var bim = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics g = bim.getGraphics();
        g.setColor(java.awt.Color.RED);
        g.fillRect(0, 0, 4, 4);
        g.dispose();
        var bos = new ByteArrayOutputStream();
        ImageIO.write(bim, "PNG", bos);
        return bos.toByteArray();
    }
}
