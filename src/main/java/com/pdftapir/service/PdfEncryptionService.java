package com.pdftapir.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.IOException;

/**
 * Encrypts and decrypts {@link PDDocument} instances using PDFBox's standard
 * security handler (AES-256). For simplicity the owner and user passwords are
 * set to the same value supplied by the caller.
 */
public class PdfEncryptionService {

    private static final int KEY_LENGTH = 256;

    /**
     * Encrypts {@code doc} with the given password (used for both owner and user).
     * The encryption is applied in-memory; the caller is responsible for saving the document.
     *
     * @param doc      the document to encrypt
     * @param password the password to apply
     * @throws IOException if PDFBox cannot apply the protection policy
     */
    public void encrypt(PDDocument doc, String password) throws IOException {
        var permissions = new AccessPermission();
        var policy = new StandardProtectionPolicy(password, password, permissions);
        policy.setEncryptionKeyLength(KEY_LENGTH);
        doc.protect(policy);
    }

    /**
     * Removes encryption from {@code doc}.
     * The caller is responsible for saving the document afterwards.
     *
     * @param doc the encrypted document to decrypt
     */
    public void decrypt(PDDocument doc) {
        doc.setAllSecurityToBeRemoved(true);
    }

    /**
     * Returns {@code true} if the document is currently encrypted.
     *
     * @param doc the document to check
     */
    public boolean isEncrypted(PDDocument doc) {
        return doc.isEncrypted();
    }
}
