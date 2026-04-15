package com.pdftapir.service;

import com.pdftapir.model.PdfDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfEncryptionServiceTest {

    private final PdfEncryptionService service = new PdfEncryptionService();
    private final PdfSaver saver = new PdfSaver();
    private final PdfLoader loader = new PdfLoader();

    @Test
    void encryptMakesDocumentEncrypted(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("enc.pdf").toFile();
        try (var pdDoc = new PDDocument()) {
            pdDoc.addPage(new PDPage(PDRectangle.A4));
            assertFalse(service.isEncrypted(pdDoc));
            service.encrypt(pdDoc, "secret");
            pdDoc.save(file);
        }
        // Reload with password — document should be encrypted
        var doc = loader.load(file, "secret");
        assertTrue(service.isEncrypted(doc.getPdDocument()));
        doc.close();
    }

    @Test
    void decryptRemovesEncryption(@TempDir Path tmp) throws Exception {
        // Create, encrypt, and save a PDF
        var file = tmp.resolve("test.pdf").toFile();
        try (var pdDoc = new PDDocument()) {
            pdDoc.addPage(new PDPage(PDRectangle.A4));
            service.encrypt(pdDoc, "secret");
            pdDoc.save(file);
        }

        // Load with password, decrypt, save, reload without password
        var doc = loader.load(file, "secret");
        service.decrypt(doc.getPdDocument());
        saver.save(doc, file);
        doc.close();

        // Should now open without a password
        var reopened = loader.load(file);
        assertFalse(service.isEncrypted(reopened.getPdDocument()));
        reopened.close();
    }

    @Test
    void wrongPasswordThrows(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("enc.pdf").toFile();
        try (var pdDoc = new PDDocument()) {
            pdDoc.addPage(new PDPage(PDRectangle.A4));
            service.encrypt(pdDoc, "correct");
            pdDoc.save(file);
        }
        assertThrows(InvalidPasswordException.class, () -> loader.load(file, "wrong"));
    }
}
